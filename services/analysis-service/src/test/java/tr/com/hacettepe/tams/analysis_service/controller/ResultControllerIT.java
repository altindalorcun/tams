package tr.com.hacettepe.tams.analysis_service.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import tr.com.hacettepe.tams.analysis_service.AbstractIntegrationTest;
import tr.com.hacettepe.tams.analysis_service.domain.AnalysisResult;
import tr.com.hacettepe.tams.analysis_service.domain.AnalysisStatus;
import tr.com.hacettepe.tams.analysis_service.repository.AnalysisResultRepository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for {@link ResultController}.
 * Verifies pagination, ownership checks, and role guards via the full HTTP stack.
 */
class ResultControllerIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AnalysisResultRepository analysisResultRepository;

    private final UUID teacherId = UUID.randomUUID();
    private final UUID otherTeacherId = UUID.randomUUID();
    private final UUID departmentId = UUID.randomUUID();
    private final String studentRef = "sha256:abcdef1234567890abcdef";

    private AnalysisResult savedResult;

    @BeforeEach
    void seed() {
        analysisResultRepository.deleteAll();
        savedResult = analysisResultRepository.save(completedResult(teacherId, studentRef));
    }

    private AnalysisResult completedResult(UUID teacher, String maskedRef) {
        AnalysisResult r = new AnalysisResult();
        r.setJobId(UUID.randomUUID().toString());
        r.setTeacherId(teacher);
        r.setDepartmentId(departmentId);
        r.setMaskedStudentRef(maskedRef);
        r.setStatus(AnalysisStatus.COMPLETED);
        r.setIsEligible(true);
        r.setTotalCredit(new BigDecimal("120.00"));
        r.setTotalEcts(new BigDecimal("240.00"));
        r.setCompletedAt(OffsetDateTime.now());
        return r;
    }

    // ── GET /api/v1/results ───────────────────────────────────────────────────

    @Test
    void listResults_withTeacherRole_returns200WithOwnResults() throws Exception {
        String token = bearerToken("TEACHER", teacherId);

        mockMvc.perform(get("/api/v1/results")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].teacherId").doesNotExist())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void listResults_withStudentRole_returns403() throws Exception {
        String token = bearerToken("STUDENT", UUID.randomUUID());

        mockMvc.perform(get("/api/v1/results")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void listResults_withAdminRole_returns403() throws Exception {
        String token = bearerToken("ADMIN", UUID.randomUUID());

        mockMvc.perform(get("/api/v1/results")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void listResults_withoutAuthentication_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/results"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listResults_withStudentRefFilter_returnsMatchingResults() throws Exception {
        // Save a second result with a different studentRef to confirm filtering
        analysisResultRepository.save(completedResult(teacherId, "sha256:different000000000000"));
        String token = bearerToken("TEACHER", teacherId);

        mockMvc.perform(get("/api/v1/results")
                        .param("studentRef", "abcdef")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void listResults_otherTeacherSeesNoResults_whenHasNone() throws Exception {
        String token = bearerToken("TEACHER", otherTeacherId);

        mockMvc.perform(get("/api/v1/results")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    // ── GET /api/v1/results/{id} ──────────────────────────────────────────────

    @Test
    void getResult_teacherOwnerOfResult_returns200WithDetail() throws Exception {
        String token = bearerToken("TEACHER", teacherId);

        mockMvc.perform(get("/api/v1/results/{id}", savedResult.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedResult.getId().toString()))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.isEligible").value(true))
                .andExpect(jsonPath("$.deficiencies").isArray())
                .andExpect(jsonPath("$.courses").isArray());
    }

    @Test
    void getResult_differentTeacher_returns403() throws Exception {
        String token = bearerToken("TEACHER", otherTeacherId);

        mockMvc.perform(get("/api/v1/results/{id}", savedResult.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void getResult_unknownId_returns404() throws Exception {
        String token = bearerToken("TEACHER", teacherId);

        mockMvc.perform(get("/api/v1/results/{id}", UUID.randomUUID())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void getResult_studentWithMatchingRef_returns200() throws Exception {
        String token = bearerToken("STUDENT", UUID.randomUUID());

        mockMvc.perform(get("/api/v1/results/{id}", savedResult.getId())
                        .param("studentRef", studentRef)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maskedStudentRef").value(studentRef));
    }

    @Test
    void getResult_studentWithMismatchedRef_returns403() throws Exception {
        String token = bearerToken("STUDENT", UUID.randomUUID());

        mockMvc.perform(get("/api/v1/results/{id}", savedResult.getId())
                        .param("studentRef", "sha256:wrongref00000000000000")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void getResult_studentWithNoRef_returns403() throws Exception {
        String token = bearerToken("STUDENT", UUID.randomUUID());

        mockMvc.perform(get("/api/v1/results/{id}", savedResult.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void getResult_withoutAuthentication_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/results/{id}", savedResult.getId()))
                .andExpect(status().isUnauthorized());
    }

    // ── GET /api/v1/results/me ────────────────────────────────────────────────

    @Test
    void getMyResult_studentWithKnownRef_returns200() throws Exception {
        String token = bearerToken("STUDENT", UUID.randomUUID());

        mockMvc.perform(get("/api/v1/results/me")
                        .param("studentRef", studentRef)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maskedStudentRef").value(studentRef))
                .andExpect(jsonPath("$.isEligible").value(true));
    }

    @Test
    void getMyResult_teacherRole_returns403() throws Exception {
        String token = bearerToken("TEACHER", teacherId);

        mockMvc.perform(get("/api/v1/results/me")
                        .param("studentRef", studentRef)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void getMyResult_unknownStudentRef_returns404() throws Exception {
        String token = bearerToken("STUDENT", UUID.randomUUID());

        mockMvc.perform(get("/api/v1/results/me")
                        .param("studentRef", "sha256:doesnotexist000000000")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void getMyResult_withoutAuthentication_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/results/me")
                        .param("studentRef", studentRef))
                .andExpect(status().isUnauthorized());
    }
}
