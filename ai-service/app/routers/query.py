"""Voice transcription, SID, intent classification, and speech response."""

import base64

from fastapi import APIRouter, File, Form, Header, HTTPException, UploadFile, status

from app.models.ecapa import EcapaTDNN, ModelUnavailableError
from app.routers.identify import parse_enrolled_speakers
from app.services.asr import ASRService, ASRUnavailableError
from app.services.audio import (
    AudioValidationError,
    remove_temporary_audio,
    save_uploaded_audio,
)
from app.services.llm import LLMService
from app.services.tts import TTSService, TTSUnavailableError

router = APIRouter()
IMPLEMENTED = True


def asr_provider() -> ASRService:
    return ASRService.shared()


def model_provider() -> EcapaTDNN:
    return EcapaTDNN.shared()


def llm_provider() -> LLMService:
    return LLMService()


def tts_provider() -> TTSService:
    return TTSService()


@router.post("/")
async def voice_query(
    audio: UploadFile = File(...),
    enrolled_speakers: str = Form("[]"),
    x_user_id: str | None = Header(None),
):
    try:
        speakers = parse_enrolled_speakers(enrolled_speakers)
        path = await save_uploaded_audio(audio)
    except (AudioValidationError, ValueError) as error:
        raise HTTPException(status.HTTP_422_UNPROCESSABLE_ENTITY, str(error)) from error
    try:
        transcript = await asr_provider().transcribe(path)
        identified_user_id = None
        identification_score = 0.0
        if speakers:
            identified_user_id, identification_score = model_provider().identify(path, speakers)
        intent = await llm_provider().classify_intent(transcript)
        response_audio, response_mime = await tts_provider().synthesize(intent["response_text"])
        return {
            "transcript": transcript,
            "intent": intent["intent"],
            "entities": intent["entities"],
            "response_text": intent["response_text"],
            "response_audio_base64": base64.b64encode(response_audio).decode("ascii"),
            "response_audio_mime": response_mime,
            "sv_required": intent["requires_sv"],
            "sv_passed": None,
            "identified_user_id": identified_user_id,
            "identification_score": identification_score,
            "request_user_id": x_user_id,
        }
    except (ASRUnavailableError, ModelUnavailableError, TTSUnavailableError) as error:
        raise HTTPException(status.HTTP_503_SERVICE_UNAVAILABLE, str(error)) from error
    finally:
        remove_temporary_audio(path)
