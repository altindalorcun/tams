"""Kafka producer that publishes PII-free parsed transcripts.

Publishes :class:`ParsedTranscript` payloads as JSON to the ``transcript.parsed``
topic. Only the PII-free transcript is ever serialized here.
"""

from __future__ import annotations

import logging

from confluent_kafka import Producer

from config import Settings
from parser.models import ParsedTranscript

logger = logging.getLogger(__name__)


class TranscriptProducer:
    """Thin wrapper around a confluent-kafka :class:`Producer`."""

    def __init__(self, settings: Settings) -> None:
        self._topic = settings.topic_parsed
        self._producer = Producer(
            {"bootstrap.servers": settings.kafka_bootstrap_servers}
        )

    def publish(self, transcript: ParsedTranscript) -> None:
        """Serialize and publish a parsed transcript keyed by student number or job id."""
        payload = transcript.model_dump_json().encode("utf-8")
        message_key = transcript.student_number or transcript.job_id or ""
        self._producer.produce(
            topic=self._topic,
            key=message_key.encode("utf-8"),
            value=payload,
            on_delivery=self._on_delivery,
        )
        self._producer.poll(0)
        # Block until the broker acknowledges the message so downstream consumers
        # can read it immediately after the parse step returns.
        self._producer.flush(timeout=10.0)

    def flush(self, timeout: float = 10.0) -> None:
        """Block until all queued messages are delivered or ``timeout`` elapses."""
        self._producer.flush(timeout)

    @staticmethod
    def _on_delivery(error, message) -> None:
        """Delivery callback. Never logs message payloads (they are PII-free)."""
        if error is not None:
            logger.error("Failed to deliver parsed transcript: %s", error)
        else:
            logger.info(
                "Published parsed transcript to %s [partition %s]",
                message.topic(),
                message.partition(),
            )
