"""Run Dearly ECAPA-TDNN baseline, smoke, and final training on Colab.

This entrypoint is sent to a Colab runtime with ``colab exec -f``.  Before
running it, upload these two files to the active runtime:

* ``/content/dearly-input/kaggle_token.txt``: a newly generated Kaggle token.
* ``/content/dearly-input/run_config.json``: non-secret run metadata.

The token is used only to download the two already-prepared Kaggle datasets,
then is removed.  It is deliberately never written to the run manifest.
"""

from __future__ import annotations

import csv
import hashlib
import json
import os
import shutil
import subprocess
import sys
import time
import wave
from datetime import UTC, datetime
from pathlib import Path
from typing import Any


WORKING_ROOT = Path("/content/dearly-training")
INPUT_ROOT = Path("/content/dearly-input")
DOWNLOAD_ROOT = WORKING_ROOT / "downloads"
BUNDLE_ROOT = WORKING_ROOT / "offline-bundle"
AI_ROOT = WORKING_ROOT / "dearly-ai"
DATA_ROOT = WORKING_ROOT / "data" / "vivos"
ARTIFACT_ROOT = WORKING_ROOT / "artifacts"
TOKEN_PATH = INPUT_ROOT / "kaggle_token.txt"
CONFIG_PATH = INPUT_ROOT / "run_config.json"
VIVOS_SLUG = "kynthesis/vivos-vietnamese-speech-corpus-for-asr"
BUNDLE_SLUG = "dinhdaivu/dearly-ecapa-offline-bundle"
EXPECTED_SPLITS = {
    "train": {"speakers": 41, "utterances": 10_310},
    "val": {"speakers": 5, "utterances": 1_350},
    "test": {"speakers": 19, "utterances": 760},
}


def run(*arguments: str, cwd: Path | None = None, environment: dict[str, str] | None = None) -> None:
    print("+", " ".join(arguments), flush=True)
    subprocess.run(arguments, cwd=cwd, env=environment, check=True)


def load_run_config() -> dict[str, Any]:
    if not CONFIG_PATH.is_file():
        raise FileNotFoundError(f"missing non-secret run config: {CONFIG_PATH}")
    config = json.loads(CONFIG_PATH.read_text(encoding="utf-8"))
    for key in ("run_id", "drive_root", "source_revision", "colab_cli_version"):
        if not str(config.get(key, "")).strip():
            raise ValueError(f"run config must contain a non-empty {key!r}")
    return config


def require_cuda() -> dict[str, Any]:
    import speechbrain
    import torch
    import torchaudio

    details: dict[str, Any] = {
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
        raise RuntimeError("CUDA is unavailable; create the Colab session with --gpu T4")
    return details


def install_kaggle_client() -> None:
    run(sys.executable, "-m", "pip", "install", "--quiet", "kaggle==2.2.4")


def download_dataset(slug: str, destination: Path, token: str) -> None:
    if destination.exists():
        shutil.rmtree(destination)
    destination.mkdir(parents=True)
    environment = os.environ.copy()
    environment["KAGGLE_API_TOKEN"] = token
    run(
        sys.executable,
        "-m",
        "kaggle",
        "datasets",
        "download",
        "-d",
        slug,
        "-p",
        str(destination),
        "--unzip",
        environment=environment,
    )


def find_directory(root: Path, required_relative_path: Path) -> Path:
    for candidate in (root, *root.rglob(required_relative_path.parts[0])):
        if candidate.is_dir() and (candidate / required_relative_path).is_dir():
            return candidate
    raise FileNotFoundError(f"could not find {required_relative_path} under {root}")


def fingerprint_tree(root: Path) -> str:
    """Return a stable, content-light identifier for a staged data tree."""
    digest = hashlib.sha256()
    for path in sorted(candidate for candidate in root.rglob("*") if candidate.is_file()):
        relative = path.relative_to(root).as_posix()
        digest.update(relative.encode("utf-8"))
        digest.update(b"\0")
        digest.update(str(path.stat().st_size).encode("ascii"))
        digest.update(b"\n")
    return digest.hexdigest()


def stage_inputs() -> tuple[Path, Path]:
    if not TOKEN_PATH.is_file():
        raise FileNotFoundError(f"missing temporary Kaggle token file: {TOKEN_PATH}")
    token = TOKEN_PATH.read_text(encoding="utf-8").strip()
    if not token:
        raise ValueError("temporary Kaggle token file is empty")
    try:
        install_kaggle_client()
        download_dataset(VIVOS_SLUG, DOWNLOAD_ROOT / "vivos", token)
        download_dataset(BUNDLE_SLUG, DOWNLOAD_ROOT / "bundle", token)
    finally:
        TOKEN_PATH.unlink(missing_ok=True)

    raw_vivos = find_directory(DOWNLOAD_ROOT / "vivos", Path("vivos") / "train" / "waves")
    raw_bundle = find_directory(
        DOWNLOAD_ROOT / "bundle", Path("pretrained") / "spkrec-ecapa-voxceleb"
    )
    if BUNDLE_ROOT.exists():
        shutil.rmtree(BUNDLE_ROOT)
    shutil.copytree(raw_bundle, BUNDLE_ROOT)
    return raw_vivos / "vivos", BUNDLE_ROOT


def install_offline_dependencies(bundle_root: Path) -> None:
    wheels_directory = bundle_root / "wheels"
    if not any(wheels_directory.glob("*.whl")):
        raise FileNotFoundError("offline bundle has no dependency wheels")
    run(
        sys.executable,
        "-m",
        "pip",
        "install",
        "--quiet",
        "--no-index",
        "--no-deps",
        "--find-links",
        str(wheels_directory),
        "speechbrain==1.1.0",
        "HyperPyYAML==1.2.2",
        "ruamel.yaml==0.18.17",
    )
    try:
        import sentencepiece  # noqa: F401
        import speechbrain  # noqa: F401
    except ImportError as error:
        raise RuntimeError(
            "the Colab Python ABI is incompatible with the prepared dependencies"
        ) from error


def stage_training_code(bundle_root: Path) -> Path:
    source = bundle_root / "dearly-ai"
    if not source.is_dir():
        raise FileNotFoundError(f"offline bundle is missing training code: {source}")
    if AI_ROOT.exists():
        shutil.rmtree(AI_ROOT)
    shutil.copytree(source, AI_ROOT)
    pretrained_source = bundle_root / "pretrained" / "spkrec-ecapa-voxceleb"
    hyperparams = pretrained_source / "hyperparams.yaml"
    if not hyperparams.is_file():
        raise FileNotFoundError(f"offline pretrained model is missing {hyperparams}")
    contents = hyperparams.read_text(encoding="utf-8")
    remote_reference = "pretrained_path: speechbrain/spkrec-ecapa-voxceleb"
    if remote_reference not in contents:
        raise ValueError("unexpected pretrained_path in offline hyperparams.yaml")
    hyperparams.write_text(
        contents.replace(remote_reference, f"pretrained_path: {pretrained_source}", 1),
        encoding="utf-8",
    )
    return pretrained_source


def speaker_split(speaker_id: str) -> str:
    digest = hashlib.sha256(speaker_id.encode("utf-8")).digest()
    return "val" if int.from_bytes(digest[:4], "big") % 10 == 9 else "train"


def link_speaker(source: Path, target: Path) -> int:
    count = 0
    for audio_path in sorted(source.rglob("*.wav")):
        destination = target / audio_path.relative_to(source)
        destination.parent.mkdir(parents=True, exist_ok=True)
        destination.symlink_to(audio_path)
        count += 1
    return count


def prepare_vivos(vivos_root: Path) -> dict[str, dict[str, int]]:
    source_train = vivos_root / "train" / "waves"
    source_test = vivos_root / "test" / "waves"
    if not source_train.is_dir() or not source_test.is_dir():
        raise FileNotFoundError(f"unexpected VIVOS layout under {vivos_root}")
    if DATA_ROOT.exists():
        shutil.rmtree(DATA_ROOT)
    statistics = {split: {"speakers": 0, "utterances": 0} for split in EXPECTED_SPLITS}
    for speaker_directory in sorted(path for path in source_train.iterdir() if path.is_dir()):
        split = speaker_split(speaker_directory.name)
        count = link_speaker(speaker_directory, DATA_ROOT / split / speaker_directory.name)
        if count:
            statistics[split]["speakers"] += 1
            statistics[split]["utterances"] += count
    for speaker_directory in sorted(path for path in source_test.iterdir() if path.is_dir()):
        count = link_speaker(speaker_directory, DATA_ROOT / "test" / speaker_directory.name)
        if count:
            statistics["test"]["speakers"] += 1
            statistics["test"]["utterances"] += count
    if statistics != EXPECTED_SPLITS:
        raise ValueError(f"unexpected VIVOS split; expected {EXPECTED_SPLITS}, got {statistics}")
    print(json.dumps(statistics, indent=2), flush=True)
    return statistics


def environment_for_model(pretrained_source: Path, checkpoint: Path | None = None) -> dict[str, str]:
    environment = os.environ.copy()
    environment.update(
        {
            "PYTHONPATH": str(AI_ROOT),
            "ECAPA_MODEL_SOURCE": str(pretrained_source),
            "ECAPA_MODEL_CACHE": str(WORKING_ROOT / "pretrained-cache"),
            "HF_HUB_OFFLINE": "1",
            "TRANSFORMERS_OFFLINE": "1",
        }
    )
    if checkpoint is None:
        environment.pop("ECAPA_MODEL_PATH", None)
    else:
        environment["ECAPA_MODEL_PATH"] = str(checkpoint)
    return environment


def write_training_config(
    *,
    label: str,
    epochs: int,
    pretrained_source: Path,
    drive_run_root: Path,
) -> tuple[Path, Path]:
    import yaml

    source = AI_ROOT / "train" / "hparams" / "train_ecapa.yaml"
    config = yaml.safe_load(source.read_text(encoding="utf-8"))
    output_folder = ARTIFACT_ROOT / label / "ECAPA"
    config.update(
        {
            "data_folder": str(DATA_ROOT),
            "train_csv": str(WORKING_ROOT / "manifests" / label / "train.csv"),
            "val_csv": str(WORKING_ROOT / "manifests" / label / "val.csv"),
            "test_csv": str(WORKING_ROOT / "manifests" / label / "test.csv"),
            "output_folder": str(output_folder),
            "checkpoint_backup_dir": str(drive_run_root / label / "ECAPA"),
            "pretrained_source": str(pretrained_source),
            "pretrained_path": str(WORKING_ROOT / "pretrained-cache" / label),
            "n_epochs": epochs,
            "batch_size": 32,
            "num_workers": 2,
        }
    )
    path = WORKING_ROOT / "configs" / f"{label}.yaml"
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(yaml.safe_dump(config, sort_keys=False), encoding="utf-8")
    return path, output_folder


def prepare_training_manifests(label: str) -> None:
    """Write manifests without relying on the removed ``torchaudio.info`` API."""
    manifest_root = WORKING_ROOT / "manifests" / label
    manifest_root.mkdir(parents=True, exist_ok=True)
    for split in ("train", "val", "test"):
        split_root = DATA_ROOT / split
        rows = []
        for audio_path in sorted(split_root.rglob("*.wav")):
            with wave.open(str(audio_path.resolve()), "rb") as audio:
                sample_rate = audio.getframerate()
                if sample_rate <= 0:
                    raise ValueError(f"invalid sample rate in {audio_path}")
                duration = audio.getnframes() / sample_rate
            rows.append(
                {
                    "utt_id": f"{audio_path.parent.name}_{audio_path.stem}",
                    "audio_path": str(audio_path.resolve()),
                    "speaker_id": audio_path.relative_to(split_root).parts[0],
                    "duration": duration,
                }
            )
        if not rows:
            raise ValueError(f"no WAV recordings found in {split_root}")
        manifest_path = manifest_root / f"{split}.csv"
        with manifest_path.open("w", newline="", encoding="utf-8") as manifest:
            writer = csv.DictWriter(manifest, fieldnames=rows[0].keys())
            writer.writeheader()
            writer.writerows(rows)


def evaluate(*, pretrained_source: Path, checkpoint: Path | None, output: Path) -> None:
    output.parent.mkdir(parents=True, exist_ok=True)
    run(
        sys.executable,
        "-m",
        "train.eval",
        str(DATA_ROOT / "test"),
        "--trials",
        "10000",
        "--seed",
        "42",
        "--output",
        str(output),
        cwd=AI_ROOT,
        environment=environment_for_model(pretrained_source, checkpoint),
    )


def copy_new_best_checkpoint(checkpoint: Path, backup_directory: Path) -> tuple[int, int]:
    """Copy a newly observed best checkpoint both as latest and an immutable backup."""
    stat = checkpoint.stat()
    signature = (stat.st_mtime_ns, stat.st_size)
    timestamp = datetime.now(UTC).strftime("%Y%m%dT%H%M%S%fZ")
    backup_directory.mkdir(parents=True, exist_ok=True)
    shutil.copy2(checkpoint, backup_directory / "best_model.ckpt")
    shutil.copy2(checkpoint, backup_directory / f"best_model_{timestamp}.ckpt")
    return signature


def run_training_with_backups(
    *, config: Path, output_folder: Path, drive_run_root: Path
) -> None:
    """Run training while persisting each changed best checkpoint to Drive."""
    command = [sys.executable, "train/finetune_ecapa.py", str(config)]
    print("+", " ".join(command), flush=True)
    process = subprocess.Popen(command, cwd=AI_ROOT)
    checkpoint = output_folder / "best_model.ckpt"
    backup_directory = drive_run_root / output_folder.parent.name / output_folder.name
    last_signature: tuple[int, int] | None = None
    while process.poll() is None:
        if checkpoint.is_file():
            signature = (checkpoint.stat().st_mtime_ns, checkpoint.stat().st_size)
            if signature != last_signature:
                last_signature = copy_new_best_checkpoint(checkpoint, backup_directory)
        time.sleep(5)
    if process.returncode:
        raise subprocess.CalledProcessError(process.returncode, command)
    if checkpoint.is_file():
        signature = (checkpoint.stat().st_mtime_ns, checkpoint.stat().st_size)
        if signature != last_signature:
            copy_new_best_checkpoint(checkpoint, backup_directory)


def train_run(*, label: str, epochs: int, pretrained_source: Path, drive_run_root: Path) -> Path:
    config, output_folder = write_training_config(
        label=label,
        epochs=epochs,
        pretrained_source=pretrained_source,
        drive_run_root=drive_run_root,
    )
    prepare_training_manifests(label)
    run_training_with_backups(
        config=config,
        output_folder=output_folder,
        drive_run_root=drive_run_root,
    )
    checkpoint = output_folder / "best_model.ckpt"
    if not checkpoint.is_file():
        raise FileNotFoundError(f"{label} training did not create {checkpoint}")
    summary = output_folder / "training_summary.json"
    if not summary.is_file():
        raise FileNotFoundError(f"{label} training did not create {summary}")
    local_summary = ARTIFACT_ROOT / f"{label}_training_summary.json"
    shutil.copy2(summary, local_summary)
    shutil.copy2(local_summary, drive_run_root / local_summary.name)
    evaluation = ARTIFACT_ROOT / f"{label}_evaluation.json"
    evaluate(pretrained_source=pretrained_source, checkpoint=checkpoint, output=evaluation)
    shutil.copy2(evaluation, drive_run_root / evaluation.name)
    return checkpoint


def verify_deployment_checkpoint(pretrained_source: Path, checkpoint: Path) -> None:
    sample = next((DATA_ROOT / "test").rglob("*.wav"), None)
    if sample is None:
        raise FileNotFoundError("cannot validate deployment checkpoint without test audio")
    code = (
        "from app.models.ecapa import EcapaTDNN; "
        f"embedding = EcapaTDNN().extract_embedding({str(sample)!r}); "
        "import math; "
        "assert len(embedding) == 192 and all(math.isfinite(value) for value in embedding); "
        "print({'embedding_dimension': len(embedding), 'finite': True})"
    )
    run(
        sys.executable,
        "-c",
        code,
        cwd=AI_ROOT,
        environment=environment_for_model(pretrained_source, checkpoint),
    )


def write_manifest(path: Path, manifest: dict[str, Any], drive_run_root: Path) -> None:
    path.write_text(json.dumps(manifest, indent=2), encoding="utf-8")
    shutil.copy2(path, drive_run_root / path.name)


def main() -> None:
    run_config = load_run_config()
    drive_root = Path(run_config["drive_root"]).expanduser()
    if not Path("/content/drive").is_dir():
        raise FileNotFoundError(
            "Google Drive is not mounted at /content/drive; run colab drivemount first"
        )
    drive_run_root = drive_root / run_config["run_id"]
    drive_run_root.mkdir(parents=True, exist_ok=True)
    if WORKING_ROOT.exists():
        shutil.rmtree(WORKING_ROOT)
    WORKING_ROOT.mkdir(parents=True)

    vivos_root, bundle_root = stage_inputs()
    install_offline_dependencies(bundle_root)
    pretrained_source = stage_training_code(bundle_root)
    hardware = require_cuda()
    statistics = prepare_vivos(vivos_root)
    manifest: dict[str, Any] = {
        "started_at": datetime.now(UTC).isoformat(),
        "run_id": run_config["run_id"],
        "source_revision": run_config["source_revision"],
        "colab_cli_version": run_config["colab_cli_version"],
        "dataset": VIVOS_SLUG,
        "dataset_license": "CC BY-NC-SA 4.0",
        "dataset_tree_fingerprint": fingerprint_tree(vivos_root),
        "offline_bundle": BUNDLE_SLUG,
        "offline_bundle_tree_fingerprint": fingerprint_tree(bundle_root),
        "hardware": hardware,
        "dataset_statistics": statistics,
        "training": {
            "smoke_epochs": 1,
            "final_epochs": 20,
            "batch_size": 32,
            "sample_rate": 16000,
            "crop_seconds": 3.0,
            "freeze_until_epoch": 5,
            "learning_rate": 1e-4,
            "learning_rate_final": 1e-6,
            "aam_margin": 0.2,
            "aam_scale": 30,
            "evaluation_trials": 10000,
        },
    }
    manifest_path = ARTIFACT_ROOT / "run_manifest.json"
    ARTIFACT_ROOT.mkdir(parents=True, exist_ok=True)
    write_manifest(manifest_path, manifest, drive_run_root)

    baseline = ARTIFACT_ROOT / "baseline_evaluation.json"
    evaluate(pretrained_source=pretrained_source, checkpoint=None, output=baseline)
    shutil.copy2(baseline, drive_run_root / baseline.name)

    smoke_checkpoint = train_run(
        label="smoke", epochs=1, pretrained_source=pretrained_source, drive_run_root=drive_run_root
    )
    final_checkpoint = train_run(
        label="final", epochs=20, pretrained_source=pretrained_source, drive_run_root=drive_run_root
    )
    final_summary = ARTIFACT_ROOT / "final_training_summary.json"
    training_summary = ARTIFACT_ROOT / "training_summary.json"
    shutil.copy2(final_summary, training_summary)
    shutil.copy2(training_summary, drive_run_root / training_summary.name)
    deployment_checkpoint = ARTIFACT_ROOT / "ecapa_dearly.ckpt"
    shutil.copy2(final_checkpoint, deployment_checkpoint)
    verify_deployment_checkpoint(pretrained_source, deployment_checkpoint)
    shutil.copy2(deployment_checkpoint, drive_run_root / deployment_checkpoint.name)
    shutil.copy2(ARTIFACT_ROOT / "final_evaluation.json", ARTIFACT_ROOT / "evaluation.json")
    shutil.copy2(ARTIFACT_ROOT / "evaluation.json", drive_run_root / "evaluation.json")
    manifest.update(
        {
            "completed_at": datetime.now(UTC).isoformat(),
            "baseline_evaluation": str(baseline),
            "smoke_checkpoint": str(smoke_checkpoint),
            "smoke_evaluation": str(ARTIFACT_ROOT / "smoke_evaluation.json"),
            "final_checkpoint": str(final_checkpoint),
            "final_evaluation": str(ARTIFACT_ROOT / "evaluation.json"),
            "deployment_checkpoint": str(deployment_checkpoint),
        }
    )
    write_manifest(manifest_path, manifest, drive_run_root)
    archive_base = WORKING_ROOT / "dearly-training-artifacts"
    archive_path = Path(shutil.make_archive(str(archive_base), "zip", ARTIFACT_ROOT))
    shutil.copy2(archive_path, drive_run_root / archive_path.name)
    print(f"Training complete. Artifacts: {ARTIFACT_ROOT}", flush=True)


if __name__ == "__main__":
    main()
