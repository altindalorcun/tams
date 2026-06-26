"""Tests for the PII safety checks on publishable transcript payloads."""

from pii.pii_masker import contains_raw_pii

_TC = "29837459164"
_OGRENCI = "21627208"


def test_contains_raw_pii_detects_tc_kimlik_no() -> None:
    assert contains_raw_pii(f"student tc is {_TC}")


def test_contains_raw_pii_ignores_short_student_numbers() -> None:
    assert not contains_raw_pii(f"ogrenci no {_OGRENCI}")


def test_contains_raw_pii_empty_text() -> None:
    assert not contains_raw_pii("")
