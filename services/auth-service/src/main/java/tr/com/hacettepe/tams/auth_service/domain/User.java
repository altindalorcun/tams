package tr.com.hacettepe.tams.auth_service.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Persistent representation of a platform user.
 * Password is always stored as a BCrypt hash — never in plain text.
 * Role is immutable after creation; changing a user's role requires
 * an explicit admin operation.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String username;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(name = "student_number", unique = true, length = 20)
    private String studentNumber;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    @Column(name = "must_change_password", nullable = false)
    @Builder.Default
    private boolean mustChangePassword = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false,
            columnDefinition = "TIMESTAMPTZ")
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false,
            columnDefinition = "TIMESTAMPTZ")
    private OffsetDateTime updatedAt;
}
