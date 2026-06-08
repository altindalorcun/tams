"""PDF transcript parser for Hacettepe University graduation transcripts.

The parser operates entirely in memory: it accepts the PDF as ``bytes`` and never
touches the file system. ``pdfplumber`` is the primary extraction engine, with
``pypdf`` kept as a fallback for documents ``pdfplumber`` cannot read.

Two format quirks of the official Hacettepe transcript are handled explicitly:

1. A faint diagonal "Hacettepe Üniversitesi" watermark whose glyphs interleave
   with real text. The watermark uses a low-ink CMYK colour, so it is filtered
   out by ink level before text reconstruction.
2. Header values are rendered in fake-bold by drawing each glyph twice at the
   same coordinates. Duplicate glyphs are removed by position before extraction.
"""

from __future__ import annotations

import io
import re

import pdfplumber
from pdfplumber.utils import extract_text as extract_text_from_chars
from pypdf import PdfReader

from parser.grades import is_passing
from parser.models import (
    Course,
    FullTranscript,
    ParsedTranscript,
    Semester,
    StudentIdentity,
    TranscriptMetadata,
)
from pii.pii_masker import build_student_ref

# --- Extraction tuning constants ------------------------------------------------

# CMYK K-channel level below which a glyph is treated as a faint watermark.
_MIN_INK_LEVEL = 0.5
# Decimal places used when comparing glyph coordinates during de-duplication.
_POSITION_ROUNDING = 1
# Horizontal tolerance (points) for word separation during text reconstruction.
_X_TOLERANCE = 1.5

# --- Metadata patterns ----------------------------------------------------------

_RE_TC = re.compile(r"Kimlik No\s*:+\s*(\d{11})")
_RE_OGRENCI = re.compile(r"Öğrenci No\s*:+\s*(\d+)")
_RE_DURATION = re.compile(r"Öğrenim Süresi\s*:+\s*(\d+)")
_RE_PROGRAM_TYPE = re.compile(r"Program Türü\s*:+\s*([^\n]+)")
_RE_GPA = re.compile(r"Mezuniyet Ortalaması\s*:+\s*([\d.,]+)")
_RE_REG_DATE = re.compile(r"Kayıt Tarihi\s*:+\s*(\d{2}\.\d{2}\.\d{4})")
_RE_GRAD_DATE = re.compile(r"Mezuniyet Tarihi\s*:+\s*(\d{2}\.\d{2}\.\d{4})")
_RE_TOTAL_ECTS = re.compile(r"Genel AKTS Toplamı\s*:+\s*(\d+)")
_RE_CODE_IN_PARENS = re.compile(r"\((\d+)\)")

# Values that may sit in the left column next to a right-column label. The
# primary pattern is anchored on the following right-column label; the fallback
# captures to end of line for single-column layouts.
_RE_NAME = re.compile(r"Adı Soyadı\s*:+\s*(.+?)\s+Kayıt Tarihi")
_RE_NAME_FALLBACK = re.compile(r"Adı Soyadı\s*:+\s*([^\n]+)")
_RE_FACULTY = re.compile(r"Fakülte/Yüksekokul\s*:+\s*(.+?)\s+Mezuniyet Tarihi")
_RE_FACULTY_FALLBACK = re.compile(r"Fakülte/Yüksekokul\s*:+\s*([^\n]+)")
_RE_PROGRAM = re.compile(r"Program\s*:+\s*(.+?)\s+Mezun Olduğu Dönem")
_RE_PROGRAM_FALLBACK = re.compile(r"Program\s*:+\s*([^\n]+)")
_RE_GRAD_TERM = re.compile(r"Mezun Olduğu Dönem\s*:+\s*([^\n]+)")

# --- Course / semester patterns -------------------------------------------------

_RE_SEMESTER = re.compile(r"^\s*(\d+\.\s*Sınıf\s+(?:Güz|Bahar|Yaz))\s*$")
_RE_COURSE = re.compile(
    r"([A-ZÇĞİÖŞÜ]{2,4}\d{3})\s+"  # course code, e.g. BBM101
    r"(.+?)\s+"  # course name (non-greedy, bounded by year)
    r"(\d{2}-\d{2})\s+"  # academic year (Başarı Yılı)
    r"(\d+)\s+"  # credit (Krd)
    r"(\d+)\s+"  # ECTS (AKTS)
    r"([A-ZÇĞİÖŞÜ]{1,2}\d?)"  # letter grade (Not)
)


def _is_visible_text(obj: dict) -> bool:
    """Return ``True`` for real text, ``False`` for the faint watermark glyphs."""
    color = obj.get("non_stroking_color")
    if isinstance(color, (tuple, list)) and len(color) == 4:
        return color[3] >= _MIN_INK_LEVEL
    return True


def _clean_page_text(page: pdfplumber.page.Page) -> str:
    """Return reconstructed text for a page with watermark and bold artifacts removed."""
    seen: set[tuple[float, float, str]] = set()
    deduped: list[dict] = []
    for char in page.chars:
        if not _is_visible_text(char):
            continue
        key = (
            round(char["x0"], _POSITION_ROUNDING),
            round(char["top"], _POSITION_ROUNDING),
            char["text"],
        )
        if key in seen:
            continue
        seen.add(key)
        deduped.append(char)
    if not deduped:
        return ""
    return extract_text_from_chars(deduped, x_tolerance=_X_TOLERANCE) or ""


def _extract_text_with_pdfplumber(pdf_bytes: bytes) -> str:
    """Extract cleaned text from all pages using pdfplumber."""
    pages_text: list[str] = []
    with pdfplumber.open(io.BytesIO(pdf_bytes)) as pdf:
        for page in pdf.pages:
            pages_text.append(_clean_page_text(page))
    return "\n".join(pages_text).strip()


def _extract_text_with_pypdf(pdf_bytes: bytes) -> str:
    """Fallback text extraction using pypdf (no watermark filtering)."""
    reader = PdfReader(io.BytesIO(pdf_bytes))
    return "\n".join((page.extract_text() or "") for page in reader.pages).strip()


def _first_match(text: str, *patterns: re.Pattern[str]) -> str | None:
    """Return the first captured group across ``patterns``, stripped, or ``None``."""
    for pattern in patterns:
        match = pattern.search(text)
        if match:
            value = match.group(1).strip()
            if value:
                return value
    return None


def _to_float(value: str | None) -> float | None:
    """Parse a Turkish-formatted decimal (comma separator) into a float."""
    if value is None:
        return None
    try:
        return float(value.replace(",", "."))
    except ValueError:
        return None


def _to_int(value: str | None) -> int | None:
    """Parse an integer string, returning ``None`` on failure."""
    if value is None:
        return None
    try:
        return int(value)
    except ValueError:
        return None


def _split_name_and_code(value: str | None) -> tuple[str | None, str | None]:
    """Split a 'Name (123)' value into a clean name and its numeric code."""
    if not value:
        return None, None
    code_match = _RE_CODE_IN_PARENS.search(value)
    code = code_match.group(1) if code_match else None
    name = re.split(r"\s*\(+", value, maxsplit=1)[0].strip()
    return (name or None), code


def _parse_metadata(text: str) -> tuple[TranscriptMetadata, StudentIdentity]:
    """Extract academic metadata and raw student identity from the header text."""
    program_name, program_code = _split_name_and_code(
        _first_match(text, _RE_PROGRAM, _RE_PROGRAM_FALLBACK)
    )
    faculty_name, faculty_code = _split_name_and_code(
        _first_match(text, _RE_FACULTY, _RE_FACULTY_FALLBACK)
    )

    metadata = TranscriptMetadata(
        program_name=program_name,
        program_code=program_code,
        faculty=faculty_name,
        faculty_code=faculty_code,
        study_duration_years=_to_int(_first_match(text, _RE_DURATION)),
        program_type=_first_match(text, _RE_PROGRAM_TYPE),
        graduation_gpa=_to_float(_first_match(text, _RE_GPA)),
        graduation_term=_first_match(text, _RE_GRAD_TERM),
        registration_date=_first_match(text, _RE_REG_DATE),
        graduation_date=_first_match(text, _RE_GRAD_DATE),
        total_ects=_to_float(_first_match(text, _RE_TOTAL_ECTS)),
    )

    identity = StudentIdentity(
        full_name=_first_match(text, _RE_NAME, _RE_NAME_FALLBACK),
        tc_kimlik_no=_first_match(text, _RE_TC),
        ogrenci_no=_first_match(text, _RE_OGRENCI),
    )
    return metadata, identity


def _parse_courses(text: str) -> list[Semester]:
    """Group courses by their semester header (e.g. '1. Sınıf Güz')."""
    semesters: list[Semester] = []
    current: Semester | None = None
    for line in text.splitlines():
        header = _RE_SEMESTER.match(line)
        if header:
            current = Semester(name=re.sub(r"\s+", " ", header.group(1)).strip())
            semesters.append(current)
            continue
        if current is None:
            continue
        for match in _RE_COURSE.finditer(line):
            code, name, year, credit, ects, grade = match.groups()
            current.courses.append(
                Course(
                    course_code=code,
                    course_name=name.strip(),
                    credit=float(credit),
                    ects=float(ects),
                    grade=grade,
                    academic_year=year,
                    is_passed=is_passing(grade),
                )
            )
    return semesters


def parse_transcript(
    pdf_bytes: bytes,
    salt: str,
    job_id: str | None = None,
    teacher_id: str | None = None,
    department_id: str | None = None,
) -> FullTranscript:
    """Parse transcript ``pdf_bytes`` into a :class:`FullTranscript`.

    PII (TC Kimlik No, Öğrenci No) is masked into ``student_ref`` via the PII
    masker before the publishable :class:`ParsedTranscript` is built. The raw
    name is retained only inside the in-memory :class:`StudentIdentity`.
    """
    text = _extract_text_with_pdfplumber(pdf_bytes)
    if not text:
        text = _extract_text_with_pypdf(pdf_bytes)

    metadata, identity = _parse_metadata(text)
    semesters = _parse_courses(text)

    student_ref = build_student_ref(
        identity.tc_kimlik_no, identity.ogrenci_no, salt
    )

    parsed = ParsedTranscript(
        student_ref=student_ref,
        job_id=job_id,
        teacher_id=teacher_id,
        department_id=department_id,
        metadata=metadata,
        semesters=semesters,
    )
    return FullTranscript(identity=identity, transcript=parsed)
