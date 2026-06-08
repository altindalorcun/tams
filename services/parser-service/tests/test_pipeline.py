"""Pipeline glue tests for the consumer message-handling path.

These exercise decode -> parse -> publish without a live Kafka broker by
injecting a fake producer. The full broker-backed integration test
(``transcript.raw`` -> ``transcript.parsed``) is part of the dedicated testing
phase and requires a running Kafka instance.
"""

import base64
import json
from pathlib import Path

from config import Settings
from consumer import TranscriptConsumer
from parser.models import ParsedTranscript

_FIXTURE = Path(__file__).parent / "fixtures" / "sample_transcript.pdf"


class _FakeProducer:
    """Captures published transcripts instead of sending them to Kafka."""

    def __init__(self) -> None:
        self.published: list[ParsedTranscript] = []

    def publish(self, transcript: ParsedTranscript) -> None:
        self.published.append(transcript)

    def flush(self, timeout: float = 10.0) -> None:  # noqa: D401 - test stub
        return None


def _build_settings() -> Settings:
    return Settings(pii_hash_salt="pipeline-test-salt", enable_consumer=False)


def test_handle_message_parses_and_publishes() -> None:
    producer = _FakeProducer()
    consumer = TranscriptConsumer(_build_settings(), producer)

    message = json.dumps(
        {
            "job_id": "job-42",
            "teacher_id": "teacher-1",
            "department_id": "dept-1",
            "pdf_base64": base64.b64encode(_FIXTURE.read_bytes()).decode("ascii"),
        }
    ).encode("utf-8")

    consumer._handle_message(message)

    assert len(producer.published) == 1
    transcript = producer.published[0]
    assert transcript.job_id == "job-42"
    assert transcript.department_id == "dept-1"
    assert transcript.metadata.program_code == "356"
    assert len(transcript.courses) == 61
    assert "29837459164" not in transcript.model_dump_json()


def test_handle_message_swallows_invalid_payload() -> None:
    producer = _FakeProducer()
    consumer = TranscriptConsumer(_build_settings(), producer)

    consumer._handle_message(b"not-valid-json")

    assert producer.published == []
