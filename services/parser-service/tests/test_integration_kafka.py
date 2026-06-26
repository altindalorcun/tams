"""End-to-end Kafka integration tests for the parser pipeline.

Requires a reachable Docker daemon. Tests spin up an ephemeral Kafka broker via
testcontainers, publish a ``transcript.raw`` message, and assert that a
correctly structured ``transcript.parsed`` message appears on the output topic.
"""

from __future__ import annotations

import base64
import json
import threading
import time
import uuid

import pytest
from confluent_kafka import Consumer, KafkaException, Producer
from confluent_kafka.admin import AdminClient, NewTopic

from config import Settings
from conftest import SAMPLE_EXPECTED, SAMPLE_TRANSCRIPT, docker_available
from consumer import TranscriptConsumer
from parser.models import ParsedTranscript
from pii.pii_masker import contains_raw_pii
from producer import TranscriptProducer

pytestmark = pytest.mark.integration

_TOPIC_RAW = "transcript.raw"
_TOPIC_PARSED = "transcript.parsed"
_CONSUMER_READY_SECONDS = 3.0
_MESSAGE_WAIT_SECONDS = 45.0


def _create_topics(bootstrap_servers: str) -> None:
    """Create the raw and parsed topics on the ephemeral broker."""
    admin = AdminClient({"bootstrap.servers": bootstrap_servers})
    futures = admin.create_topics(
        [
            NewTopic(_TOPIC_RAW, num_partitions=1, replication_factor=1),
            NewTopic(_TOPIC_PARSED, num_partitions=1, replication_factor=1),
        ]
    )
    for _topic, future in futures.items():
        try:
            future.result(timeout=30)
        except KafkaException as exc:
            if "already exists" not in str(exc).lower():
                raise


@pytest.fixture(scope="module")
def kafka_bootstrap_servers() -> str:
    """Start a disposable Kafka broker for the integration test module."""
    if not docker_available():
        pytest.skip("Docker is not available — skipping Kafka integration tests")

    from testcontainers.kafka import KafkaContainer

    with KafkaContainer().with_kraft() as kafka:
        bootstrap = kafka.get_bootstrap_server()
        _create_topics(bootstrap)
        yield bootstrap


def _build_settings(bootstrap_servers: str) -> Settings:
    return Settings(
        kafka_bootstrap_servers=bootstrap_servers,
        kafka_consumer_group=f"parser-integration-{uuid.uuid4()}",
        topic_raw=_TOPIC_RAW,
        topic_parsed=_TOPIC_PARSED,
        enable_consumer=False,
    )


def _publish_raw_message(bootstrap_servers: str, job_id: str) -> None:
    payload = {
        "job_id": job_id,
        "teacher_id": "teacher-integration",
        "department_id": "dept-integration",
        "pdf_base64": base64.b64encode(SAMPLE_TRANSCRIPT.read_bytes()).decode("ascii"),
    }
    producer = Producer({"bootstrap.servers": bootstrap_servers})
    producer.produce(_TOPIC_RAW, value=json.dumps(payload).encode("utf-8"))
    producer.flush(15)


def _poll_parsed_message(bootstrap_servers: str) -> bytes | None:
    consumer = Consumer(
        {
            "bootstrap.servers": bootstrap_servers,
            "group.id": f"parsed-reader-{uuid.uuid4()}",
            "auto.offset.reset": "earliest",
        }
    )
    consumer.subscribe([_TOPIC_PARSED])
    deadline = time.time() + _MESSAGE_WAIT_SECONDS
    raw_value: bytes | None = None
    try:
        while time.time() < deadline:
            message = consumer.poll(1.0)
            if message is None:
                continue
            if message.error():
                continue
            raw_value = message.value()
            break
    finally:
        consumer.close()
    return raw_value


def test_raw_to_parsed_pipeline(kafka_bootstrap_servers: str) -> None:
    """Publish to transcript.raw and receive a valid transcript.parsed message."""
    settings = _build_settings(kafka_bootstrap_servers)
    producer = TranscriptProducer(settings)
    service_consumer = TranscriptConsumer(settings, producer)

    thread = threading.Thread(target=service_consumer.start, daemon=True)
    thread.start()
    time.sleep(_CONSUMER_READY_SECONDS)

    job_id = f"integration-job-{uuid.uuid4()}"
    try:
        _publish_raw_message(kafka_bootstrap_servers, job_id)
        raw_value = _poll_parsed_message(kafka_bootstrap_servers)
    finally:
        service_consumer.stop()
        thread.join(timeout=10)

    assert raw_value is not None, "No message received on transcript.parsed"

    transcript = ParsedTranscript.model_validate_json(raw_value.decode("utf-8"))
    published = transcript.model_dump_json()

    assert transcript.job_id == job_id
    assert transcript.teacher_id == "teacher-integration"
    assert transcript.department_id == "dept-integration"
    assert transcript.student_number == SAMPLE_EXPECTED["ogrenci_no"]

    assert transcript.metadata.program_name == SAMPLE_EXPECTED["program_name"]
    assert transcript.metadata.program_code == SAMPLE_EXPECTED["program_code"]
    assert transcript.metadata.faculty == SAMPLE_EXPECTED["faculty"]
    assert transcript.metadata.graduation_gpa == pytest.approx(
        SAMPLE_EXPECTED["graduation_gpa"]
    )
    assert transcript.metadata.total_ects == pytest.approx(
        SAMPLE_EXPECTED["total_ects"]
    )

    assert len(transcript.semesters) == SAMPLE_EXPECTED["semester_count"]
    assert len(transcript.courses) == SAMPLE_EXPECTED["course_count"]

    assert "VOLKAN" not in published
    assert SAMPLE_EXPECTED["tc_kimlik_no"] not in published
    assert transcript.student_number == SAMPLE_EXPECTED["ogrenci_no"]
    assert not contains_raw_pii(published)


def test_settings_read_kafka_topic_env_aliases(monkeypatch: pytest.MonkeyPatch) -> None:
    """Docker Compose uses KAFKA_TOPIC_* names; settings must accept them."""
    monkeypatch.setenv("KAFKA_TOPIC_RAW", "custom.raw")
    monkeypatch.setenv("KAFKA_TOPIC_PARSED", "custom.parsed")
    settings = Settings()
    assert settings.topic_raw == "custom.raw"
    assert settings.topic_parsed == "custom.parsed"
