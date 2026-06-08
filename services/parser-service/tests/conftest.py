"""Shared pytest fixtures and helpers for the parser-service test suite."""

from __future__ import annotations

import shutil
import subprocess
from pathlib import Path

import pytest

FIXTURES_DIR = Path(__file__).parent / "fixtures"
SAMPLE_TRANSCRIPT = FIXTURES_DIR / "sample_transcript.pdf"

# Known-good values for the bundled sample transcript (VOLKAN ERCİYAS).
SAMPLE_EXPECTED = {
    "full_name": "VOLKAN ERCİYAS",
    "tc_kimlik_no": "29837459164",
    "ogrenci_no": "21627208",
    "program_name": "BİLGİSAYAR MÜHENDİSLİĞİ",
    "program_code": "356",
    "faculty": "MÜHENDİSLİK FAKÜLTESİ",
    "faculty_code": "310",
    "study_duration_years": 4,
    "program_type": "Lisans",
    "graduation_gpa": 2.72,
    "graduation_term": "2023-2024 Bahar",
    "registration_date": "15.08.2016",
    "graduation_date": "13.06.2024",
    "total_ects": 244.0,
    "semester_count": 9,
    "course_count": 61,
    "course_codes": (
        "BBM101", "BBM103", "BBM384", "BBM432", "BBM434", "BBM456", "BBM458",
        "BBM459", "BBM460", "BBM461", "BBM463", "BBM465", "BBM475", "BEB650",
        "FİZ103", "FİZ137", "İNG111", "MAT123", "SEC189", "TKD103", "ÜNİ101",
        "BBM102", "BBM104", "FİZ104", "FİZ138", "İNG112", "MAT124", "TKD104",
        "AİT203", "BBM201", "BBM203", "BBM205", "BBM231", "BBM233", "HAS222",
        "İST299", "AİT204", "BBM202", "BBM204", "BBM234", "ELE296", "İST292",
        "MAT254", "MÜH104", "BBM301", "BBM325", "BBM341", "BBM371", "BBM342",
        "BBM382", "BBM425", "BBM427", "BBM479", "İNG127", "KMÖ175", "SEC167",
        "BBM428", "BBM480", "BBM498", "SEC179", "TAR106",
    ),
}


@pytest.fixture(scope="session")
def sample_pdf_bytes() -> bytes:
    """Raw bytes of the bundled sample Hacettepe transcript."""
    return SAMPLE_TRANSCRIPT.read_bytes()


@pytest.fixture(scope="session")
def sample_expected() -> dict:
    """Known-good parsed values for the bundled sample transcript."""
    return dict(SAMPLE_EXPECTED)


def docker_available() -> bool:
    """Return ``True`` if a Docker daemon is reachable for integration tests."""
    if shutil.which("docker") is None:
        return False
    try:
        result = subprocess.run(
            ["docker", "info"],
            stdout=subprocess.DEVNULL,
            stderr=subprocess.DEVNULL,
            timeout=10,
        )
    except (OSError, subprocess.SubprocessError):
        return False
    return result.returncode == 0
