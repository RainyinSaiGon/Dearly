"""
Dearly AI Service — FastAPI entry point.

Provides:
  - Speaker Verification (SV)
  - Speaker Identification (SID)
  - ASR (Whisper)
  - LLM intent routing (OpenAI GPT-4o)
  - TTS (Google Cloud)
"""

import os
from pathlib import Path

from fastapi import FastAPI, status
from fastapi.responses import JSONResponse

from app.models.ecapa import EcapaTDNN
from app.routers import enroll, identify, query, verify

app = FastAPI(
    title="Dearly AI Service",
    version="0.1.0",
    description="Voice processing microservice for Dearly elder-care app",
)


@app.get("/health")
async def health_check():
    return {"status": "ok"}


@app.get("/ready", responses={status.HTTP_503_SERVICE_UNAVAILABLE: {}})
async def readiness_check():
    file_checks = {
        "model": EcapaTDNN.configured_model_exists(),
        "google_credentials": _file_is_available("GOOGLE_APPLICATION_CREDENTIALS"),
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


def _file_is_available(environment_variable: str) -> bool:
    configured_path = os.getenv(environment_variable, "").strip()
    return bool(configured_path and Path(configured_path).is_file())


app.include_router(enroll.router, prefix="/enroll", tags=["enrollment"])
app.include_router(verify.router, prefix="/verify", tags=["verification"])
app.include_router(identify.router, prefix="/identify", tags=["identification"])
app.include_router(query.router, prefix="/query", tags=["assistant"])
