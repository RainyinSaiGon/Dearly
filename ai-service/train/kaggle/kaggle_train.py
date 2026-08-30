"""Offline Kaggle GPU entrypoint for Dearly ECAPA-TDNN training.

Required Kaggle inputs:
- kynthesis/vivos-vietnamese-speech-corpus-for-asr
- dinhdaivu/dearly-ecapa-offline-bundle

The script never needs outbound Internet. The private Dearly bundle contains
Linux wheels, Dearly training code, and pretrained ECAPA-VOXCELEB weights.
VIVOS supplies licensed Vietnamese audio with speaker-labelled directories.
"""

from __future__ import annotations

import hashlib
import json
import os
import shutil
import subprocess
import sys
from datetime import UTC, datetime
from pathlib import Path


INPUT_ROOT = Path("/kaggle/input")
WORKING_ROOT = Path("/kaggle/working")
BUNDLE_INPUT_ROOT = INPUT_ROOT / "dearly-ecapa-offline-bundle"
BUNDLE_ROOT = WORKING_ROOT / "offline-bundle"
VIVOS_ROOT = INPUT_ROOT / "vivos-vietnamese-speech-corpus-for-asr" / "vivos"
AI_ROOT = WORKING_ROOT / "dearly-ai"
DATA_ROOT = WORKING_ROOT / "data" / "vivos"
ARTIFACT_ROOT = WORKING_ROOT / "dearly-artifacts"
CONFIG_PATH = WORKING_ROOT / "train_ecapa_kaggle.yaml"
PRETRAINED_SOURCE = BUNDLE_ROOT / "pretrained" / "spkrec-ecapa-voxceleb"


def run(
    *arguments: str,
    cwd: Path | None = None,
    environment: dict[str, str] | None = None,
) -> None:
    print("+", " ".join(arguments), flush=True)
    subprocess.run(arguments, cwd=cwd, env=environment, check=True)


def stage_offline_bundle() -> None:
    if not BUNDLE_INPUT_ROOT.is_dir():
        raise FileNotFoundError(f"missing Kaggle input: {BUNDLE_INPUT_ROOT}")
    if BUNDLE_ROOT.exists():
        shutil.rmtree(BUNDLE_ROOT)
    BUNDLE_ROOT.mkdir(parents=True)
    for name in ("wheels", "pretrained", "dearly-ai"):
        directory = BUNDLE_INPUT_ROOT / name
        archive = BUNDLE_INPUT_ROOT / f"{name}.zip"
        destination = BUNDLE_ROOT / name
        if directory.is_dir():
            shutil.copytree(directory, destination)
        elif archive.is_file():
            destination.mkdir(parents=True)
            shutil.unpack_archive(str(archive), destination)
        else:
            raise FileNotFoundError(f"offline bundle component is missing: {name}")


def require_inputs() -> None:
    required = {
        "offline bundle": BUNDLE_ROOT,
        "VIVOS corpus": VIVOS_ROOT,
        "pretrained ECAPA": PRETRAINED_SOURCE,
    }
    missing = [f"{label}: {path}" for label, path in required.items() if not path.exists()]
    if missing:
        raise FileNotFoundError("Missing Kaggle inputs:\n" + "\n".join(missing))


def install_dependencies() -> None:
    wheel_directory = BUNDLE_ROOT / "wheels"
    wheels = sorted(str(path) for path in wheel_directory.glob("*.whl"))
    if not wheels:
        raise FileNotFoundError(f"offline wheel bundle is empty: {wheel_directory}")
    run(
        sys.executable,
        "-m",
        "pip",
        "install",
        "--quiet",
        "--no-index",
        "--no-deps",
        *wheels,
    )


def stage_training_code() -> None:
    source = BUNDLE_ROOT / "dearly-ai"
    if AI_ROOT.exists():
        shutil.rmtree(AI_ROOT)
    shutil.copytree(source, AI_ROOT)


def verify_runtime() -> dict[str, object]:
    nvidia_smi_path = shutil.which("nvidia-smi")
    if nvidia_smi_path:
        nvidia_smi = subprocess.run(
            [nvidia_smi_path],
            capture_output=True,
            text=True,
            check=False,
        )
        print(nvidia_smi.stdout or nvidia_smi.stderr, flush=True)
    else:
        print("nvidia-smi: unavailable", flush=True)

    import speechbrain
    import torch
    import torchaudio

    details: dict[str, object] = {
        "python": sys.version,
        "speechbrain": speechbrain.__version__,
        "torch": torch.__version__,
        "torchaudio": torchaudio.__version__,
        "cuda_available": torch.cuda.is_available(),
        "cuda_version": torch.version.cuda,
        "gpu": torch.cuda.get_device_name(0) if torch.cuda.is_available() else None,
    }
    print(json.dumps(details, indent=2), flush=True)
    if not details["cuda_available"]:
        raise RuntimeError("CUDA is unavailable; the Kaggle session must use a GPU.")
    return details


def speaker_split(speaker_id: str) -> str:
    digest = hashlib.sha256(speaker_id.encode("utf-8")).digest()
    return "val" if int.from_bytes(digest[:4], "big") % 10 == 9 else "train"


def link_speaker(source: Path, target: Path) -> int:
    count = 0
    for audio_path in sorted(source.rglob("*.wav")):
        relative = audio_path.relative_to(source)
        destination = target / relative
        destination.parent.mkdir(parents=True, exist_ok=True)
        destination.symlink_to(audio_path)
        count += 1
    return count


def prepare_vivos() -> dict[str, dict[str, int]]:
    source_train = VIVOS_ROOT / "train" / "waves"
    source_test = VIVOS_ROOT / "test" / "waves"
    if not source_train.is_dir() or not source_test.is_dir():
        raise FileNotFoundError(f"unexpected VIVOS layout under {VIVOS_ROOT}")
    if DATA_ROOT.exists():
        shutil.rmtree(DATA_ROOT)

    statistics = {
        "train": {"speakers": 0, "utterances": 0},
        "val": {"speakers": 0, "utterances": 0},
        "test": {"speakers": 0, "utterances": 0},
    }
    train_speakers = sorted(path for path in source_train.iterdir() if path.is_dir())
    for speaker_directory in train_speakers:
        split = speaker_split(speaker_directory.name)
        utterances = link_speaker(
            speaker_directory,
            DATA_ROOT / split / speaker_directory.name,
        )
        if utterances:
            statistics[split]["speakers"] += 1
            statistics[split]["utterances"] += utterances
    test_speakers = sorted(path for path in source_test.iterdir() if path.is_dir())
    for speaker_directory in test_speakers:
        utterances = link_speaker(
            speaker_directory,
            DATA_ROOT / "test" / speaker_directory.name,
        )
        if utterances:
            statistics["test"]["speakers"] += 1
            statistics["test"]["utterances"] += utterances

    if statistics["train"]["speakers"] < 2 or statistics["val"]["speakers"] < 2:
        raise ValueError(f"speaker split is too small: {statistics}")
    print(json.dumps(statistics, indent=2), flush=True)
    return statistics


def create_training_config() -> dict[str, object]:
    import yaml

    source = AI_ROOT / "train" / "hparams" / "train_ecapa.yaml"
    config = yaml.safe_load(source.read_text(encoding="utf-8"))
    config.update(
        {
            "data_folder": str(DATA_ROOT),
            "train_csv": str(WORKING_ROOT / "manifests" / "train.csv"),
            "val_csv": str(WORKING_ROOT / "manifests" / "val.csv"),
            "test_csv": str(WORKING_ROOT / "manifests" / "test.csv"),
            "output_folder": str(ARTIFACT_ROOT / "ECAPA"),
            "pretrained_source": str(PRETRAINED_SOURCE),
            "pretrained_path": str(WORKING_ROOT / "pretrained" / "ecapa-voxceleb"),
            "n_epochs": int(os.getenv("DEARLY_EPOCHS", "20")),
            "batch_size": int(os.getenv("DEARLY_BATCH_SIZE", "32")),
            "num_workers": int(os.getenv("DEARLY_NUM_WORKERS", "2")),
        }
    )
    CONFIG_PATH.write_text(
        yaml.safe_dump(config, sort_keys=False),
        encoding="utf-8",
    )
    return config


def train_and_evaluate() -> None:
    ARTIFACT_ROOT.mkdir(parents=True, exist_ok=True)
    run(
        sys.executable,
        "train/finetune_ecapa.py",
        str(CONFIG_PATH),
        "--prepare",
        cwd=AI_ROOT,
    )
    checkpoint = ARTIFACT_ROOT / "ECAPA" / "best_model.ckpt"
    deploy_checkpoint = ARTIFACT_ROOT / "ecapa_dearly.ckpt"
    shutil.copy2(checkpoint, deploy_checkpoint)

    environment = os.environ.copy()
    environment.update(
        {
            "PYTHONPATH": str(AI_ROOT),
            "ECAPA_MODEL_PATH": str(deploy_checkpoint),
            "ECAPA_MODEL_SOURCE": str(PRETRAINED_SOURCE),
            "ECAPA_MODEL_CACHE": str(WORKING_ROOT / "pretrained" / "ecapa-eval"),
        }
    )
    run(
        sys.executable,
        "-m",
        "train.eval",
        str(DATA_ROOT / "test"),
        "--trials",
        os.getenv("DEARLY_EVALUATION_TRIALS", "10000"),
        "--output",
        str(ARTIFACT_ROOT / "evaluation.json"),
        cwd=AI_ROOT,
        environment=environment,
    )


def main() -> None:
    os.environ.update(
        {
            "HF_HUB_OFFLINE": "1",
            "TRANSFORMERS_OFFLINE": "1",
            "HF_DATASETS_OFFLINE": "1",
        }
    )
    stage_offline_bundle()
    require_inputs()
    install_dependencies()
    stage_training_code()
    hardware = verify_runtime()
    dataset_statistics = prepare_vivos()
    config = create_training_config()
    train_and_evaluate()
    manifest = {
        "completed_at": datetime.now(UTC).isoformat(),
        "dataset": "kynthesis/vivos-vietnamese-speech-corpus-for-asr",
        "dataset_license": "CC BY-NC-SA 4.0",
        "dataset_statistics": dataset_statistics,
        "pretrained_model": "speechbrain/spkrec-ecapa-voxceleb",
        "offline_bundle": "dinhdaivu/dearly-ecapa-offline-bundle",
        "hardware": hardware,
        "training": config,
    }
    (ARTIFACT_ROOT / "run_manifest.json").write_text(
        json.dumps(manifest, indent=2),
        encoding="utf-8",
    )
    print(f"Training complete. Artifacts: {ARTIFACT_ROOT}", flush=True)


if __name__ == "__main__":
    main()
