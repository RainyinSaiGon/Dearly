"""Fine-tune SpeechBrain ECAPA-TDNN with an AAM-Softmax speaker head."""

import argparse
import csv
import json
import math
import random
import shutil
from pathlib import Path

import torch
import torch.nn.functional as functional
import torchaudio
import yaml
from speechbrain.inference.speaker import EncoderClassifier
from torch import nn
from torch.utils.data import DataLoader, Dataset

AUDIO_SUFFIXES = {".flac", ".m4a", ".mp3", ".ogg", ".wav"}


def audio_duration(audio_path: Path) -> float:
    info = getattr(torchaudio, "info", None)
    if info is not None:
        metadata = info(str(audio_path))
        return metadata.num_frames / metadata.sample_rate
    signal, sample_rate = torchaudio.load(str(audio_path))
    if sample_rate <= 0:
        raise ValueError(f"invalid sample rate in {audio_path}")
    return signal.shape[-1] / sample_rate


def make_manifest(split_directory: Path, output_csv: Path) -> None:
    rows = []
    if not split_directory.is_dir():
        raise FileNotFoundError(f"dataset split does not exist: {split_directory}")
    for speaker_directory in sorted(path for path in split_directory.iterdir() if path.is_dir()):
        for audio_path in sorted(speaker_directory.rglob("*")):
            if audio_path.suffix.lower() not in AUDIO_SUFFIXES:
                continue
            rows.append(
                {
                    "utt_id": f"{speaker_directory.name}_{audio_path.stem}",
                    "audio_path": str(audio_path.resolve()),
                    "speaker_id": speaker_directory.name,
                    "duration": audio_duration(audio_path),
                }
            )
    if not rows:
        raise ValueError(f"no supported audio found in {split_directory}")
    output_csv.parent.mkdir(parents=True, exist_ok=True)
    with output_csv.open("w", newline="", encoding="utf-8") as manifest:
        writer = csv.DictWriter(manifest, fieldnames=rows[0].keys())
        writer.writeheader()
        writer.writerows(rows)


def read_manifest(path: Path) -> list[dict[str, str]]:
    with path.open(newline="", encoding="utf-8") as manifest:
        rows = list(csv.DictReader(manifest))
    if not rows:
        raise ValueError(f"manifest is empty: {path}")
    return rows


class ManifestDataset(Dataset):
    def __init__(
        self,
        rows: list[dict[str, str]],
        speaker_indexes: dict[str, int],
        sample_rate: int,
        sample_count: int,
        training: bool,
    ) -> None:
        self.rows = rows
        self.speaker_indexes = speaker_indexes
        self.sample_rate = sample_rate
        self.sample_count = sample_count
        self.training = training

    def __len__(self) -> int:
        return len(self.rows)

    def __getitem__(self, index: int) -> tuple[torch.Tensor, int]:
        row = self.rows[index]
        signal, source_rate = torchaudio.load(row["audio_path"])
        signal = signal.mean(dim=0)
        if source_rate != self.sample_rate:
            signal = torchaudio.functional.resample(signal, source_rate, self.sample_rate)
        if signal.numel() > self.sample_count:
            maximum_start = signal.numel() - self.sample_count
            start = random.randint(0, maximum_start) if self.training else maximum_start // 2
            signal = signal[start : start + self.sample_count]
        elif signal.numel() < self.sample_count:
            signal = functional.pad(signal, (0, self.sample_count - signal.numel()))
        return signal, self.speaker_indexes[row["speaker_id"]]


class AAMSoftmaxHead(nn.Module):
    def __init__(self, embedding_dimension: int, speaker_count: int, margin: float, scale: float) -> None:
        super().__init__()
        self.weight = nn.Parameter(torch.empty(speaker_count, embedding_dimension))
        nn.init.xavier_uniform_(self.weight)
        self.margin = margin
        self.scale = scale

    def forward(self, embeddings: torch.Tensor, labels: torch.Tensor) -> torch.Tensor:
        embeddings = functional.normalize(embeddings, dim=1)
        weights = functional.normalize(self.weight, dim=1)
        cosine = functional.linear(embeddings, weights).clamp(-1 + 1e-7, 1 - 1e-7)
        sine = torch.sqrt((1.0 - cosine.square()).clamp_min(1e-7))
        target_cosine = cosine * math.cos(self.margin) - sine * math.sin(self.margin)
        one_hot = functional.one_hot(labels, num_classes=weights.shape[0]).bool()
        logits = torch.where(one_hot, target_cosine, cosine)
        return logits * self.scale


class FineTuningModel(nn.Module):
    def __init__(self, pretrained: EncoderClassifier, head: AAMSoftmaxHead) -> None:
        super().__init__()
        self.compute_features = pretrained.mods.compute_features
        self.mean_var_norm = pretrained.mods.mean_var_norm
        self.embedding_model = pretrained.mods.embedding_model
        self.head = head

    def embeddings(self, waveforms: torch.Tensor) -> torch.Tensor:
        lengths = torch.ones(waveforms.shape[0], device=waveforms.device)
        features = self.compute_features(waveforms)
        features = self.mean_var_norm(features, lengths)
        embeddings = self.embedding_model(features, lengths)
        return embeddings.squeeze(1)

    def forward(self, waveforms: torch.Tensor, labels: torch.Tensor) -> torch.Tensor:
        return self.head(self.embeddings(waveforms), labels)


def train(config: dict, device: torch.device) -> dict[str, float | int | str]:
    train_rows = read_manifest(Path(config["train_csv"]))
    validation_rows = read_manifest(Path(config["val_csv"]))
    train_speakers = sorted({row["speaker_id"] for row in train_rows})
    validation_speakers = sorted({row["speaker_id"] for row in validation_rows})
    if set(train_speakers) & set(validation_speakers):
        raise ValueError("train and validation splits must be speaker-disjoint")

    speaker_indexes = {speaker: index for index, speaker in enumerate(train_speakers)}
    validation_indexes = {speaker: index for index, speaker in enumerate(validation_speakers)}

    sample_rate = int(config.get("sample_rate", 16_000))
    sample_count = int(float(config.get("sentence_len", 3.0)) * sample_rate)
    train_dataset = ManifestDataset(train_rows, speaker_indexes, sample_rate, sample_count, True)
    validation_dataset = ManifestDataset(validation_rows, validation_indexes, sample_rate, sample_count, False)
    workers = int(config.get("num_workers", 0))
    batch_size = int(config.get("batch_size", 16))
    train_loader = DataLoader(train_dataset, batch_size=batch_size, shuffle=True, num_workers=workers)
    validation_loader = DataLoader(validation_dataset, batch_size=batch_size, num_workers=workers)

    source = config.get("pretrained_source", "speechbrain/spkrec-ecapa-voxceleb")
    pretrained = EncoderClassifier.from_hparams(
        source=source,
        savedir=config.get("pretrained_path", "pretrained_models/spkrec-ecapa-voxceleb"),
        run_opts={"device": str(device)},
    )
    head = AAMSoftmaxHead(
        int(config.get("embedding_dim", 192)),
        len(train_speakers),
        float(config.get("margin", 0.2)),
        float(config.get("scale", 30.0)),
    )
    model = FineTuningModel(pretrained, head).to(device)
    optimizer = torch.optim.AdamW(model.parameters(), lr=float(config.get("lr", 1e-4)))
    scheduler = torch.optim.lr_scheduler.CosineAnnealingLR(
        optimizer,
        T_max=int(config.get("n_epochs", 20)),
        eta_min=float(config.get("lr_final", 1e-6)),
    )
    output_directory = Path(config.get("output_folder", "results/ECAPA"))
    output_directory.mkdir(parents=True, exist_ok=True)
    checkpoint_backup_directory = config.get("checkpoint_backup_dir")
    backup_directory = (
        Path(checkpoint_backup_directory) if checkpoint_backup_directory else None
    )
    if backup_directory is not None:
        backup_directory.mkdir(parents=True, exist_ok=True)
    freeze_until = int(config.get("freeze_until_epoch", 0))
    best_loss = float("inf")

    for epoch in range(1, int(config.get("n_epochs", 20)) + 1):
        backbone_trainable = epoch > freeze_until
        for parameter in model.embedding_model.parameters():
            parameter.requires_grad = backbone_trainable
        model.train()
        train_loss = run_epoch(model, train_loader, optimizer, device)
        model.eval()
        with torch.no_grad():
            validation_loss = verification_validation_loss(model, validation_loader, device)
        if not math.isfinite(train_loss) or not math.isfinite(validation_loss):
            raise RuntimeError(
                f"non-finite loss at epoch {epoch}: "
                f"train_loss={train_loss}, val_loss={validation_loss}"
            )
        scheduler.step()
        print(f"epoch={epoch} train_loss={train_loss:.5f} val_loss={validation_loss:.5f}")
        if validation_loss < best_loss:
            best_loss = validation_loss
            checkpoint_path = output_directory / "best_model.ckpt"
            torch.save(
                {
                    "embedding_model": model.embedding_model.state_dict(),
                    "speaker_indexes": speaker_indexes,
                    "epoch": epoch,
                    "validation_loss": validation_loss,
                },
                checkpoint_path,
            )
            if backup_directory is not None:
                shutil.copy2(checkpoint_path, backup_directory / checkpoint_path.name)

    summary = {
        "best_validation_loss": best_loss,
        "speakers": len(train_speakers),
        "checkpoint": str((output_directory / "best_model.ckpt").resolve()),
    }
    (output_directory / "training_summary.json").write_text(
        json.dumps(summary, indent=2),
        encoding="utf-8",
    )
    if backup_directory is not None:
        shutil.copy2(
            output_directory / "training_summary.json",
            backup_directory / "training_summary.json",
        )
    return summary


def run_epoch(
    model: FineTuningModel,
    loader: DataLoader,
    optimizer: torch.optim.Optimizer | None,
    device: torch.device,
) -> float:
    total_loss = 0.0
    examples = 0
    for waveforms, labels in loader:
        waveforms, labels = waveforms.to(device), labels.to(device)
        if optimizer is not None:
            optimizer.zero_grad(set_to_none=True)
        logits = model(waveforms, labels)
        loss = functional.cross_entropy(logits, labels)
        if optimizer is not None:
            loss.backward()
            torch.nn.utils.clip_grad_norm_(model.parameters(), 5.0)
            optimizer.step()
        total_loss += loss.item() * labels.shape[0]
        examples += labels.shape[0]
    return total_loss / max(examples, 1)


def verification_validation_loss(
    model: FineTuningModel,
    loader: DataLoader,
    device: torch.device,
) -> float:
    embeddings, labels = [], []
    for waveforms, batch_labels in loader:
        embeddings.append(model.embeddings(waveforms.to(device)).cpu())
        labels.append(batch_labels)
    vectors = functional.normalize(torch.cat(embeddings), dim=1)
    label_vector = torch.cat(labels)
    similarities = vectors @ vectors.T
    row, column = torch.triu_indices(vectors.shape[0], vectors.shape[0], offset=1)
    pair_scores = similarities[row, column]
    same_speaker = label_vector[row] == label_vector[column]
    if not same_speaker.any() or same_speaker.all():
        raise ValueError("validation needs genuine and impostor speaker pairs")
    genuine_loss = (1.0 - pair_scores[same_speaker]).square().mean()
    impostor_loss = functional.relu(pair_scores[~same_speaker] - 0.2).square().mean()
    return float(genuine_loss + impostor_loss)


def prepare_manifests(config: dict) -> None:
    root = Path(config["data_folder"])
    for split, key in (("train", "train_csv"), ("val", "val_csv"), ("test", "test_csv")):
        make_manifest(root / split, Path(config[key]))


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("config", type=Path)
    parser.add_argument("--prepare", action="store_true", help="generate CSV manifests before training")
    parser.add_argument("--prepare-only", action="store_true")
    args = parser.parse_args()
    config = yaml.safe_load(args.config.read_text(encoding="utf-8"))
    random.seed(int(config.get("seed", 42)))
    torch.manual_seed(int(config.get("seed", 42)))
    if args.prepare or args.prepare_only:
        prepare_manifests(config)
    if not args.prepare_only:
        device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
        print(json.dumps(train(config, device), indent=2))


if __name__ == "__main__":
    main()
