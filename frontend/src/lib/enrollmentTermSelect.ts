import type { EnrollmentTerm } from "@/types";

/** Select value representing an unset enrollment term. */
export const ENROLLMENT_TERM_NONE = "none";

/** Display label for an unset enrollment term in select triggers. */
export const ENROLLMENT_TERM_UNSET_LABEL = "—";

/** User-facing labels for enrollment term enum values. */
export const ENROLLMENT_TERM_LABELS: Record<EnrollmentTerm, string> = {
  GUZ: "Güz",
  BAHAR: "Bahar",
};

/** Base UI Select items for enrollment term pickers (includes unset option). */
export const ENROLLMENT_TERM_SELECT_ITEMS = [
  { value: ENROLLMENT_TERM_NONE, label: ENROLLMENT_TERM_UNSET_LABEL },
  { value: "GUZ", label: ENROLLMENT_TERM_LABELS.GUZ },
  { value: "BAHAR", label: ENROLLMENT_TERM_LABELS.BAHAR },
] as const;
