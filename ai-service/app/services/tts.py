"""Google Cloud Vietnamese text-to-speech service."""

import asyncio
import os
from typing import Any


class TTSUnavailableError(RuntimeError):
    """Raised when Google TTS is unavailable."""


class TTSService:
    def __init__(self, client: Any | None = None) -> None:
        self._client = client

    async def synthesize(self, text: str) -> tuple[bytes, str]:
        if not text.strip():
            raise ValueError("text is required")

        def run() -> tuple[bytes, str]:
            try:
                from google.cloud import texttospeech

                client = self._client or texttospeech.TextToSpeechClient()
                response = client.synthesize_speech(
                    input=texttospeech.SynthesisInput(text=text),
                    voice=texttospeech.VoiceSelectionParams(
                        language_code="vi-VN",
                        name=os.getenv("GOOGLE_TTS_VOICE", "vi-VN-Wavenet-A"),
                    ),
                    audio_config=texttospeech.AudioConfig(
                        audio_encoding=texttospeech.AudioEncoding.MP3,
                        speaking_rate=float(os.getenv("GOOGLE_TTS_RATE", "0.92")),
                    ),
                )
                return bytes(response.audio_content), "audio/mpeg"
            except Exception as error:
                raise TTSUnavailableError(f"speech synthesis failed: {error}") from error

        return await asyncio.to_thread(run)
