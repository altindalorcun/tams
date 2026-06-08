"""Tests for the PII masker.

These tests guard the core privacy guarantee: raw identifiers must never survive
into the masked output, and masking must be deterministic but salt-dependent.
"""

import logging

import pytest

from pii.pii_masker import build_student_ref, contains_raw_pii, mask_value

_SALT = "unit-test-salt"
_TC = "29837459164"
_OGRENCI = "21627208"
_MASK_HEX_LENGTH = 16


def test_mask_value_is_deterministic() -> None:
    assert mask_value(_TC, _SALT) == mask_value(_TC, _SALT)


def test_mask_value_length_and_hex() -> None:
    masked = mask_value(_TC, _SALT)
    assert len(masked) == _MASK_HEX_LENGTH
    int(masked, 16)  # raises if not valid hex


def test_mask_value_depends_on_salt() -> None:
    assert mask_value(_TC, _SALT) != mask_value(_TC, "another-salt")


def test_mask_value_requires_salt() -> None:
    with pytest.raises(ValueError):
        mask_value(_TC, "")


def test_masked_output_contains_no_raw_pii() -> None:
    ref = build_student_ref(_TC, _OGRENCI, _SALT)
    assert _TC not in ref
    assert _OGRENCI not in ref
    assert not contains_raw_pii(ref)


def test_build_student_ref_requires_an_identifier() -> None:
    with pytest.raises(ValueError):
        build_student_ref(None, None, _SALT)


def test_masker_never_logs_raw_pii(caplog: pytest.LogCaptureFixture) -> None:
    with caplog.at_level(logging.DEBUG):
        build_student_ref(_TC, _OGRENCI, _SALT)
    assert _TC not in caplog.text
    assert _OGRENCI not in caplog.text
