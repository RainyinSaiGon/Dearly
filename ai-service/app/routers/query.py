"""
Router: Voice Assistant Query — full pipeline

Complete flow (W5):

    Audio file (Elder speaks)
        ↓
    [1] ASR — Whisper                     → transcript (str)
        ↓
    [2] SID — ECAPA-TDNN 1-to-N           → identified user_id (or None)
        ↓
    [3] LLM — GPT-4o function-calling     → intent, entities, requires_sv (bool)
        ↓
    [4] SV (only if requires_sv=True)     → passed (bool)
        |
        ├── FAIL → TTS("Xác minh giọng nói thất bại") → return error audio
        |
        └── PASS ↓
    [5] Execute task                       → result_text (str)
        ↓
    [6] TTS — Google Cloud                → response audio (bytes / URL)
        ↓
    Return JSON: { transcript, intent, response_text, response_audio_url,
                   sv_required, sv_passed, identified_user_id }
"""

from fastapi import APIRouter, UploadFile, File, Header
from fastapi.responses import JSONResponse
from typing import Optional

router = APIRouter()


@router.post("/")
async def voice_query(
    audio: UploadFile = File(...),
    x_user_id: Optional[str] = Header(None),          # Elder's userID from Go backend
    x_enrollment_embedding: Optional[str] = Header(None),  # Base64 avg embedding for SV
):
    """
    Full voice assistant pipeline.

    TODO(W5) — Step by step:

    Step 1 — ASR:
        tmp = save_audio_to_tmp(audio)
        from app.services.asr import ASRService
        transcript = await ASRService().transcribe(tmp)
        # Returns Vietnamese or English text

    Step 2 — Speaker Identification:
        # (Optional but enriches personalization)
        # Get all enrolled embeddings from Go backend (passed as header or body)
        # identified_user_id = await identify(tmp, enrolled_speakers)

    Step 3 — LLM Intent Classification:
        from app.services.llm import LLMService
        intent_result = await LLMService().classify_intent(transcript)
        # Returns:
        # {
        #   "intent": "CALL_CONTACT" | "CHECK_MED" | "MARK_TAKEN" | "ASK_TIME" | ...,
        #   "entities": { "contact_name": "con Lan", ... },
        #   "requires_sv": True/False,
        #   "response_text": "Bạn muốn gọi cho con Lan không?"   ← GPT-4o draft response
        # }

    Step 4 — Speaker Verification (if requires_sv):
        if intent_result["requires_sv"]:
            from app.routers.verify import verify_speaker  # or call service directly
            sv_result = await verify(tmp, x_enrollment_embedding)
            if not sv_result["passed"]:
                error_audio = await TTSService().synthesize("Xác minh giọng nói thất bại")
                return { "sv_required": True, "sv_passed": False, "response_audio": error_audio }

    Step 5 — Execute Task:
        # Tasks implemented here or dispatched back to Go backend via HTTP
        # Examples:
        #   CALL_CONTACT  → return { action: "INITIATE_CALL", contact_id: ... }
        #   MARK_TAKEN    → POST Go backend /api/v1/medications/:id/taken
        #   ASK_TIME      → response_text = f"Bây giờ là {datetime.now().strftime('%H:%M')}"
        #   ASK_MED       → fetch med schedule from Go backend and format as Vietnamese text

    Step 6 — TTS:
        from app.services.tts import TTSService
        audio_url = await TTSService().synthesize(response_text, output_path)

    Step 7 — Return:
        return {
            "transcript": str,
            "intent": str,
            "response_text": str,
            "response_audio_url": str,
            "sv_required": bool,
            "sv_passed": bool,
            "identified_user_id": str | None,
        }
    """
    # TODO(W5): implement the steps above
    return JSONResponse(
        status_code=501,
        content={"status": "not_implemented", "detail": "Voice query pipeline — see TODO(W5)"},
    )
