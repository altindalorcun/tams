"""Kafka consumer that drives the transcript parsing pipeline.

Subscribes to ``transcript.raw``, decodes each message, runs the parser, and
hands the resulting PII-free transcript to the producer for publication to
``transcript.parsed``.
"""

from __future__ import annotations

import base64
import json
import logging
import threading

from confluent_kafka import Consumer, KafkaError

from config import Settings
from parser.models import RawTranscriptMessage
from parser.pdf_parser import parse_transcript
from producer import TranscriptProducer

logger = logging.getLogger(__name__)

# How long (seconds) a single poll waits for a new message before looping.
_POLL_TIMEOUT_SECONDS = 1.0


class TranscriptConsumer:
    """Long-running consumer loop for the ``transcript.raw`` topic."""

    def __init__(self, settings: Settings, producer: TranscriptProducer) -> None:
        self._settings = settings
        self._producer = producer
        self._consumer = Consumer(
            {
                "bootstrap.servers": settings.kafka_bootstrap_servers,
                "group.id": settings.kafka_consumer_group,
                "auto.offset.reset": settings.kafka_auto_offset_reset,
                "enable.auto.commit": True,
            }
        )
        self._stop_event = threading.Event()

    def start(self) -> None:
        """Subscribe and block, processing messages until :meth:`stop` is called."""
        self._consumer.subscribe([self._settings.topic_raw])
        logger.info("Subscribed to topic %s", self._settings.topic_raw)
        try:
            while not self._stop_event.is_set():
                message = self._consumer.poll(_POLL_TIMEOUT_SECONDS)
                if message is None:
                    continue
                if message.error():
                    if message.error().code() == KafkaError._PARTITION_EOF:
                        continue
                    logger.error("Consumer error: %s", message.error())
                    continue
                self._handle_message(message.value())
        finally:
            self._consumer.close()
            self._producer.flush()

    def stop(self) -> None:
        """Signal the consumer loop to terminate."""
        self._stop_event.set()

    def _handle_message(self, raw_value: bytes) -> None:
        """Parse a single raw message and publish the PII-free result."""
        try:
            payload = json.loads(raw_value.decode("utf-8"))
            message = RawTranscriptMessage.model_validate(payload)
            pdf_bytes = base64.b64decode(message.pdf_base64)
            result = parse_transcript(
                pdf_bytes,
                job_id=message.job_id,
                teacher_id=message.teacher_id,
                department_id=message.department_id,
            )
            self._producer.publish(result.transcript)
            logger.info(
                "Processed transcript for job %s: student_number_present=%s",
                message.job_id,
                bool(result.transcript.student_number),
            )
        except Exception:  # noqa: BLE001 - isolate one bad message from the loop
            # The exception is logged without the payload to avoid leaking PII.
            logger.exception("Failed to process a transcript.raw message")
