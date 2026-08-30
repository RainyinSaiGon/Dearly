import json

import pytest
from fastapi.testclient import TestClient

from app.main import app
from app.models.ecapa import EcapaTDNN, InvalidEmbeddingError
from app.routers import enroll, identify, query, verify
from app.services.llm import LLMService
from train.eval import compute_metrics, compute_top1_accuracy, generate_trials

client = TestClient(app)


class FakeSpeakerModel:
    def extract_embedding(self, _path):
        return [1.0, 0.0]

    def verify(self, _path, enrolled):
        return enrolled == [1.0, 0.0], 0.97

    def identify(self, _path, speakers):
        return speakers[0]["user_id"], 0.93


class FakeASR:
    async def transcribe(self, _path):
        return "Tôi đã uống thuốc huyết áp"


class FakeLLM:
    async def classify_intent(self, _transcript):
        return {
            "intent": "MARK_TAKEN",
            "entities": {"medication_name": "huyết áp"},
            "requires_sv": True,
            "response_text": "Bác vui lòng xác minh giọng nói.",
        }


@pytest.fixture
def fake_model(monkeypatch):
    model = FakeSpeakerModel()
    monkeypatch.setattr(enroll, "model_provider", lambda: model)
    monkeypatch.setattr(verify, "model_provider", lambda: model)
    monkeypatch.setattr(identify, "model_provider", lambda: model)
    monkeypatch.setattr(query, "model_provider", lambda: model)
    return model


def test_enrollment_returns_embedding(fake_model):
    response = client.post(
        "/enroll/",
        files={"audio": ("phrase.wav", b"audio", "audio/wav")},
        data={"phrase_index": "2"},
    )

    assert response.status_code == 200
    assert response.json() == {"phrase_index": 2, "embedding": [1.0, 0.0]}


def test_verification_and_identification(fake_model):
    verification = client.post(
        "/verify/",
        files={"audio": ("verify.wav", b"audio", "audio/wav")},
        data={"enrollment_embedding": "[1, 0]"},
    )
    identification = client.post(
        "/identify/",
        files={"audio": ("identify.wav", b"audio", "audio/wav")},
        data={
            "enrolled_speakers": json.dumps(
                [{"user_id": "elder-1", "embedding": [1, 0]}]
            )
        },
    )

    assert verification.json() == {"passed": True, "score": 0.97}
    assert identification.json() == {"user_id": "elder-1", "score": 0.93}


def test_query_runs_full_non_authoritative_pipeline(fake_model, monkeypatch):
    monkeypatch.setattr(query, "asr_provider", lambda: FakeASR())
    monkeypatch.setattr(query, "llm_provider", lambda: FakeLLM())

    response = client.post(
        "/query/",
        files={"audio": ("query.wav", b"audio", "audio/wav")},
        data={
            "enrolled_speakers": json.dumps(
                [{"user_id": "elder-1", "embedding": [1, 0]}]
            )
        },
        headers={"X-User-ID": "elder-1"},
    )

    body = response.json()
    assert response.status_code == 200
    assert body["intent"] == "MARK_TAKEN"
    assert body["sv_required"] is True
    assert body["sv_passed"] is None
    assert body["identified_user_id"] == "elder-1"
    assert body["response_text"] == "Bác vui lòng xác minh giọng nói."
    assert "response_audio_base64" not in body
@pytest.mark.asyncio
async def test_local_intent_policy_cannot_be_weakened_by_missing_openai(monkeypatch):
    monkeypatch.delenv("OPENAI_API_KEY", raising=False)

    result = await LLMService().classify_intent("Tôi đã uống thuốc huyết áp")

    assert result["intent"] == "MARK_TAKEN"
    assert result["requires_sv"] is True


def test_embedding_and_evaluation_math():
    assert EcapaTDNN.cosine_similarity([1, 0], [1, 0]) == 1
    with pytest.raises(InvalidEmbeddingError):
        EcapaTDNN.cosine_similarity([1, 0], [1])

    metrics = compute_metrics([0.8, 0.9], [0.1, 0.2])
    assert metrics["eer"] == 0
    assert metrics["min_dcf"] == 0


def test_trial_generation_caps_imbalanced_pairs(tmp_path):
    first = [tmp_path / f"first-{index}.wav" for index in range(5)]
    second = [tmp_path / "second.wav"]

    trials = generate_trials({"first": first, "second": second}, 100, seed=42)

    assert sum(label == 1 for _, _, label in trials) == 5
    assert sum(label == 0 for _, _, label in trials) == 5


def test_sid_top1_accuracy(tmp_path):
    class VectorModel:
        vectors = {
            "a-enroll.wav": [1.0, 0.0],
            "a-query.wav": [0.9, 0.1],
            "b-enroll.wav": [0.0, 1.0],
            "b-query.wav": [0.1, 0.9],
        }

        def extract_embedding(self, path):
            return self.vectors[path.name]

        cosine_similarity = staticmethod(EcapaTDNN.cosine_similarity)

    speakers = {
        "a": [tmp_path / "a-enroll.wav", tmp_path / "a-query.wav"],
        "b": [tmp_path / "b-enroll.wav", tmp_path / "b-query.wav"],
    }

    result = compute_top1_accuracy(VectorModel(), speakers)

    assert result["sid_top1_accuracy"] == 1
    assert result["sid_queries"] == 2
