package tr.com.hacettepe.tams.auth_service.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import tr.com.hacettepe.tams.auth_service.domain.Role;
import tr.com.hacettepe.tams.auth_service.domain.User;
import tr.com.hacettepe.tams.auth_service.dto.UserResponse;
import tr.com.hacettepe.tams.auth_service.repository.RefreshTokenRepository;
import tr.com.hacettepe.tams.auth_service.repository.UserRepository;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AdminUserServiceImpl}.
 * Repository dependencies are mocked — no Spring context or database is involved.
 */
@ExtendWith(MockitoExtension.class)
class AdminUserServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminUserServiceImpl adminUserService;

    @Test
    @DisplayName("listUsers — returns non-admin users in repository sort order")
    void listUsers_preservesRepositorySortOrder() {
        User alpha = user("alpha", Role.TEACHER);
        User zeta = user("zeta", Role.STUDENT);
        when(userRepository.findAllByRoleNotSortedByUsernameAsc(Role.ADMIN))
                .thenReturn(List.of(alpha, zeta));

        List<UserResponse> result = adminUserService.listUsers();

        assertThat(result).extracting(UserResponse::username)
                .containsExactly("alpha", "zeta");
    }

    @Test
    @DisplayName("listUsers — empty repository returns empty list")
    void listUsers_empty_returnsEmpty() {
        when(userRepository.findAllByRoleNotSortedByUsernameAsc(Role.ADMIN))
                .thenReturn(List.of());

        assertThat(adminUserService.listUsers()).isEmpty();
    }

    private static User user(String username, Role role) {
        return User.builder()
                .id(UUID.randomUUID())
                .username(username)
                .email(username + "@example.com")
                .passwordHash("hash")
                .role(role)
                .build();
    }
}
