"""ASR Service — Whisper-based speech-to-text."""

# TODO (W5): Implement using openai-whisper or whisper API
# import whisper


class ASRService:
    def __init__(self):
        # self.model = whisper.load_model("base")
        pass

    async def transcribe(self, audio_path: str) -> str:
        """Transcribe audio file to text (Vietnamese + English)."""
        raise NotImplementedError("ASR not yet implemented")
