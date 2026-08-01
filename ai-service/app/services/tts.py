"""TTS Service — Google Cloud Text-to-Speech (Vietnamese)."""

# TODO (W5): Implement using google-cloud-texttospeech
# from google.cloud import texttospeech


class TTSService:
    def __init__(self):
        # self.client = texttospeech.TextToSpeechClient()
        pass

    async def synthesize(self, text: str, output_path: str) -> str:
        """
        Convert text to speech audio file (Vietnamese, vi-VN-Wavenet).
        Returns path to generated audio file.
        """
        raise NotImplementedError("TTS not yet implemented")
