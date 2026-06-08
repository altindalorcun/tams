"""Tests for the FastAPI application entrypoint."""

from fastapi.testclient import TestClient

import main


def test_health_endpoint_returns_ok() -> None:
    with TestClient(main.app) as client:
        response = client.get("/health")
    assert response.status_code == 200
    assert response.json() == {"status": "ok"}


def test_consumer_disabled_when_configured(monkeypatch) -> None:
    monkeypatch.setenv("ENABLE_CONSUMER", "false")
    monkeypatch.setenv("PII_HASH_SALT", "health-test-salt")
    main.get_settings.cache_clear()
    try:
        with TestClient(main.app) as client:
            response = client.get("/health")
        assert response.status_code == 200
    finally:
        main.get_settings.cache_clear()
