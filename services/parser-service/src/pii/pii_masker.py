"""Deterministic PII masking.

This module is the single place in the service that is allowed to handle raw
PII values (TC Kimlik No, Öğrenci No). It converts them into a one-way,
salted SHA-256 digest so that no downstream service or datastore ever sees the
original value.

Hard rule: this module must NEVER log, print, or otherwise emit a raw PII value.
There are deliberately no logging calls here.
"""

from __future__ import annotations

import hashlib
import re

# Length of the truncated hex digest used for masked references.
_MASK_HEX_LENGTH = 16

# 11 consecutive digits — the shape of a TC Kimlik No / Öğrenci No.
_TC_PATTERN = re.compile(r"\b\d{11}\b")


def mask_value(raw_value: str, salt: str) -> str:
    """Return ``sha256(salt + raw_value)`` truncated to 16 hex characters.

    The same ``raw_value`` and ``salt`` always yield the same digest, which lets
    downstream services correlate a student across uploads without ever learning
    the underlying identifier.
    """
    if not salt:
        raise ValueError("A non-empty PII salt is required for masking.")
    digest = hashlib.sha256(f"{salt}{raw_value}".encode("utf-8")).hexdigest()
    return digest[:_MASK_HEX_LENGTH]


def build_student_ref(
    tc_kimlik_no: str | None, ogrenci_no: str | None, salt: str
) -> str:
    """Collapse TC Kimlik No and Öğrenci No into a single masked reference.

    At least one of the two identifiers must be present; they are combined before
    hashing so the resulting reference is stable for a given student.
    """
    tc = (tc_kimlik_no or "").strip()
    ogrenci = (ogrenci_no or "").strip()
    if not tc and not ogrenci:
        raise ValueError("Cannot build a student reference without any identifier.")
    return mask_value(f"{tc}:{ogrenci}", salt)


def contains_raw_pii(text: str) -> bool:
    """Return ``True`` if ``text`` still contains an 11-digit PII-shaped number.

    Useful as a safety assertion in tests to guarantee published payloads are
    free of raw identifiers.
    """
    return bool(_TC_PATTERN.search(text or ""))
