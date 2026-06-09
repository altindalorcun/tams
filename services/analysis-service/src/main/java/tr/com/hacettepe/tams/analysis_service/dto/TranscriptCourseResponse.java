package tr.com.hacettepe.tams.analysis_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import tr.com.hacettepe.tams.analysis_service.domain.TranscriptCourse;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Read-only snapshot of a single course taken from a parsed transcript.
 */
@Schema(description = "Single course entry from the parsed transcript snapshot")
public record TranscriptCourseResponse(
        @Schema(description = "Primary key of this transcript course record")
        UUID id,
        @Schema(description = "Official course code", example = "CS301")
        String courseCode,
        @Schema(description = "Full course name", example = "Algorithms and Data Structures")
        String courseName,
        @Schema(description = "Credit hours of the course", example = "3.0")
        BigDecimal credit,
        @Schema(description = "ECTS points of the course", example = "5.0")
        BigDecimal ects,
        @Schema(description = "Letter grade received", example = "AA")
        String grade,
        @Schema(description = "Academic semester label", example = "2022-2023 Güz")
        String semester,
        @Schema(description = "True when the student passed this course")
        boolean passed
) {
    public static TranscriptCourseResponse from(TranscriptCourse tc) {
        return new TranscriptCourseResponse(
                tc.getId(),
                tc.getCourseCode(),
                tc.getCourseName(),
                tc.getCredit(),
                tc.getEcts(),
                tc.getGrade(),
                tc.getSemester(),
                tc.isPassed()
        );
    }
}
