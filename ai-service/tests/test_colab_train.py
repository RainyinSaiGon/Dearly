"""Unit checks for deterministic, no-network Colab runner helpers."""

from pathlib import Path

from train.colab.colab_train import fingerprint_tree, speaker_split


def test_speaker_split_is_deterministic() -> None:
    assert speaker_split("VIVOSSPK01") == speaker_split("VIVOSSPK01")
    assert speaker_split("VIVOSSPK01") in {"train", "val"}


def test_tree_fingerprint_changes_when_staged_contents_change(tmp_path: Path) -> None:
    (tmp_path / "nested").mkdir()
    audio = tmp_path / "nested" / "sample.wav"
    audio.write_bytes(b"first")
    initial = fingerprint_tree(tmp_path)
    audio.write_bytes(b"second-value")
    assert fingerprint_tree(tmp_path) != initial
