"""One-to-one ECAPA speaker verification."""

import json

from fastapi import APIRouter, File, Form, HTTPException, UploadFile, status

from app.models.ecapa import (
    EcapaTDNN,
    InvalidEmbeddingError,
    ModelUnavailableError,
    validate_embedding,
)
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
async def verify_speaker(
    audio: UploadFile = File(...),
    enrollment_embedding: str = Form(...),
):
    try:
        enrolled = validate_embedding(json.loads(enrollment_embedding))
        path = await save_uploaded_audio(audio)
    except (AudioValidationError, InvalidEmbeddingError, json.JSONDecodeError) as error:
        raise HTTPException(status.HTTP_422_UNPROCESSABLE_ENTITY, str(error)) from error
    try:
        passed, score = model_provider().verify(path, enrolled)
        return {"passed": passed, "score": score}
    except (InvalidEmbeddingError, ModelUnavailableError) as error:
        raise HTTPException(status.HTTP_503_SERVICE_UNAVAILABLE, str(error)) from error
    finally:
        remove_temporary_audio(path)
