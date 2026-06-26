"""Robustness and invariant tests for the transcript parser.

These go beyond the happy-path field checks: they assert structural invariants on
the parsed sample, validate watermark/bold cleanup, and exercise the internal
helpers directly so future regressions in the PDF heuristics are caught early.
"""

from __future__ import annotations

import pytest

from parser.pdf_parser import (
    _extract_ogrenci_no,
    _extract_text_with_pdfplumber,
    _extract_text_with_pypdf,
    _is_visible_text,
    _parse_courses,
    _parse_metadata,
    _split_name_and_code,
    _to_float,
    _to_int,
    parse_transcript,
)

_KNOWN_TOTAL_CREDIT = 143.0


# --- Invariants on the parsed sample transcript --------------------------------


def test_course_ects_sum_matches_reported_total(
    sample_pdf_bytes: bytes, sample_expected: dict
) -> None:
    """Summing every parsed course's ECTS must equal the transcript's total."""
    transcript = parse_transcript(sample_pdf_bytes).transcript
    summed = sum(course.ects for course in transcript.courses)
    assert summed == pytest.approx(transcript.metadata.total_ects)
    assert summed == pytest.approx(sample_expected["total_ects"])


def test_course_credit_sum_is_stable(sample_pdf_bytes: bytes) -> None:
    transcript = parse_transcript(sample_pdf_bytes).transcript
    summed = sum(course.credit for course in transcript.courses)
    assert summed == pytest.approx(_KNOWN_TOTAL_CREDIT)


def test_every_course_has_valid_fields(sample_pdf_bytes: bytes) -> None:
    transcript = parse_transcript(sample_pdf_bytes).transcript
    for course in transcript.courses:
        assert course.course_code.strip()
        assert course.course_name.strip()
        assert course.credit > 0
        assert course.ects > 0
        assert course.grade.strip()
        assert course.academic_year


def test_no_duplicate_course_codes(sample_pdf_bytes: bytes) -> None:
    transcript = parse_transcript(sample_pdf_bytes).transcript
    codes = [course.course_code for course in transcript.courses]
    assert len(codes) == len(set(codes))


def test_every_semester_has_courses(
    sample_pdf_bytes: bytes, sample_expected: dict
) -> None:
    transcript = parse_transcript(sample_pdf_bytes).transcript
    assert len(transcript.semesters) == sample_expected["semester_count"]
    for semester in transcript.semesters:
        assert semester.courses, f"empty semester: {semester.name}"


def test_both_columns_are_parsed(sample_pdf_bytes: bytes) -> None:
    """Courses from the left and the right column of the same row are captured."""
    transcript = parse_transcript(sample_pdf_bytes).transcript
    codes = {course.course_code for course in transcript.courses}
    # BBM101 (left) and BBM103 (right) share the first physical row.
    assert {"BBM101", "BBM103"}.issubset(codes)


def test_watermark_and_bold_artifacts_are_removed(sample_pdf_bytes: bytes) -> None:
    """Names corrupted by the watermark in raw extraction must come out clean."""
    transcript = parse_transcript(sample_pdf_bytes).transcript
    names = {course.course_name for course in transcript.courses}
    # In raw extraction these read "VERİ YAvPILARI", "MiANTIKSAL TASARIM", etc.
    assert "VERİ YAPILARI" in names
    assert "MANTIKSAL TASARIM" in names
    assert "PROBABILITY" in names
    # Bold-doubled TC ("2299883377...") must be de-duplicated back to 11 digits.
    assert parse_transcript(sample_pdf_bytes).identity.tc_kimlik_no == "29837459164"


def test_extracted_text_is_non_empty(sample_pdf_bytes: bytes) -> None:
    text = _extract_text_with_pdfplumber(sample_pdf_bytes)
    assert "1. Sınıf Güz" in text
    assert "BBM101" in text


# --- Internal helper unit tests ------------------------------------------------


def test_is_visible_text_filters_faint_watermark() -> None:
    assert _is_visible_text({"non_stroking_color": (0, 0, 0, 1)}) is True
    assert _is_visible_text({"non_stroking_color": (0, 0, 0, 0.120605)}) is False
    assert _is_visible_text({"non_stroking_color": (0, 0, 0)}) is True
    assert _is_visible_text({"non_stroking_color": None}) is True


@pytest.mark.parametrize(
    ("raw", "expected"),
    [("2,72", 2.72), ("244", 244.0), ("3.5", 3.5), (None, None), ("abc", None)],
)
def test_to_float(raw, expected) -> None:
    assert _to_float(raw) == expected


@pytest.mark.parametrize(
    ("raw", "expected"),
    [("4", 4), ("0", 0), (None, None), ("x", None)],
)
def test_to_int(raw, expected) -> None:
    assert _to_int(raw) == expected


@pytest.mark.parametrize(
    ("raw", "name", "code"),
    [
        ("BİLGİSAYAR MÜHENDİSLİĞİ (356)", "BİLGİSAYAR MÜHENDİSLİĞİ", "356"),
        ("MÜHENDİSLİK FAKÜLTESİ ((310)", "MÜHENDİSLİK FAKÜLTESİ", "310"),
        ("NO CODE HERE", "NO CODE HERE", None),
        (None, None, None),
    ],
)
def test_split_name_and_code(raw, name, code) -> None:
    assert _split_name_and_code(raw) == (name, code)


# --- Synthetic parsing tests (format documentation + future-proofing) ----------

_SYNTHETIC_TWO_COLUMN = (
    "2. Sınıf Güz\n"
    "Ders Kodu Ders Adı Başarı Yılı Krd AKTS Not "
    "Ders Kodu Ders Adı Başarı Yılı Krd AKTS Not\n"
    "AİT203 ATATÜRK İLKELERİ VE İNKILAP TARİHİ I 17-18 2 2 B1 "
    "BBM201 VERİ YAPILARI 20-21 3 5 C1\n"
    "İST299 PROBABILITY 20-21 3 5 F1\n"
)


def test_parse_courses_handles_two_columns_and_grades() -> None:
    semesters = _parse_courses(_SYNTHETIC_TWO_COLUMN)
    assert len(semesters) == 1
    semester = semesters[0]
    assert semester.name == "2. Sınıf Güz"
    assert len(semester.courses) == 3

    by_code = {c.course_code: c for c in semester.courses}
    assert by_code["AİT203"].course_name == "ATATÜRK İLKELERİ VE İNKILAP TARİHİ I"
    assert by_code["BBM201"].course_name == "VERİ YAPILARI"
    assert by_code["BBM201"].is_passed is True
    # A failing grade in a synthetic row must flip is_passed.
    assert by_code["İST299"].is_passed is False


def test_parse_metadata_two_column_layout() -> None:
    text = (
        "T.C.Kimlik No : 11111111111 Öğrenci No : 21627208 Kayıt Nedeni : ÖSS\n"
        "Adı Soyadı : JOHN DOE Kayıt Tarihi : 01.01.2020\n"
        "Fakülte/Yüksekokul : SOME FACULTY (99) Mezuniyet Tarihi : 02.02.2024\n"
        "Program : SOME PROGRAM (123) Mezun Olduğu Dönem : 2023-2024 Güz\n"
        "Öğrenim Süresi : 4 Mezuniyet Ortalaması : 3,10\n"
        "Program Türü : Lisans\n"
    )
    metadata, identity = _parse_metadata(text)
    assert identity.full_name == "JOHN DOE"
    assert identity.tc_kimlik_no == "11111111111"
    assert identity.ogrenci_no == "21627208"
    assert metadata.program_name == "SOME PROGRAM"
    assert metadata.program_code == "123"
    assert metadata.faculty == "SOME FACULTY"
    assert metadata.faculty_code == "99"
    assert metadata.study_duration_years == 4
    assert metadata.graduation_gpa == pytest.approx(3.10)
    assert metadata.graduation_term == "2023-2024 Güz"


def test_parse_metadata_single_column_fallback() -> None:
    text = "Adı Soyadı : JANE ROE\nProgram : ANOTHER PROGRAM (777)\n"
    metadata, identity = _parse_metadata(text)
    assert identity.full_name == "JANE ROE"
    assert metadata.program_name == "ANOTHER PROGRAM"
    assert metadata.program_code == "777"


@pytest.mark.parametrize(
    "text",
    [
        "Ogrenci No : 21627208 Kayıt Nedeni : ÖSS",
        "Öğrenci No :\n21627208",
        "Öğrenci No\n: 21627208",
        "Öğrenci No : N24114501 Kayıt Nedeni : YüksekLisans",
    ],
)
def test_extract_ogrenci_no_handles_alternate_header_layouts(text: str) -> None:
    expected = "N24114501" if "N24114501" in text else "21627208"
    assert _extract_ogrenci_no(text) == expected


def test_extract_ogrenci_no_from_columnar_pypdf_layout(sample_pdf_bytes: bytes) -> None:
    text = _extract_text_with_pypdf(sample_pdf_bytes)
    assert _extract_ogrenci_no(text) == "21627208"


def test_parse_transcript_falls_back_to_pypdf_for_student_number(
    sample_pdf_bytes: bytes, monkeypatch: pytest.MonkeyPatch
) -> None:
    """When pdfplumber omits Öğrenci No, pypdf columnar layout still supplies it."""

    def _pdfplumber_without_ogrenci(_pdf_bytes: bytes) -> str:
        return (
            "Program : BİLGİSAYAR MÜHENDİSLİĞİ (356) Mezun Olduğu Dönem : 2023-2024 Bahar\n"
            "1. Sınıf Güz\n"
            "BBM101 PROGRAMLAMAYA GİRİŞ I 16-17 3 6 D\n"
        )

    monkeypatch.setattr(
        "parser.pdf_parser._extract_text_with_pdfplumber",
        _pdfplumber_without_ogrenci,
    )
    result = parse_transcript(sample_pdf_bytes, job_id="job-fallback")
    assert result.transcript.student_number == "21627208"
