package tr.com.hacettepe.tams.analysis_service.dto.kafka;

import java.util.List;

/**
 * A semester block within a {@link ParsedTranscriptMessage}.
 * Mirrors the {@code Semester} model in parser-service.
 */
public record ParsedSemester(
        String name,
        List<ParsedCourse> courses
) {}
