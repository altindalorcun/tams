"""Grade interpretation rules for Hacettepe University transcripts.

The graduation engine downstream needs a boolean ``is_passed`` flag per course.
Hacettepe uses a letter-grade system (A1, A2, A3, B1, B2, B3, C1, C2, C3, D for
passing tiers and F1, F2, F3 for failures). The lowest passing tier ``D`` is a
conditional pass and still counts toward graduation, which is why it appears on
graduation transcripts.

The rule is intentionally driven by an explicit set of **failing** codes so that
any unrecognised-but-clearly-passing letter grade still resolves to passed. The
sets are module-level constants so the policy can be reviewed and adjusted in one
place if the university regulation changes.
"""

from __future__ import annotations

# Codes that explicitly mean the course was NOT passed.
FAILING_GRADES: frozenset[str] = frozenset(
    {
        "F1",
        "F2",
        "F3",
        "FF",
        "F",
        "K",  # Kaldı (failed)
        "G",  # Girmedi (did not sit the exam)
        "D0",
    }
)

# Status codes that are neither a pass nor a fail (in progress / not graded yet).
NON_GRADED: frozenset[str] = frozenset(
    {
        "E",  # Eksik (incomplete)
        "I",  # In progress
    }
)


def normalize_grade(grade: str | None) -> str:
    """Return an upper-cased, whitespace-stripped grade token."""
    return (grade or "").strip().upper()


def is_passing(grade: str | None) -> bool:
    """Return ``True`` if ``grade`` represents a passed course.

    Unknown but non-empty grades are treated as passing on the assumption that
    the transcript only lists earned courses; explicit failure and non-graded
    codes are excluded.
    """
    normalized = normalize_grade(grade)
    if not normalized:
        return False
    if normalized in FAILING_GRADES or normalized in NON_GRADED:
        return False
    return True
