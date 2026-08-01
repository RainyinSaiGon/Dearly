"""
Router: Speaker Identification (SID)

Pipeline (W5):
    Audio file (incoming voice)
        ↓
    ECAPA-TDNN → 192-dim embedding
        ↓
    Cosine similarity vs. ALL enrolled speakers in the system
        ↓
    Best match above threshold  →  { "user_id": str, "score": float }
    No match                    →  { "user_id": null, "score": float }

Unlike SV (1-to-1), SID is 1-to-N: we compare against every enrolled user.
"""

from fastapi import APIRouter, UploadFile, File, Form
from fastapi.responses import JSONResponse
import json

router = APIRouter()


@router.post("/")
async def identify_speaker(
    audio: UploadFile = File(...),
    enrolled_speakers: str = Form(...),  # JSON: [{"user_id": str, "embedding": list[float]}]
):
    """
    Identify which enrolled speaker is present in `audio`.

    TODO(W5) — Step by step:
    1. Save audio to a temp file.

    2. Deserialize enrolled_speakers:
           speakers = json.loads(enrolled_speakers)
           # [{"user_id": "uuid", "embedding": [0.12, -0.03, ...]}, ...]

    3. Extract embedding from incoming audio:
           model = EcapaTDNN()
           incoming_vec = model.extract_embedding(tmp_path)

    4. Compute cosine similarity against each enrolled speaker:
           scores = [
               (s["user_id"], model.cosine_similarity(incoming_vec, s["embedding"]))
               for s in speakers
           ]

    5. Find the best match:
           best_user_id, best_score = max(scores, key=lambda x: x[1])

    6. Apply threshold (same SV_COSINE_THRESHOLD = 0.80):
           if best_score >= threshold:
               return { "user_id": best_user_id, "score": best_score }
           else:
               return { "user_id": null, "score": best_score }

    7. Clean up temp file.
    """
    # TODO(W5): implement the steps above
    return JSONResponse(
        status_code=501,
        content={"status": "not_implemented", "detail": "Identification endpoint — see TODO(W5)"},
    )
