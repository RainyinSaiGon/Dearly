"""Extract an ECAPA embedding for one enrollment phrase."""

from fastapi import APIRouter, File, Form, HTTPException, UploadFile, status

from app.models.ecapa import EcapaTDNN, ModelUnavailableError
from app.services.audio import (
    AudioValidationError,
    remove_temporary_audio,
    save_uploaded_audio,
)
from app.services.asr import ASRService, ASRUnavailableError, NoSpeechDetectedError

router = APIRouter()
IMPLEMENTED = True


def model_provider() -> EcapaTDNN:
    return EcapaTDNN.shared()


def asr_provider() -> ASRService:
    return ASRService.shared()


@router.post("/")
async def enroll_speaker(
    audio: UploadFile = File(...),
    phrase_index: int = Form(...),
):
    if phrase_index not in range(5):
        raise HTTPException(status.HTTP_422_UNPROCESSABLE_ENTITY, "phrase_index must be between 0 and 4")
    try:
        path = await save_uploaded_audio(audio)
    except AudioValidationError as error:
        raise HTTPException(status.HTTP_422_UNPROCESSABLE_ENTITY, str(error)) from error
    try:
        # Client VAD is the first quality gate. This prevents a bypassed
        # client from creating an enrollment vector from silence or noise.
        await asr_provider().transcribe(path)
        embedding = model_provider().extract_embedding(path)
        return {"phrase_index": phrase_index, "embedding": embedding}
    except NoSpeechDetectedError as error:
        raise HTTPException(status.HTTP_422_UNPROCESSABLE_ENTITY, str(error)) from error
    except ASRUnavailableError as error:
        raise HTTPException(status.HTTP_503_SERVICE_UNAVAILABLE, str(error)) from error
    except ModelUnavailableError as error:
        raise HTTPException(status.HTTP_503_SERVICE_UNAVAILABLE, str(error)) from error
    finally:
        remove_temporary_audio(path)
