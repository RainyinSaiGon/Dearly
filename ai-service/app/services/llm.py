"""Structured intent routing with an OpenAI-backed and deterministic path."""

import json
import os
import re
import threading
import unicodedata
from difflib import SequenceMatcher
from typing import Any

PROTECTED_INTENTS = {"CALL_CONTACT", "MARK_TAKEN", "UPDATE_SETTINGS"}
KNOWN_INTENTS = PROTECTED_INTENTS | {
    "ASK_DATE",
    "ASK_TIME",
    "CHECK_MEDICATIONS",
    "GREETING",
    "UNKNOWN",
}

SYSTEM_PROMPT = """You classify Vietnamese elder-care voice requests.
Return JSON with exactly: display_transcript, intent, entities, response_text.
Allowed intents: ASK_TIME, ASK_DATE, CHECK_MEDICATIONS, CALL_CONTACT,
MARK_TAKEN, UPDATE_SETTINGS, GREETING, UNKNOWN.
Never claim an action was completed. Keep response_text short and respectful.
For display_transcript, correct only obvious Vietnamese punctuation, capitalization,
diacritics, and unambiguous speech-recognition spelling errors. Never invent, remove,
or change the meaning of words; when uncertain, return the original wording. Never
translate the transcript or replace it with words from another language.
For CALL_CONTACT extract contact_name. For MARK_TAKEN extract medication_name when spoken."""


class LLMService:
    _instance: "LLMService | None" = None
    _lock = threading.Lock()

    def __init__(self, client: Any | None = None) -> None:
        self._client = client

    @classmethod
    def shared(cls) -> "LLMService":
        if cls._instance is None:
            with cls._lock:
                if cls._instance is None:
                    cls._instance = cls()
        return cls._instance

    async def classify_intent(self, transcript: str) -> dict[str, Any]:
        transcript = transcript.strip()
        if not transcript:
            raise ValueError("transcript is required")
        result = None
        if os.getenv("OPENAI_API_KEY", "").strip():
            result = await self._classify_with_openai(transcript)
        if result is None:
            result = classify_locally(transcript)
        intent = str(result.get("intent", "UNKNOWN")).upper()
        if intent not in KNOWN_INTENTS:
            intent = "UNKNOWN"
        entities = result.get("entities")
        if not isinstance(entities, dict):
            entities = {}
        response_text = str(result.get("response_text", "")).strip()
        if not response_text:
            response_text = local_response(intent, entities)
        display_transcript = safe_display_transcript(
            transcript,
            str(result.get("display_transcript", transcript)).strip(),
        )
        return {
            "display_transcript": display_transcript,
            "intent": intent,
            "entities": entities,
            "requires_sv": intent in PROTECTED_INTENTS,
            "response_text": response_text,
        }

    async def _classify_with_openai(self, transcript: str) -> dict[str, Any] | None:
        try:
            if self._client is None:
                from openai import AsyncOpenAI

                self._client = AsyncOpenAI(api_key=os.environ["OPENAI_API_KEY"])
            response = await self._client.chat.completions.create(
                model=os.getenv("OPENAI_INTENT_MODEL", "gpt-4o-mini"),
                messages=[
                    {"role": "system", "content": SYSTEM_PROMPT},
                    {"role": "user", "content": transcript},
                ],
                response_format={"type": "json_object"},
                temperature=0,
            )
            content = response.choices[0].message.content or "{}"
            parsed = json.loads(content)
            return parsed if isinstance(parsed, dict) else None
        except Exception:
            return None


def safe_display_transcript(transcript: str, candidate: str) -> str:
    """Permit small Vietnamese corrections, but reject translations or rewrites."""
    if not candidate or len(candidate) > len(transcript) * 2:
        return transcript
    if any(character.isalpha() and "LATIN" not in unicodedata.name(character, "") for character in candidate):
        return transcript

    original_words = _normalized_words(transcript)
    candidate_words = _normalized_words(candidate)
    if not original_words or not candidate_words:
        return transcript
    shared_words = set(original_words) & set(candidate_words)
    similar_enough = SequenceMatcher(
        None,
        " ".join(original_words),
        " ".join(candidate_words),
    ).ratio() >= 0.7
    if not shared_words and not similar_enough:
        return transcript
    if any(word.isdigit() and word not in candidate_words for word in original_words):
        return transcript
    return candidate


def _normalized_words(value: str) -> list[str]:
    normalized = unicodedata.normalize("NFD", value.casefold()).replace("đ", "d")
    without_accents = "".join(character for character in normalized if not unicodedata.combining(character))
    return re.findall(r"[a-z0-9]+", without_accents)


def classify_locally(transcript: str) -> dict[str, Any]:
    normalized = transcript.casefold()
    if any(phrase in normalized for phrase in ("đã uống", "uống xong", "đánh dấu thuốc")):
        medication = _tail_after(normalized, ("thuốc",))
        return _result("MARK_TAKEN", {"medication_name": medication} if medication else {})
    if any(phrase in normalized for phrase in ("gọi cho", "gọi điện", "video call")):
        contact = _tail_after(normalized, ("gọi cho", "gọi điện cho"))
        return _result("CALL_CONTACT", {"contact_name": contact} if contact else {})
    if any(phrase in normalized for phrase in ("cài đặt", "âm lượng", "ngôn ngữ", "giao diện")):
        return _result("UPDATE_SETTINGS", {})
    if any(phrase in normalized for phrase in ("mấy giờ", "bao nhiêu giờ", "giờ hiện tại")):
        return _result("ASK_TIME", {})
    if any(phrase in normalized for phrase in ("hôm nay ngày", "ngày bao nhiêu", "thứ mấy")):
        return _result("ASK_DATE", {})
    if "thuốc" in normalized:
        return _result("CHECK_MEDICATIONS", {})
    if any(phrase in normalized for phrase in ("xin chào", "chào dearly", "chào bạn")):
        return _result("GREETING", {})
    return _result("UNKNOWN", {})


def local_response(intent: str, entities: dict[str, Any]) -> str:
    responses = {
        "ASK_TIME": "Tôi sẽ đọc giờ hiện tại cho bác.",
        "ASK_DATE": "Tôi sẽ đọc ngày hôm nay cho bác.",
        "CHECK_MEDICATIONS": "Tôi sẽ kiểm tra lịch thuốc hôm nay.",
        "CALL_CONTACT": "Bác vui lòng xác minh giọng nói trước khi gọi.",
        "MARK_TAKEN": "Bác vui lòng xác minh giọng nói để xác nhận liều thuốc.",
        "UPDATE_SETTINGS": "Bác vui lòng xác minh giọng nói trước khi đổi cài đặt.",
        "GREETING": "Chào bác, Dearly đang lắng nghe.",
        "UNKNOWN": "Tôi chưa hiểu rõ. Bác vui lòng nói lại nhé.",
    }
    return responses.get(intent, responses["UNKNOWN"])


def _result(intent: str, entities: dict[str, str]) -> dict[str, Any]:
    return {
        "intent": intent,
        "entities": entities,
        "response_text": local_response(intent, entities),
    }


def _tail_after(text: str, markers: tuple[str, ...]) -> str:
    for marker in markers:
        match = re.search(rf"{re.escape(marker)}\s+(.+)$", text)
        if match:
            return match.group(1).strip(" .,!?")
    return ""
