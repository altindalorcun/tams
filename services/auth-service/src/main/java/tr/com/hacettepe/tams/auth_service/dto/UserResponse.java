package tr.com.hacettepe.tams.auth_service.dto;

import tr.com.hacettepe.tams.auth_service.domain.Role;
import tr.com.hacettepe.tams.auth_service.domain.User;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Read-only projection of a {@link User} entity returned by admin endpoints.
 * Password hash is never included in responses.
 */
public record UserResponse(
        UUID id,
        String username,
        String email,
        Role role,
        String studentNumber,
        boolean isActive,
        boolean mustChangePassword,
        OffsetDateTime createdAt
) {
    /**
     * Maps a {@link User} entity to a {@link UserResponse}.
     */
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.getStudentNumber(),
                user.isActive(),
                user.isMustChangePassword(),
                user.getCreatedAt()
        );
    }
}
