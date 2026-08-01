"""
Router: Speaker Enrollment

Pipeline (W4):
    Audio file (wav/m4a)
        ↓
    Resample to 16 kHz mono              ← TODO(W4): use torchaudio.transforms.Resample
        ↓
    ECAPA-TDNN feature extraction        ← TODO(W4): call EcapaTDNN.extract_embedding()
        ↓
    192-dim embedding vector
        ↓
    Return vector as JSON                ← TODO(W4): serialize as list[float]

The Go backend calls this per phrase (5 total) and stores the embeddings.
After all 5 phrases, Go backend calls ComputeAverageEmbedding.
"""

from fastapi import APIRouter, UploadFile, File, Form
from fastapi.responses import JSONResponse
import tempfile, os

router = APIRouter()


@router.post("/")
async def enroll_speaker(
    audio: UploadFile = File(...),
    phrase_index: int = Form(...),  # 0–4
):
    """
    Accept one audio phrase and return its ECAPA-TDNN embedding vector.

    TODO(W4) — Step by step:
    1. Save uploaded audio to a temp file:
           with tempfile.NamedTemporaryFile(suffix='.wav', delete=False) as f:
               f.write(await audio.read())
               tmp_path = f.name

    2. Validate phrase_index is in range 0–4.

    3. Load ECAPA-TDNN model (singleton, loaded once at startup):
           from app.models.ecapa import EcapaTDNN
           model = EcapaTDNN()  # loads from ECAPA_MODEL_PATH env var

    4. Extract embedding:
           embedding = model.extract_embedding(tmp_path)  → list[float] (192-dim)

    5. Clean up temp file.

    6. Return:
           { "phrase_index": int, "embedding": list[float] }
    """
    # TODO(W4): implement the steps above
    return JSONResponse(
        status_code=501,
        content={"status": "not_implemented", "detail": "Enrollment endpoint — see TODO(W4)"},
    )
