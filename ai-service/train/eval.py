"""Evaluate ECAPA speaker verification with EER and minDCF."""

import argparse
import bisect
import json
import random
from itertools import combinations
from pathlib import Path

from app.models.ecapa import EcapaTDNN

AUDIO_SUFFIXES = {".flac", ".m4a", ".mp3", ".ogg", ".wav"}


def collect_speakers(test_directory: Path) -> dict[str, list[Path]]:
    speakers = {
        directory.name: sorted(
            path for path in directory.rglob("*") if path.suffix.lower() in AUDIO_SUFFIXES
        )
        for directory in test_directory.iterdir()
        if directory.is_dir()
    }
    return {speaker: recordings for speaker, recordings in speakers.items() if recordings}


def generate_trials(
    speakers: dict[str, list[Path]],
    trial_count: int,
    seed: int,
) -> list[tuple[Path, Path, int]]:
    genuine = [
        (first, second, 1)
        for recordings in speakers.values()
        for first, second in combinations(recordings, 2)
    ]
    if not genuine:
        raise ValueError("test data needs at least one speaker with two recordings")
    speaker_ids = sorted(speakers)
    if len(speaker_ids) < 2:
        raise ValueError("test data needs at least two speakers")
    generator = random.Random(seed)
    generator.shuffle(genuine)
    blocks = []
    cumulative_sizes = []
    possible_impostors = 0
    for first_speaker, second_speaker in combinations(speaker_ids, 2):
        first_recordings = speakers[first_speaker]
        second_recordings = speakers[second_speaker]
        possible_impostors += len(first_recordings) * len(second_recordings)
        blocks.append((first_recordings, second_recordings))
        cumulative_sizes.append(possible_impostors)
    target_each = min(trial_count // 2, len(genuine), possible_impostors)
    if target_each == 0:
        raise ValueError("trial_count must allow genuine and impostor trials")
    impostor = []
    for rank in generator.sample(range(possible_impostors), target_each):
        block_index = bisect.bisect_right(cumulative_sizes, rank)
        block_start = cumulative_sizes[block_index - 1] if block_index else 0
        first_recordings, second_recordings = blocks[block_index]
        local_rank = rank - block_start
        first = first_recordings[local_rank // len(second_recordings)]
        second = second_recordings[local_rank % len(second_recordings)]
        impostor.append((first, second, 0))
    trials = genuine[:target_each] + impostor
    generator.shuffle(trials)
    return trials


def score_trials(
    model: EcapaTDNN,
    trials: list[tuple[Path, Path, int]],
    cache: dict[Path, list[float]] | None = None,
) -> tuple[list[float], list[float]]:
    embeddings = cache if cache is not None else {}

    def embedding(path: Path) -> list[float]:
        if path not in embeddings:
            embeddings[path] = model.extract_embedding(path)
        return embeddings[path]

    positive, negative = [], []
    for first, second, label in trials:
        score = model.cosine_similarity(embedding(first), embedding(second))
        (positive if label == 1 else negative).append(score)
    return positive, negative


def compute_top1_accuracy(
    model: EcapaTDNN,
    speakers: dict[str, list[Path]],
    cache: dict[Path, list[float]] | None = None,
    enrollment_count: int = 5,
) -> dict[str, float | int]:
    embeddings = cache if cache is not None else {}

    def embedding(path: Path) -> list[float]:
        if path not in embeddings:
            embeddings[path] = model.extract_embedding(path)
        return embeddings[path]

    profiles: dict[str, list[float]] = {}
    queries: list[tuple[str, Path]] = []
    for speaker, recordings in speakers.items():
        if len(recordings) < 2:
            continue
        used_for_enrollment = min(enrollment_count, len(recordings) - 1)
        enrolled = [embedding(path) for path in recordings[:used_for_enrollment]]
        dimension = len(enrolled[0])
        if any(len(vector) != dimension for vector in enrolled):
            raise ValueError("embedding dimensions do not match")
        profiles[speaker] = [
            sum(vector[index] for vector in enrolled) / len(enrolled)
            for index in range(dimension)
        ]
        queries.extend((speaker, path) for path in recordings[used_for_enrollment:])
    if len(profiles) < 2 or not queries:
        raise ValueError("SID evaluation needs two speakers with enrollment and query audio")
    correct = 0
    for expected_speaker, query_path in queries:
        query = embedding(query_path)
        predicted = max(
            profiles,
            key=lambda speaker: model.cosine_similarity(query, profiles[speaker]),
        )
        correct += predicted == expected_speaker
    return {
        "sid_top1_accuracy": correct / len(queries),
        "sid_queries": len(queries),
        "sid_speakers": len(profiles),
    }


def compute_metrics(
    positive: list[float],
    negative: list[float],
    target_probability: float = 0.01,
) -> dict[str, float]:
    if not positive or not negative:
        raise ValueError("both genuine and impostor scores are required")
    thresholds = sorted(set(positive + negative))
    operating_points = []
    for threshold in thresholds:
        false_reject = sum(score < threshold for score in positive) / len(positive)
        false_accept = sum(score >= threshold for score in negative) / len(negative)
        cost = false_reject * target_probability + false_accept * (1 - target_probability)
        operating_points.append((threshold, false_reject, false_accept, cost))
    threshold, false_reject, false_accept, _ = min(
        operating_points,
        key=lambda item: abs(item[1] - item[2]),
    )
    min_dcf = min(point[3] for point in operating_points)
    return {
        "eer": (false_reject + false_accept) / 2,
        "eer_percent": (false_reject + false_accept) * 50,
        "eer_threshold": threshold,
        "min_dcf": min_dcf,
    }


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("test_directory", type=Path)
    parser.add_argument("--trials", type=int, default=10_000)
    parser.add_argument("--seed", type=int, default=42)
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()
    speakers = collect_speakers(args.test_directory)
    trials = generate_trials(speakers, args.trials, args.seed)
    model = EcapaTDNN.shared()
    embedding_cache: dict[Path, list[float]] = {}
    positive, negative = score_trials(model, trials, embedding_cache)
    result = {
        **compute_metrics(positive, negative),
        **compute_top1_accuracy(model, speakers, embedding_cache),
        "genuine_trials": len(positive),
        "impostor_trials": len(negative),
        "speakers": len(speakers),
    }
    serialized = json.dumps(result, indent=2)
    print(serialized)
    if args.output:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(serialized + "\n", encoding="utf-8")


if __name__ == "__main__":
    main()
