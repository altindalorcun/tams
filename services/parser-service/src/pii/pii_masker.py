"""PII safety checks for publishable transcript payloads.

TC Kimlik No must never appear in Kafka messages or downstream stores.
Öğrenci No is not treated as PII and may be published as plain text.
"""

from __future__ import annotations

import re

# 11 consecutive digits — the shape of a TC Kimlik No.
_TC_PATTERN = re.compile(r"\b\d{11}\b")


def contains_raw_pii(text: str) -> bool:
    """Return ``True`` if ``text`` still contains an 11-digit TC Kimlik No pattern.

    Useful as a safety assertion in tests to guarantee published payloads are
    free of TC identifiers.
    """
    return bool(_TC_PATTERN.search(text or ""))
