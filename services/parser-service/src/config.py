"""Application configuration.

All values are sourced from environment variables (or an optional local ``.env``
file) so that no secret is ever hardcoded. Settings are resolved lazily via
:func:`get_settings` to avoid import-time failures in test environments that do
not provide Kafka broker settings.
"""

from __future__ import annotations

from functools import lru_cache

from pydantic import AliasChoices, Field
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    """Runtime configuration for the parser-service."""

    app_name: str = "parser-service"
    host: str = "0.0.0.0"
    port: int = 8000
    log_level: str = "INFO"

    # Kafka
    kafka_bootstrap_servers: str = "localhost:9092"
    kafka_consumer_group: str = "parser-service"
    kafka_auto_offset_reset: str = "earliest"
    topic_raw: str = Field(
        default="transcript.raw",
        validation_alias=AliasChoices("KAFKA_TOPIC_RAW", "TOPIC_RAW"),
    )
    topic_parsed: str = Field(
        default="transcript.parsed",
        validation_alias=AliasChoices("KAFKA_TOPIC_PARSED", "TOPIC_PARSED"),
    )

    # When False, the FastAPI app starts without launching the Kafka consumer
    # loop. Useful for tests and for running the service purely as an HTTP probe.
    enable_consumer: bool = True

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore",
    )


@lru_cache
def get_settings() -> Settings:
    """Return a cached :class:`Settings` instance built from the environment."""
    return Settings()
