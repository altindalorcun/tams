package tr.com.hacettepe.tams.analysis_service.service;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;

/**
 * Parses a registration date string (format {@code DD.MM.YYYY}) into an enrollment year
 * and academic term.
 *
 * <p>Term mapping follows Hacettepe University's academic calendar:
 * <ul>
 *   <li>Months 09–12 → {@code "GUZ"} (fall semester)</li>
 *   <li>Months 01–08 → {@code "BAHAR"} (spring semester)</li>
 * </ul>
 *
 * <p>Returns {@code Optional.empty()} when the input is null or cannot be parsed.
 */
@Component
public class EnrollmentYearParser {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    /**
     * Extracts the calendar year from a registration date string.
     *
     * @param registrationDate date in {@code DD.MM.YYYY} format, may be null
     * @return the year as an {@link Optional}, or empty if the input is absent or unparseable
     */
    public Optional<Integer> parse(String registrationDate) {
        return parseDate(registrationDate).map(LocalDate::getYear);
    }

    /**
     * Determines the academic term from a registration date string.
     *
     * @param registrationDate date in {@code DD.MM.YYYY} format, may be null
     * @return {@code "GUZ"} for months 09–12, {@code "BAHAR"} for months 01–08,
     *         or {@code null} if the input is absent or unparseable
     */
    public String parseTerm(String registrationDate) {
        return parseDate(registrationDate)
                .map(date -> date.getMonthValue() >= 9 ? "GUZ" : "BAHAR")
                .orElse(null);
    }

    private Optional<LocalDate> parseDate(String registrationDate) {
        if (registrationDate == null || registrationDate.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(LocalDate.parse(registrationDate.trim(), FORMATTER));
        } catch (DateTimeParseException e) {
            return Optional.empty();
        }
    }
}
