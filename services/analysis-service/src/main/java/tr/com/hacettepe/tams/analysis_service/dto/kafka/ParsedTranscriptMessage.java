package tr.com.hacettepe.tams.analysis_service.dto.kafka;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Transcript payload consumed from the {@code transcript.parsed} Kafka topic.
 * Mirrors the {@code ParsedTranscript} model in parser-service.
 * The {@code metadata} field is optional and may be null when parser-service omits it.
 */
public record ParsedTranscriptMessage(
        @JsonProperty("student_number") String studentNumber,
        @JsonProperty("job_id") String jobId,
        @JsonProperty("teacher_id") String teacherId,
        @JsonProperty("department_id") String departmentId,
        List<ParsedSemester> semesters,
        TranscriptMetadataDto metadata
) {
    /**
     * Returns a flat list of all courses across all semesters,
     * with the originating semester name embedded in each course's context.
     */
    public List<ParsedCourse> allCourses() {
        if (semesters == null) {
            return List.of();
        }
        return semesters.stream()
                .filter(s -> s.courses() != null)
                .flatMap(s -> s.courses().stream())
                .collect(Collectors.toList());
    }
}
