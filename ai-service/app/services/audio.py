"""Bounded temporary-file handling for uploaded voice audio."""

from pathlib import Path
from tempfile import NamedTemporaryFile

from fastapi import UploadFile

MAX_AUDIO_BYTES = 20 * 1024 * 1024
ALLOWED_SUFFIXES = {".aac", ".flac", ".m4a", ".mp3", ".ogg", ".wav", ".webm"}


class AudioValidationError(ValueError):
    """Raised when an uploaded recording cannot be safely processed."""


async def save_uploaded_audio(upload: UploadFile) -> Path:
    suffix = Path(upload.filename or "recording.wav").suffix.lower()
    if suffix not in ALLOWED_SUFFIXES:
        raise AudioValidationError("unsupported audio format")
    content = await upload.read(MAX_AUDIO_BYTES + 1)
    if not content or len(content) > MAX_AUDIO_BYTES:
        raise AudioValidationError("audio must be between 1 byte and 20 MB")
    with NamedTemporaryFile(suffix=suffix, delete=False) as temporary:
        temporary.write(content)
        return Path(temporary.name)


def remove_temporary_audio(path: Path) -> None:
    path.unlink(missing_ok=True)
