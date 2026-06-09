package tr.com.hacettepe.tams.analysis_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import tr.com.hacettepe.tams.analysis_service.AbstractIntegrationTest;
import tr.com.hacettepe.tams.analysis_service.domain.AnalysisStatus;
import tr.com.hacettepe.tams.analysis_service.repository.AnalysisResultRepository;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for {@link TranscriptUploadController}.
 * Tests the full HTTP layer including JWT auth, multipart upload, and status polling.
 */
class TranscriptUploadControllerIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AnalysisResultRepository analysisResultRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private final UUID teacherId = UUID.randomUUID();
    private final UUID departmentId = UUID.randomUUID();

    private MockMultipartFile fakePdf() {
        return new MockMultipartFile("file", "transcript.pdf",
                "application/pdf", "fake-pdf-content".getBytes());
    }

    @BeforeEach
    void cleanDatabase() {
        analysisResultRepository.deleteAll();
    }

    @Test
    void upload_withTeacherRole_returns202WithJobId() throws Exception {
        String token = bearerToken("TEACHER", teacherId);

        String body = mockMvc.perform(multipart("/api/v1/transcripts")
                        .file(fakePdf())
                        .param("departmentId", departmentId.toString())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.jobId").isNotEmpty())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn().getResponse().getContentAsString();

        String jobId = objectMapper.readTree(body).get("jobId").asText();
        assertThat(analysisResultRepository.findByJobId(jobId)).isPresent();
        assertThat(analysisResultRepository.findByJobId(jobId).get().getStatus())
                .isEqualTo(AnalysisStatus.PENDING);
    }

    @Test
    void upload_withStudentRole_returns403() throws Exception {
        String token = bearerToken("STUDENT", UUID.randomUUID());

        mockMvc.perform(multipart("/api/v1/transcripts")
                        .file(fakePdf())
                        .param("departmentId", departmentId.toString())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void upload_withAdminRole_returns403() throws Exception {
        String token = bearerToken("ADMIN", UUID.randomUUID());

        mockMvc.perform(multipart("/api/v1/transcripts")
                        .file(fakePdf())
                        .param("departmentId", departmentId.toString())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void upload_withoutAuthentication_returns401() throws Exception {
        mockMvc.perform(multipart("/api/v1/transcripts")
                        .file(fakePdf())
                        .param("departmentId", departmentId.toString()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void upload_withMissingDepartmentId_returns400() throws Exception {
        String token = bearerToken("TEACHER", teacherId);

        mockMvc.perform(multipart("/api/v1/transcripts")
                        .file(fakePdf())
                        // departmentId intentionally omitted
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void upload_persistsResultLinkedToAuthenticatedTeacher() throws Exception {
        String token = bearerToken("TEACHER", teacherId);

        String body = mockMvc.perform(multipart("/api/v1/transcripts")
                        .file(fakePdf())
                        .param("departmentId", departmentId.toString())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsString();

        String jobId = objectMapper.readTree(body).get("jobId").asText();
        var result = analysisResultRepository.findByJobId(jobId).orElseThrow();
        assertThat(result.getTeacherId()).isEqualTo(teacherId);
        assertThat(result.getDepartmentId()).isEqualTo(departmentId);
    }

    @Test
    void getJobStatus_existingJob_returnsPendingStatus() throws Exception {
        String token = bearerToken("TEACHER", teacherId);

        String body = mockMvc.perform(multipart("/api/v1/transcripts")
                        .file(fakePdf())
                        .param("departmentId", departmentId.toString())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsString();

        String jobId = objectMapper.readTree(body).get("jobId").asText();

        mockMvc.perform(get("/api/v1/transcripts/{jobId}/status", jobId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId").value(jobId))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void getJobStatus_unknownJobId_returns404() throws Exception {
        String token = bearerToken("TEACHER", teacherId);

        mockMvc.perform(get("/api/v1/transcripts/{jobId}/status", "non-existent-job")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void getJobStatus_withoutAuthentication_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/transcripts/{jobId}/status", "any-job"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getJobStatus_withStudentRole_returns403() throws Exception {
        String token = bearerToken("STUDENT", UUID.randomUUID());

        mockMvc.perform(get("/api/v1/transcripts/{jobId}/status", "any-job")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }
}
