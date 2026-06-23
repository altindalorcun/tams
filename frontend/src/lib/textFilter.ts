const TURKISH_LOCALE = "tr-TR";

/**
 * Normalizes user-visible text for Turkish case-insensitive partial matching in client-side filters.
 */
export function normalizeForTextFilter(value: string): string {
  return value.normalize("NFKC").toLocaleLowerCase(TURKISH_LOCALE);
}

/**
 * Returns true when the filter is empty or the haystack contains the normalized filter substring.
 */
export function matchesTextFilter(haystack: string, filter: string): boolean {
  const needle = normalizeForTextFilter(filter.trim());
  if (!needle) return true;
  return normalizeForTextFilter(haystack).includes(needle);
}
