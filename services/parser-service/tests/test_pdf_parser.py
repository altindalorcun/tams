"""Tests for the PDF transcript parser using the sample Hacettepe transcript."""

from pathlib import Path

import pytest

from parser.pdf_parser import parse_transcript
from pii.pii_masker import build_student_ref, contains_raw_pii

_SALT = "unit-test-salt"
_FIXTURE = Path(__file__).parent / "fixtures" / "sample_transcript.pdf"
_EXPECTED_COURSE_COUNT = 61
_EXPECTED_SEMESTER_COUNT = 9


@pytest.fixture(scope="module")
def pdf_bytes() -> bytes:
    return _FIXTURE.read_bytes()


def test_metadata_is_extracted(pdf_bytes: bytes) -> None:
    meta = parse_transcript(pdf_bytes, salt=_SALT).transcript.metadata
    assert meta.program_name == "BİLGİSAYAR MÜHENDİSLİĞİ"
    assert meta.program_code == "356"
    assert meta.faculty == "MÜHENDİSLİK FAKÜLTESİ"
    assert meta.faculty_code == "310"
    assert meta.study_duration_years == 4
    assert meta.program_type == "Lisans"
    assert meta.graduation_gpa == pytest.approx(2.72)
    assert meta.graduation_term == "2023-2024 Bahar"
    assert meta.registration_date == "15.08.2016"
    assert meta.graduation_date == "13.06.2024"
    assert meta.total_ects == pytest.approx(244.0)


def test_identity_is_extracted(pdf_bytes: bytes) -> None:
    identity = parse_transcript(pdf_bytes, salt=_SALT).identity
    assert identity.full_name == "VOLKAN ERCİYAS"
    assert identity.tc_kimlik_no == "29837459164"
    assert identity.ogrenci_no == "21627208"


def test_courses_and_semesters(pdf_bytes: bytes) -> None:
    transcript = parse_transcript(pdf_bytes, salt=_SALT).transcript
    assert len(transcript.semesters) == _EXPECTED_SEMESTER_COUNT
    assert len(transcript.courses) == _EXPECTED_COURSE_COUNT

    first = transcript.semesters[0]
    assert first.name == "1. Sınıf Güz"
    bbm101 = next(c for c in transcript.courses if c.course_code == "BBM101")
    assert bbm101.course_name == "PROGRAMLAMAYA GİRİŞ I"
    assert bbm101.credit == 3.0
    assert bbm101.ects == 6.0
    assert bbm101.grade == "D"
    assert bbm101.academic_year == "16-17"
    assert bbm101.is_passed is True


def test_student_ref_is_masked(pdf_bytes: bytes) -> None:
    transcript = parse_transcript(pdf_bytes, salt=_SALT).transcript
    expected = build_student_ref("29837459164", "21627208", _SALT)
    assert transcript.student_ref == expected


def test_published_payload_contains_no_raw_pii(pdf_bytes: bytes) -> None:
    transcript = parse_transcript(pdf_bytes, salt=_SALT).transcript
    published = transcript.model_dump_json()
    assert "VOLKAN" not in published
    assert "29837459164" not in published
    assert "21627208" not in published
    assert not contains_raw_pii(published)


def test_all_expected_course_codes_are_parsed(
    pdf_bytes: bytes, sample_expected: dict
) -> None:
    """Every course listed on the sample transcript must be captured exactly once."""
    transcript = parse_transcript(pdf_bytes, salt=_SALT).transcript
    parsed_codes = {course.course_code for course in transcript.courses}
    expected_codes = set(sample_expected["course_codes"])
    assert parsed_codes == expected_codes


def test_semester_names_match_hacettepe_layout(
    pdf_bytes: bytes, sample_expected: dict
) -> None:
    transcript = parse_transcript(pdf_bytes, salt=_SALT).transcript
    assert [semester.name for semester in transcript.semesters] == [
        "1. Sınıf Güz",
        "1. Sınıf Bahar",
        "2. Sınıf Güz",
        "2. Sınıf Bahar",
        "3. Sınıf Güz",
        "3. Sınıf Bahar",
        "4. Sınıf Güz",
        "4. Sınıf Bahar",
        "4. Sınıf Yaz",
    ]
    assert len(transcript.semesters) == sample_expected["semester_count"]
