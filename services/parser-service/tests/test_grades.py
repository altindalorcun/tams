"""Tests for the grade interpretation rules."""

import pytest

from parser.grades import is_passing


@pytest.mark.parametrize(
    "grade",
    ["A1", "A2", "A3", "B1", "B2", "B3", "C1", "C2", "C3", "D", "a1", " b2 "],
)
def test_passing_grades(grade: str) -> None:
    assert is_passing(grade) is True


@pytest.mark.parametrize("grade", ["F1", "F2", "F3", "FF", "F", "K", "G"])
def test_failing_grades(grade: str) -> None:
    assert is_passing(grade) is False


@pytest.mark.parametrize("grade", ["", None, "E", "I"])
def test_non_passing_status_codes(grade) -> None:
    assert is_passing(grade) is False
