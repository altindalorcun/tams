package tr.com.hacettepe.tams.auth_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tr.com.hacettepe.tams.auth_service.dto.CreateUserRequest;
import tr.com.hacettepe.tams.auth_service.dto.UpdateUserRequest;
import tr.com.hacettepe.tams.auth_service.dto.UserResponse;
import tr.com.hacettepe.tams.auth_service.service.AdminUserService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin — User Management", description = "CRUD operations for platform users (admin only)")
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    @Operation(summary = "List all non-admin users")
    public ResponseEntity<List<UserResponse>> listUsers() {
        return ResponseEntity.ok(adminUserService.listUsers());
    }

    @PostMapping
    @Operation(summary = "Create a new Teacher or Student account with a default password")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminUserService.createUser(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing user's username, email, or active status")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRequest request
    ) {
        return ResponseEntity.ok(adminUserService.updateUser(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a user and invalidate all their sessions")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
        adminUserService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/reset-password")
    @Operation(summary = "Reset a user's password to the system default")
    public ResponseEntity<Void> resetPassword(@PathVariable UUID id) {
        adminUserService.resetPassword(id);
        return ResponseEntity.noContent().build();
    }
}
