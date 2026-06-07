package tr.com.hacettepe.tams.auth_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tr.com.hacettepe.tams.auth_service.domain.TeacherStudentId;
import tr.com.hacettepe.tams.auth_service.domain.TeacherStudentMap;
import tr.com.hacettepe.tams.auth_service.dto.TeacherStudentRequest;
import tr.com.hacettepe.tams.auth_service.dto.TokenValidationResponse;
import tr.com.hacettepe.tams.auth_service.exception.ConflictException;
import tr.com.hacettepe.tams.auth_service.exception.ResourceNotFoundException;
import tr.com.hacettepe.tams.auth_service.repository.TeacherStudentMapRepository;
import tr.com.hacettepe.tams.auth_service.repository.UserRepository;
import tr.com.hacettepe.tams.auth_service.service.AuthService;

import java.util.UUID;

/**
 * Internal-only endpoints — not exposed through the api-gateway.
 * Accessible solely via Kubernetes ClusterIP service within the cluster.
 */
@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
@Tag(name = "Internal", description = "Cluster-internal endpoints for service-to-service calls")
public class InternalController {

    private final AuthService authService;
    private final TeacherStudentMapRepository teacherStudentMapRepository;
    private final UserRepository userRepository;

    @PostMapping("/auth/validate")
    @Operation(summary = "Validate a JWT and return user identity (used by api-gateway)")
    public ResponseEntity<TokenValidationResponse> validate(
            @RequestHeader("Authorization") String authorization) {
        return ResponseEntity.ok(authService.validate(authorization));
    }

    @PostMapping("/teacher-student")
    @Operation(summary = "Create a teacher-student relationship")
    public ResponseEntity<Void> createRelationship(@Valid @RequestBody TeacherStudentRequest request) {
        if (!userRepository.existsById(request.teacherId())) {
            throw new ResourceNotFoundException("Teacher not found: " + request.teacherId());
        }
        if (!userRepository.existsById(request.studentId())) {
            throw new ResourceNotFoundException("Student not found: " + request.studentId());
        }

        TeacherStudentId id = new TeacherStudentId(request.teacherId(), request.studentId());
        if (teacherStudentMapRepository.existsById(id)) {
            throw new ConflictException("Relationship already exists");
        }

        teacherStudentMapRepository.save(new TeacherStudentMap(id, null));
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/teacher-student/{studentId}")
    @Operation(summary = "Check if a student is linked to any teacher")
    public ResponseEntity<Void> checkStudentLinked(@PathVariable UUID studentId) {
        if (!teacherStudentMapRepository.existsByIdStudentId(studentId)) {
            throw new ResourceNotFoundException("No teacher-student relationship for student: " + studentId);
        }
        return ResponseEntity.ok().build();
    }
}
