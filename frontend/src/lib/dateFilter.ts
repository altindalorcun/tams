/** Comparison mode for filtering records by calendar day. */
export type DateComparisonMode = "ON" | "AFTER" | "BEFORE";

/**
 * Formats a date as dd/mm/yyyy for display in Turkish UI contexts.
 */
export function formatDateDdMmYyyy(date: Date): string {
  const day = String(date.getDate()).padStart(2, "0");
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const year = date.getFullYear();
  return `${day}/${month}/${year}`;
}

/**
 * Returns midnight in the local timezone for stable day-level comparisons.
 */
export function startOfLocalDay(date: Date): Date {
  return new Date(date.getFullYear(), date.getMonth(), date.getDate());
}

/**
 * Returns the local-day timestamp for an ISO date string.
 */
function toLocalDayTimestamp(isoDate: string): number {
  return startOfLocalDay(new Date(isoDate)).getTime();
}

/**
 * Returns true when no date filter is active, or when the record matches the selected day and mode.
 */
export function matchesDateFilter(
  isoDate: string,
  selected: Date | null,
  mode: DateComparisonMode | null,
): boolean {
  if (!selected || !mode) return true;

  const targetDay = startOfLocalDay(selected).getTime();
  const recordDay = toLocalDayTimestamp(isoDate);

  switch (mode) {
    case "ON":
      return recordDay === targetDay;
    case "AFTER":
      return recordDay > targetDay;
    case "BEFORE":
      return recordDay < targetDay;
    default:
      return true;
  }
}

/**
 * Returns true when a date filter is considered active (both date and mode are set).
 */
export function isDateFilterActive(
  selected: Date | null,
  mode: DateComparisonMode | null,
): boolean {
  return selected !== null && mode !== null;
}

/** Guard against invalid Date objects in filter state. */
function isValidDate(date: Date | null | undefined): date is Date {
  return date instanceof Date && !Number.isNaN(date.getTime());
}

/** Normalizes selected calendar days to midnight local time. */
export function normalizeSelectedDate(date: Date | undefined): Date | null {
  if (!isValidDate(date)) return null;
  return startOfLocalDay(date);
}
