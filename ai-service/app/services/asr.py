"""Local Whisper speech-to-text service."""

import asyncio
import os
import threading
from pathlib import Path
from typing import Any


class ASRUnavailableError(RuntimeError):
    """Raised when Whisper cannot be loaded or transcription fails."""


class ASRService:
    _instance: "ASRService | None" = None
    _lock = threading.Lock()

    def __init__(self, model: Any | None = None) -> None:
        self._model = model or self._load_model()

    @classmethod
    def shared(cls) -> "ASRService":
        if cls._instance is None:
            with cls._lock:
                if cls._instance is None:
                    cls._instance = cls()
        return cls._instance

    async def transcribe(self, audio_path: str | Path) -> str:
        def run() -> str:
            try:
                use_fp16 = os.getenv("WHISPER_FP16", "").strip().lower() in {
                    "1",
                    "true",
                    "yes",
                }
                result = self._model.transcribe(
                    str(audio_path),
                    language=os.getenv("WHISPER_LANGUAGE", "vi"),
                    fp16=use_fp16,
                )
            except Exception as error:
                raise ASRUnavailableError(f"transcription failed: {error}") from error
            transcript = str(result.get("text", "")).strip()
            if not transcript:
                raise ASRUnavailableError("transcription produced no text")
            return transcript

        return await asyncio.to_thread(run)

    @staticmethod
    def _load_model() -> Any:
        try:
            import torch
            import whisper
        except ImportError as error:
            raise ASRUnavailableError("openai-whisper is required") from error
        try:
            configured_device = os.getenv("WHISPER_DEVICE", "").strip()
            device = configured_device or ("cuda" if torch.cuda.is_available() else "cpu")
            return whisper.load_model(
                os.getenv("WHISPER_MODEL", "base"),
                download_root=os.getenv("WHISPER_MODEL_CACHE", "/tmp/dearly-whisper"),
                device=device,
            )
        except Exception as error:
            raise ASRUnavailableError(f"could not load Whisper: {error}") from error
