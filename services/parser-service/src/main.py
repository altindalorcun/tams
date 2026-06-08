"""FastAPI entrypoint for the parser-service.

Exposes a health probe and, when enabled, runs the Kafka consumer loop in a
background thread for the lifetime of the application.
"""

from __future__ import annotations

import logging
import threading
from contextlib import asynccontextmanager

from fastapi import FastAPI

from config import get_settings
from consumer import TranscriptConsumer
from producer import TranscriptProducer

logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Start the Kafka consumer on startup and stop it on shutdown."""
    settings = get_settings()
    logging.basicConfig(level=settings.log_level)

    consumer: TranscriptConsumer | None = None
    thread: threading.Thread | None = None

    if settings.enable_consumer:
        producer = TranscriptProducer(settings)
        consumer = TranscriptConsumer(settings, producer)
        thread = threading.Thread(target=consumer.start, daemon=True)
        thread.start()
        logger.info("Kafka consumer thread started")
    else:
        logger.info("Kafka consumer disabled via configuration")

    try:
        yield
    finally:
        if consumer is not None:
            consumer.stop()
        if thread is not None:
            thread.join(timeout=5.0)


app = FastAPI(title="TAMS parser-service", lifespan=lifespan)


@app.get("/health")
def health() -> dict[str, str]:
    """Liveness/readiness probe used by Kubernetes and Docker."""
    return {"status": "ok"}
