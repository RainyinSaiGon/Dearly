"""Extract an ECAPA embedding for one enrollment phrase."""

from fastapi import APIRouter, File, Form, HTTPException, UploadFile, status

from app.models.ecapa import EcapaTDNN, ModelUnavailableError
from app.services.audio import (
    AudioValidationError,
    remove_temporary_audio,
    save_uploaded_audio,
)

router = APIRouter()
IMPLEMENTED = True


def model_provider() -> EcapaTDNN:
    return EcapaTDNN.shared()


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
        embedding = model_provider().extract_embedding(path)
        return {"phrase_index": phrase_index, "embedding": embedding}
    except ModelUnavailableError as error:
        raise HTTPException(status.HTTP_503_SERVICE_UNAVAILABLE, str(error)) from error
    finally:
        remove_temporary_audio(path)
