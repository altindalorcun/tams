package tr.com.hacettepe.tams.analysis_service.dto.kafka;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A single course row within a {@link ParsedTranscriptMessage}.
 * Mirrors the {@code Course} model in parser-service.
 */
public record ParsedCourse(
        @JsonProperty("course_code") String courseCode,
        @JsonProperty("course_name") String courseName,
        double credit,
        double ects,
        String grade,
        @JsonProperty("academic_year") String academicYear,
        @JsonProperty("is_passed") boolean isPassed
) {}
