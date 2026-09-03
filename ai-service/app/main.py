"""
Dearly AI Service — FastAPI entry point.

Provides:
  - Speaker Verification (SV)
  - Speaker Identification (SID)
  - ASR (Whisper)
  - LLM intent routing (OpenAI GPT-4o)
  - Vietnamese TTS (on-device Android)
"""

import asyncio
from contextlib import asynccontextmanager
from pathlib import Path

from fastapi import FastAPI, status
from fastapi.responses import JSONResponse
from dotenv import load_dotenv

from app.models.ecapa import EcapaTDNN
from app.routers import enroll, identify, query, verify
from app.services.asr import ASRService


# Local runs start from either the repository root or ai-service/. Docker uses
# env_file, so loading these files is harmless there and makes local Uvicorn
# runs use the same configuration contract.
_repository_root = Path(__file__).resolve().parents[2]
load_dotenv(_repository_root / ".env")
load_dotenv(Path(__file__).resolve().parents[1] / ".env")

@asynccontextmanager
async def lifespan(_app: FastAPI):
    """Load the configured ASR model before accepting voice requests."""
    await asyncio.to_thread(ASRService.shared)
    yield


app = FastAPI(
    title="Dearly AI Service",
    version="0.1.0",
    description="Voice processing microservice for Dearly elder-care app",
    lifespan=lifespan,
)


@app.get("/health")
async def health_check():
    return {"status": "ok"}


@app.get("/ready", responses={status.HTTP_503_SERVICE_UNAVAILABLE: {}})
async def readiness_check():
    file_checks = {
        "model": EcapaTDNN.configured_model_exists(),
    }
    capability_checks = {
        "enrollment": enroll.IMPLEMENTED,
        "verification": verify.IMPLEMENTED,
        "identification": identify.IMPLEMENTED,
        "query": query.IMPLEMENTED,
    }
    checks = {**file_checks, **capability_checks}
    if not all(checks.values()):
        return JSONResponse(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            content={
                "status": "not_ready",
                "checks": checks,
            },
        )
    return {"status": "ready", "checks": checks}
app.include_router(enroll.router, prefix="/enroll", tags=["enrollment"])
app.include_router(verify.router, prefix="/verify", tags=["verification"])
app.include_router(identify.router, prefix="/identify", tags=["identification"])
app.include_router(query.router, prefix="/query", tags=["assistant"])
