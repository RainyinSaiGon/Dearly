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
from fastapi import FastAPI

app = FastAPI(
    title="Dearly AI Service",
    version="0.1.0",
    description="Voice processing microservice for Dearly elder-care app",
)


@app.get("/health")
async def health_check():
    return {"status": "ok"}


# ─── Route stubs ────────────────────────────────────────────────────
from app.routers import enroll, verify, identify, query

app.include_router(enroll.router, prefix="/enroll", tags=["enrollment"])
app.include_router(verify.router, prefix="/verify", tags=["verification"])
app.include_router(identify.router, prefix="/identify", tags=["identification"])
app.include_router(query.router, prefix="/query", tags=["assistant"])
