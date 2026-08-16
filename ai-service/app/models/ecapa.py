"""SpeechBrain ECAPA-TDNN inference and checkpoint loading."""

import math
import os
import threading
from pathlib import Path
from typing import Any


class ModelUnavailableError(RuntimeError):
    """Raised when the configured speaker model cannot be loaded."""


class InvalidEmbeddingError(ValueError):
    """Raised when an embedding is empty, malformed, or dimensionally invalid."""


class EcapaTDNN:
    """Thread-safe singleton wrapper around SpeechBrain's ECAPA encoder."""

    _instance: "EcapaTDNN | None" = None
    _lock = threading.Lock()

    def __init__(self, classifier: Any | None = None) -> None:
        self._classifier = classifier or self._load_classifier()

    @classmethod
    def shared(cls) -> "EcapaTDNN":
        if cls._instance is None:
            with cls._lock:
                if cls._instance is None:
                    cls._instance = cls()
        return cls._instance

    @staticmethod
    def configured_model_exists() -> bool:
        configured = os.getenv("ECAPA_MODEL_PATH", "").strip()
        if configured:
            return Path(configured).exists()
        return bool(os.getenv("ECAPA_MODEL_SOURCE", "speechbrain/spkrec-ecapa-voxceleb").strip())

    def extract_embedding(self, audio_path: str | Path) -> list[float]:
        try:
            import torch
            import torchaudio
        except ImportError as error:
            raise ModelUnavailableError("torch and torchaudio are required") from error

        signal, sample_rate = torchaudio.load(str(audio_path))
        if signal.numel() == 0:
            raise InvalidEmbeddingError("audio contains no samples")
        if signal.shape[0] > 1:
            signal = signal.mean(dim=0, keepdim=True)
        if sample_rate != 16_000:
            signal = torchaudio.functional.resample(signal, sample_rate, 16_000)
        with torch.inference_mode():
            embedding = self._classifier.encode_batch(signal).squeeze()
        vector = [float(value) for value in embedding.detach().cpu().tolist()]
        return validate_embedding(vector)

    @staticmethod
    def cosine_similarity(first: list[float], second: list[float]) -> float:
        a = validate_embedding(first)
        b = validate_embedding(second, expected_dimension=len(a))
        numerator = sum(left * right for left, right in zip(a, b, strict=True))
        a_norm = math.sqrt(sum(value * value for value in a))
        b_norm = math.sqrt(sum(value * value for value in b))
        if a_norm == 0 or b_norm == 0:
            raise InvalidEmbeddingError("embedding norm must be non-zero")
        return numerator / (a_norm * b_norm)

    def verify(self, audio_path: str | Path, enrolled: list[float]) -> tuple[bool, float]:
        incoming = self.extract_embedding(audio_path)
        score = self.cosine_similarity(incoming, enrolled)
        threshold = float(os.getenv("SV_COSINE_THRESHOLD", "0.80"))
        return score >= threshold, round(score, 6)

    def identify(
        self,
        audio_path: str | Path,
        enrolled: list[dict[str, Any]],
    ) -> tuple[str | None, float]:
        if not enrolled:
            return None, 0.0
        incoming = self.extract_embedding(audio_path)
        scored = [
            (
                str(speaker["user_id"]),
                self.cosine_similarity(incoming, speaker["embedding"]),
            )
            for speaker in enrolled
        ]
        best_user_id, best_score = max(scored, key=lambda item: item[1])
        threshold = float(os.getenv("SID_COSINE_THRESHOLD", os.getenv("SV_COSINE_THRESHOLD", "0.80")))
        return (best_user_id if best_score >= threshold else None), round(best_score, 6)

    @staticmethod
    def _load_classifier() -> Any:
        try:
            import torch
            from speechbrain.inference.speaker import EncoderClassifier
        except ImportError as error:
            raise ModelUnavailableError("speechbrain and torch are required") from error

        configured_path = os.getenv("ECAPA_MODEL_PATH", "").strip()
        path = Path(configured_path) if configured_path else None
        if path is not None and not path.exists():
            raise ModelUnavailableError(f"speaker model does not exist: {path}")

        configured_source = os.getenv(
            "ECAPA_MODEL_SOURCE", "speechbrain/spkrec-ecapa-voxceleb"
        ).strip()
        source = str(path) if path is not None and path.is_dir() else configured_source
        if not source:
            raise ModelUnavailableError("ECAPA_MODEL_PATH or ECAPA_MODEL_SOURCE is required")
        cache = os.getenv("ECAPA_MODEL_CACHE", "/tmp/dearly-ecapa")
        device = "cuda" if torch.cuda.is_available() else "cpu"
        try:
            classifier = EncoderClassifier.from_hparams(
                source=source,
                savedir=cache,
                run_opts={"device": device},
            )
            if path is not None and path.is_file():
                checkpoint = torch.load(path, map_location=device, weights_only=False)
                state = checkpoint.get("embedding_model", checkpoint)
                classifier.mods.embedding_model.load_state_dict(state)
            return classifier
        except Exception as error:
            raise ModelUnavailableError(f"could not load speaker model: {error}") from error


def validate_embedding(
    values: Any,
    expected_dimension: int | None = None,
) -> list[float]:
    if not isinstance(values, list) or not values:
        raise InvalidEmbeddingError("embedding must be a non-empty list")
    try:
        vector = [float(value) for value in values]
    except (TypeError, ValueError) as error:
        raise InvalidEmbeddingError("embedding must contain numbers") from error
    if expected_dimension is not None and len(vector) != expected_dimension:
        raise InvalidEmbeddingError("embedding dimensions do not match")
    if not all(math.isfinite(value) for value in vector):
        raise InvalidEmbeddingError("embedding values must be finite")
    return vector
