"""One-to-many ECAPA speaker identification."""

import json
from typing import Any

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


def parse_enrolled_speakers(serialized: str) -> list[dict[str, Any]]:
    parsed = json.loads(serialized)
    if not isinstance(parsed, list):
        raise ValueError("enrolled_speakers must be a list")
    speakers = []
    dimension = None
    for item in parsed:
        if not isinstance(item, dict) or not str(item.get("user_id", "")).strip():
            raise ValueError("each enrolled speaker requires user_id")
        embedding = validate_embedding(item.get("embedding"), expected_dimension=dimension)
        dimension = len(embedding)
        speakers.append({"user_id": str(item["user_id"]), "embedding": embedding})
    return speakers


@router.post("/")
async def identify_speaker(
    audio: UploadFile = File(...),
    enrolled_speakers: str = Form(...),
):
    try:
        speakers = parse_enrolled_speakers(enrolled_speakers)
        path = await save_uploaded_audio(audio)
    except (AudioValidationError, InvalidEmbeddingError, json.JSONDecodeError, ValueError) as error:
        raise HTTPException(status.HTTP_422_UNPROCESSABLE_ENTITY, str(error)) from error
    try:
        user_id, score = model_provider().identify(path, speakers)
        return {"user_id": user_id, "score": score}
    except (InvalidEmbeddingError, ModelUnavailableError) as error:
        raise HTTPException(status.HTTP_503_SERVICE_UNAVAILABLE, str(error)) from error
    finally:
        remove_temporary_audio(path)
