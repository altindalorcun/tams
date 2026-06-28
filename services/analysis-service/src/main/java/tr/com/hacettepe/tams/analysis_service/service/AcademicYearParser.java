package tr.com.hacettepe.tams.analysis_service.service;

import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Parses the "Başarı Yılı" (course completion year) from Hacettepe transcript format.
 *
 * <p>Transcripts encode the year a course was passed as a two-digit range such as
 * {@code "16-17"}, meaning the 2016-2017 academic year. The start year (2016) is
 * used as the canonical completion year for boundary comparisons.
 *
 * <p>Examples:
 * <pre>
 *   "16-17" → 2016
 *   "19-20" → 2019
 *   "20-21" → 2020
 * </pre>
 */
@Component
public class AcademicYearParser {

    /**
     * Parses a transcript academic year string (e.g. {@code "16-17"}) and returns
     * the four-digit start year (e.g. {@code 2016}).
     *
     * @param academicYear the raw two-digit range from the transcript, may be null
     * @return the start year as an {@link Optional}, or empty if the input is absent or unparseable
     */
    public Optional<Integer> parse(String academicYear) {
        if (academicYear == null || academicYear.isBlank()) {
            return Optional.empty();
        }
        String[] parts = academicYear.trim().split("-");
        if (parts.length != 2) {
            return Optional.empty();
        }
        try {
            int twoDigitYear = Integer.parseInt(parts[0].trim());
            // Two-digit years in the range 00-99: add 2000 (all Hacettepe courses are post-2000).
            return Optional.of(2000 + twoDigitYear);
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    /**
     * Returns true when the given academic year string represents a completion that occurred
     * strictly before the specified effective year (and optional term).
     *
     * <p>Term semantics: {@code GUZ} (fall) begins in September of {@code effectiveFromYear},
     * {@code BAHAR} (spring) begins in January of {@code effectiveFromYear + 1}. A course
     * completed in the academic year {@code effectiveFromYear - 1} is always before the boundary.
     * A course completed in {@code effectiveFromYear} is considered before the boundary only when
     * the term is {@code BAHAR} (i.e. the change took effect in fall, so the spring of the same
     * start year is still under the old curriculum).
     *
     * @param academicYear       the raw transcript year string (e.g. {@code "16-17"})
     * @param effectiveFromYear  the calendar year the curriculum change took effect
     * @param effectiveFromTerm  {@code "GUZ"}, {@code "BAHAR"}, or null (null treated as {@code "GUZ"})
     * @return true if the course was completed before the curriculum change
     */
    public boolean isBeforeEffective(String academicYear, int effectiveFromYear, String effectiveFromTerm) {
        Optional<Integer> startYear = parse(academicYear);
        if (startYear.isEmpty()) {
            return false;
        }
        int courseYear = startYear.get();
        if (courseYear < effectiveFromYear) {
            return true;
        }
        // A course from the same start year as the effective year is "before" only when the
        // change took effect in the fall term (GUZ). If the change was in spring (BAHAR) the
        // course is in the same fall semester as the old curriculum, so it still counts.
        if (courseYear == effectiveFromYear && "BAHAR".equals(effectiveFromTerm)) {
            return true;
        }
        return false;
    }
}
