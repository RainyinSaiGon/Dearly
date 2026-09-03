"""Bounded temporary-file handling and normalization for uploaded voice audio."""

import asyncio
import subprocess
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
        uploaded_path = Path(temporary.name)

    # Android's MediaRecorder produces AAC in an M4A container. Whisper can
    # read it, but the CPU torchaudio backend used by ECAPA cannot. Normalize
    # compressed uploads once here so ASR, enrollment, verification, and SID
    # all receive the same mono 16 kHz WAV input.
    if suffix == ".wav":
        return uploaded_path

    with NamedTemporaryFile(suffix=".wav", delete=False) as normalized:
        normalized_path = Path(normalized.name)
    try:
        await asyncio.to_thread(
            subprocess.run,
            [
                "ffmpeg",
                "-nostdin",
                "-v",
                "error",
                "-y",
                "-i",
                str(uploaded_path),
                "-ac",
                "1",
                "-ar",
                "16000",
                str(normalized_path),
            ],
            check=True,
            stdout=subprocess.DEVNULL,
            stderr=subprocess.PIPE,
        )
    except (FileNotFoundError, subprocess.CalledProcessError) as error:
        remove_temporary_audio(normalized_path)
        remove_temporary_audio(uploaded_path)
        raise AudioValidationError("audio could not be decoded") from error

    remove_temporary_audio(uploaded_path)
    return normalized_path


def remove_temporary_audio(path: Path) -> None:
    path.unlink(missing_ok=True)
