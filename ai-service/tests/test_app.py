from fastapi.testclient import TestClient

from app.main import app


client = TestClient(app)


def test_health_check_reports_process_is_alive():
    response = client.get("/health")

    assert response.status_code == 200
    assert response.json() == {"status": "ok"}


def test_readiness_rejects_incomplete_service(tmp_path, monkeypatch):
    monkeypatch.setenv("ECAPA_MODEL_PATH", str(tmp_path / "missing.ckpt"))

    response = client.get("/ready")

    assert response.status_code == 503
    checks = response.json()["checks"]
    assert checks["model"] is False
    assert checks["enrollment"] is True
    assert checks["verification"] is True
    assert checks["identification"] is True
    assert checks["query"] is True


def test_readiness_accepts_complete_service(tmp_path, monkeypatch):
    model = tmp_path / "model.ckpt"
    model.touch()
    monkeypatch.setenv("ECAPA_MODEL_PATH", str(model))
    response = client.get("/ready")

    assert response.status_code == 200
    assert response.json()["status"] == "ready"
    assert all(response.json()["checks"].values())
def test_readiness_accepts_pretrained_model_source(tmp_path, monkeypatch):
    monkeypatch.delenv("ECAPA_MODEL_PATH", raising=False)
    monkeypatch.setenv("ECAPA_MODEL_SOURCE", "speechbrain/spkrec-ecapa-voxceleb")

    response = client.get("/ready")

    assert response.status_code == 200
    assert response.json()["checks"]["model"] is True


def test_voice_routes_are_registered():
    paths = {route.path for route in app.routes}

    assert {"/enroll/", "/verify/", "/identify/", "/query/"} <= paths
