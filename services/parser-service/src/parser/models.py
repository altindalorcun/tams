"""Pydantic models describing a parsed transcript.

Two layers exist on purpose:

* :class:`ParsedTranscript` is the **PII-free** payload published to the
  ``transcript.parsed`` Kafka topic. It carries only the masked ``student_ref``.
* :class:`FullTranscript` is the **in-memory only** result returned by the
  parser. It additionally embeds :class:`StudentIdentity`, which holds raw PII
  (full name, TC Kimlik No, Öğrenci No). This object must never be serialized to
  Kafka, disk, or logs.
"""

from __future__ import annotations

from pydantic import BaseModel, Field


class Course(BaseModel):
    """A single course entry taken from a transcript semester block."""

    course_code: str = Field(..., description="e.g. BBM101")
    course_name: str
    credit: float = Field(..., description="National credit (Krd)")
    ects: float = Field(..., description="ECTS / AKTS")
    grade: str = Field(..., description="Raw letter grade, e.g. A1, B2, D")
    academic_year: str = Field(..., description="Başarı Yılı, e.g. 16-17")
    is_passed: bool = Field(..., description="Derived from the grade")


class Semester(BaseModel):
    """A semester block grouping the courses taken within it."""

    name: str = Field(..., description="e.g. '1. Sınıf Güz'")
    courses: list[Course] = Field(default_factory=list)


class TranscriptMetadata(BaseModel):
    """PII-free academic metadata extracted from the transcript header."""

    program_name: str | None = None
    program_code: str | None = None
    faculty: str | None = None
    faculty_code: str | None = None
    study_duration_years: int | None = Field(
        default=None, description="Öğrenim Süresi"
    )
    program_type: str | None = Field(default=None, description="e.g. Lisans")
    graduation_gpa: float | None = Field(
        default=None, description="Mezuniyet Ortalaması"
    )
    graduation_term: str | None = Field(
        default=None, description="Mezun Olduğu Dönem"
    )
    registration_date: str | None = Field(default=None, description="Kayıt Tarihi")
    graduation_date: str | None = Field(
        default=None, description="Mezuniyet Tarihi"
    )
    total_ects: float | None = Field(
        default=None, description="Genel AKTS Toplamı"
    )


class ParsedTranscript(BaseModel):
    """PII-free transcript payload published to ``transcript.parsed``."""

    student_ref: str = Field(..., description="Deterministic masked student id")
    job_id: str | None = None
    teacher_id: str | None = None
    department_id: str | None = None
    metadata: TranscriptMetadata = Field(default_factory=TranscriptMetadata)
    semesters: list[Semester] = Field(default_factory=list)

    @property
    def courses(self) -> list[Course]:
        """Flattened list of every course across all semesters."""
        return [course for semester in self.semesters for course in semester.courses]


class RawTranscriptMessage(BaseModel):
    """Inbound ``transcript.raw`` message produced by analysis-service.

    The PDF is carried as a Base64-encoded string so it survives JSON transport.
    """

    job_id: str
    teacher_id: str | None = None
    department_id: str | None = None
    pdf_base64: str


class StudentIdentity(BaseModel):
    """Raw PII. Lives only inside :class:`FullTranscript`; never published."""

    full_name: str | None = None
    tc_kimlik_no: str | None = None
    ogrenci_no: str | None = None


class FullTranscript(BaseModel):
    """In-memory parse result combining raw identity with the publishable part."""

    identity: StudentIdentity
    transcript: ParsedTranscript
