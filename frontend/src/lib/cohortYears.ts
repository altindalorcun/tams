/** Earliest enrollment year offered in cohort year pickers. */
export const COHORT_YEAR_MIN = 2000;

/** Buffer beyond the current year for upcoming cohorts. */
export const COHORT_YEAR_FUTURE_BUFFER = 5;

/** Latest enrollment year offered in cohort year pickers. */
export const COHORT_YEAR_MAX = new Date().getFullYear() + COHORT_YEAR_FUTURE_BUFFER;

export interface DecadeYearGroup {
  label: string;
  years: number[];
}

/** Returns the start year of the decade containing {@code year} (e.g. 2017 → 2010). */
export function getDecadeStart(year: number): number {
  return Math.floor(year / 10) * 10;
}

/** Clamps {@code year} to the cohort picker range. */
export function clampCohortYear(
  year: number,
  min: number = COHORT_YEAR_MIN,
  max: number = COHORT_YEAR_MAX,
): number {
  return Math.min(max, Math.max(min, year));
}

/**
 * Parses a year input string for cohort fields.
 * Returns empty string for blank, partial, or out-of-range input.
 */
export function parseCohortYearInput(
  raw: string,
  min: number = COHORT_YEAR_MIN,
  max: number = COHORT_YEAR_MAX,
): number | "" {
  const trimmed = raw.trim();
  if (trimmed === "") {
    return "";
  }
  if (!/^\d{4}$/.test(trimmed)) {
    return "";
  }
  const year = Number(trimmed);
  if (year < min || year > max) {
    return "";
  }
  return year;
}

/** Lists years within a single decade that fall inside [min, max]. */
export function getYearsInDecade(
  decadeStart: number,
  min: number = COHORT_YEAR_MIN,
  max: number = COHORT_YEAR_MAX,
): number[] {
  const years: number[] = [];
  for (let year = decadeStart; year <= decadeStart + 9; year++) {
    if (year >= min && year <= max) {
      years.push(year);
    }
  }
  return years;
}

/**
 * Groups consecutive years into decade buckets for the cohort year picker UI.
 * Partial decades at the range boundaries include only years within [min, max].
 */
export function groupYearsByDecade(
  min: number = COHORT_YEAR_MIN,
  max: number = COHORT_YEAR_MAX,
): DecadeYearGroup[] {
  const groups: DecadeYearGroup[] = [];
  const startDecade = getDecadeStart(min);
  const endDecade = getDecadeStart(max);

  for (let decade = startDecade; decade <= endDecade; decade += 10) {
    const years = getYearsInDecade(decade, min, max);
    if (years.length > 0) {
      groups.push({
        label: `${years[0]}–${years[years.length - 1]}`,
        years,
      });
    }
  }

  return groups;
}
