"""Materialize VoxVietnam from Hugging Face into speaker directories.

The Hugging Face dataset stores decoded audio and speaker IDs in a streaming
dataset.  This script writes the selected samples as 16 kHz WAV files in the
directory layout consumed by ``finetune_ecapa.py`` and ``eval.py``.

The official ``train_small`` partition is split into train/validation by
speaker ID.  The official ``test`` partition is kept untouched, so no speaker
can appear in both development and final evaluation.
"""

from __future__ import annotations

import argparse
import hashlib
import re
from collections import defaultdict
from pathlib import Path
from typing import Any, Iterable


DEFAULT_DATASET = "hustep-lab/VoxVietnam-Dataset"
DEFAULT_TRAIN_SPLIT = "train_small"
DEFAULT_TEST_SPLIT = "test"
SPEAKER_BUCKETS = 10


def speaker_split(speaker_id: str) -> str:
    """Return a deterministic, speaker-disjoint development split."""

    digest = hashlib.sha256(speaker_id.encode("utf-8")).digest()
    bucket = int.from_bytes(digest[:4], "big") % SPEAKER_BUCKETS
    return "val" if bucket == SPEAKER_BUCKETS - 1 else "train"


def safe_name(value: str) -> str:
    """Make a dataset ID safe to use as a local directory or file stem."""

    cleaned = re.sub(r"[^A-Za-z0-9_.-]+", "_", value).strip("._")
    return cleaned or "unknown"


def load_stream(dataset_id: str, split: str) -> Iterable[dict[str, Any]]:
    try:
        from datasets import load_dataset
    except ImportError as error:  # pragma: no cover - exercised in CLI setup
        raise RuntimeError(
            "Install training dependencies first: "
            "python -m pip install -r train/requirements.txt"
        ) from error

    return load_dataset(dataset_id, split=split, streaming=True)


def audio_to_array(audio: dict[str, Any]) -> tuple[Any, int]:
    import numpy as np

    if not audio or audio.get("array") is None:
        raise ValueError("sample has no decoded audio")
    array = np.asarray(audio["array"], dtype=np.float32)
    if array.size == 0:
        raise ValueError("sample has empty audio")
    if array.ndim > 1:
        # Hugging Face Audio normally returns mono 1-D arrays.  For any
        # multi-channel sample, average the channel dimension conservatively.
        array = array.mean(axis=-1 if array.shape[-1] <= 8 else 0)
    return array, int(audio.get("sampling_rate") or 16_000)


def write_sample(output_path: Path, audio: dict[str, Any], min_duration: float) -> bool:
    import soundfile as sf

    array, sample_rate = audio_to_array(audio)
    if array.size / sample_rate < min_duration:
        return False
    output_path.parent.mkdir(parents=True, exist_ok=True)
    sf.write(str(output_path), array, sample_rate, subtype="PCM_16")
    return True


def materialize_split(
    rows: Iterable[dict[str, Any]],
    output_root: Path,
    split: str,
    max_speakers: int,
    max_utterances_per_speaker: int,
    min_duration: float,
    apply_dev_split: bool,
) -> dict[str, int]:
    counts: defaultdict[str, int] = defaultdict(int)
    selected: set[str] = set()
    skipped = 0

    for row_index, row in enumerate(rows):
        speaker = str(row.get("speaker", "")).strip()
        if not speaker:
            skipped += 1
            continue

        target_split = speaker_split(speaker) if apply_dev_split else split
        if target_split != split:
            continue

        if speaker not in selected:
            if max_speakers and len(selected) >= max_speakers:
                continue
            selected.add(speaker)
        if max_utterances_per_speaker and counts[speaker] >= max_utterances_per_speaker:
            continue

        try:
            output_path = (
                output_root
                / target_split
                / safe_name(speaker)
                / f"{row_index:08d}.wav"
            )
            if write_sample(output_path, row["audio"], min_duration):
                counts[speaker] += 1
            else:
                skipped += 1
        except (KeyError, TypeError, ValueError, OSError):
            skipped += 1

    return {
        "speakers": len(selected),
        "utterances": sum(counts.values()),
        "skipped": skipped,
    }


def materialize_development(
    rows: Iterable[dict[str, Any]],
    output_root: Path,
    max_speakers: int,
    max_utterances_per_speaker: int,
    min_duration: float,
) -> dict[str, dict[str, int]]:
    """Write train and validation in one pass over the streaming train split."""

    selected: dict[str, set[str]] = {"train": set(), "val": set()}
    counts: defaultdict[tuple[str, str], int] = defaultdict(int)
    skipped = 0

    for row_index, row in enumerate(rows):
        speaker = str(row.get("speaker", "")).strip()
        if not speaker:
            skipped += 1
            continue
        split = speaker_split(speaker)
        if speaker not in selected[split]:
            if max_speakers and len(selected[split]) >= max_speakers:
                continue
            selected[split].add(speaker)
        key = (split, speaker)
        if max_utterances_per_speaker and counts[key] >= max_utterances_per_speaker:
            continue
        try:
            output_path = output_root / split / safe_name(speaker) / f"{row_index:08d}.wav"
            if write_sample(output_path, row["audio"], min_duration):
                counts[key] += 1
            else:
                skipped += 1
        except (KeyError, TypeError, ValueError, OSError):
            skipped += 1

    return {
        split: {
            "speakers": len(selected[split]),
            "utterances": sum(value for (item_split, _), value in counts.items() if item_split == split),
            "skipped": skipped,
        }
        for split in ("train", "val")
    }


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--dataset-id", default=DEFAULT_DATASET)
    parser.add_argument("--train-split", default=DEFAULT_TRAIN_SPLIT)
    parser.add_argument("--test-split", default=DEFAULT_TEST_SPLIT)
    parser.add_argument("--output", type=Path, default=Path("data/voxvietnam"))
    parser.add_argument(
        "--max-speakers",
        type=int,
        default=0,
        help="cap speakers per materialized source split; 0 means all",
    )
    parser.add_argument(
        "--max-utterances-per-speaker",
        type=int,
        default=0,
        help="cap utterances per speaker; 0 means all",
    )
    parser.add_argument("--min-duration", type=float, default=1.0)
    parser.add_argument("--skip-test", action="store_true")
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    args.output.mkdir(parents=True, exist_ok=True)

    if args.max_speakers < 0 or args.max_utterances_per_speaker < 0:
        raise SystemExit("speaker and utterance caps must be non-negative")

    print(f"Streaming {args.dataset_id}:{args.train_split}")
    development_stats = materialize_development(
        load_stream(args.dataset_id, args.train_split),
        args.output,
        args.max_speakers,
        args.max_utterances_per_speaker,
        args.min_duration,
    )
    for split, stats in development_stats.items():
        print(f"{split}: {stats}")

    if not args.skip_test:
        print(f"Streaming {args.dataset_id}:{args.test_split}")
        test_stats = materialize_split(
            load_stream(args.dataset_id, args.test_split),
            args.output,
            "test",
            args.max_speakers,
            args.max_utterances_per_speaker,
            args.min_duration,
            apply_dev_split=False,
        )
        print(f"test: {test_stats}")

    print(f"Dataset written to {args.output.resolve()}")


if __name__ == "__main__":
    main()
