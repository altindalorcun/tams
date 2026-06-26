/** Earliest enrollment year offered in cohort year pickers. */
export const COHORT_YEAR_MIN = 1990;

/** Small buffer beyond the current year for upcoming cohorts. */
export const COHORT_YEAR_FUTURE_BUFFER = 2;

/** Latest enrollment year offered in cohort year pickers. */
export const COHORT_YEAR_MAX = new Date().getFullYear() + COHORT_YEAR_FUTURE_BUFFER;

export interface DecadeYearGroup {
  label: string;
  years: number[];
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
  const startDecade = Math.floor(min / 10) * 10;
  const endDecade = Math.floor(max / 10) * 10;

  for (let decade = startDecade; decade <= endDecade; decade += 10) {
    const decadeEnd = decade + 9;
    const years: number[] = [];
    for (let year = decade; year <= decadeEnd; year++) {
      if (year >= min && year <= max) {
        years.push(year);
      }
    }
    if (years.length > 0) {
      groups.push({
        label: `${years[0]}–${years[years.length - 1]}`,
        years,
      });
    }
  }

  return groups;
}
