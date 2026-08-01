"""
Router: Speaker Verification

Pipeline (W5):
    Audio file (incoming voice)
        ↓
    Resample to 16 kHz mono
        ↓
    ECAPA-TDNN → 192-dim embedding
        ↓
    Cosine similarity vs. stored enrollment embedding (passed from Go backend)
        ↓
    similarity >= 0.80  →  { "passed": true,  "score": float }
    similarity <  0.80  →  { "passed": false, "score": float }
"""

from fastapi import APIRouter, UploadFile, File, Form
from fastapi.responses import JSONResponse
import json

router = APIRouter()


@router.post("/")
async def verify_speaker(
    audio: UploadFile = File(...),
    enrollment_embedding: str = Form(...),  # JSON-serialized list[float] from Go backend
):
    """
    Verify whether the speaker in `audio` matches the `enrollment_embedding`.

    TODO(W5) — Step by step:
    1. Save audio to a temp file (same as enroll.py step 1).

    2. Deserialize enrollment_embedding:
           stored_vec = json.loads(enrollment_embedding)  # list[float]

    3. Extract embedding from incoming audio:
           model = EcapaTDNN()
           incoming_vec = model.extract_embedding(tmp_path)

    4. Compute cosine similarity:
           score = model.cosine_similarity(incoming_vec, stored_vec)

    5. Apply threshold (read from env SV_COSINE_THRESHOLD, default 0.80):
           passed = score >= float(os.getenv("SV_COSINE_THRESHOLD", "0.80"))

    6. Clean up temp file.

    7. Return:
           { "passed": bool, "score": float }
    """
    # TODO(W5): implement the steps above
    return JSONResponse(
        status_code=501,
        content={"status": "not_implemented", "detail": "Verification endpoint — see TODO(W5)"},
    )
