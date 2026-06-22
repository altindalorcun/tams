package tr.com.hacettepe.tams.auth_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tr.com.hacettepe.tams.auth_service.domain.Role;
import tr.com.hacettepe.tams.auth_service.domain.User;
import tr.com.hacettepe.tams.auth_service.dto.CreateUserRequest;
import tr.com.hacettepe.tams.auth_service.dto.UpdateUserRequest;
import tr.com.hacettepe.tams.auth_service.dto.UserResponse;
import tr.com.hacettepe.tams.auth_service.exception.ConflictException;
import tr.com.hacettepe.tams.auth_service.exception.ResourceNotFoundException;
import tr.com.hacettepe.tams.auth_service.exception.UnauthorizedException;
import tr.com.hacettepe.tams.auth_service.repository.RefreshTokenRepository;
import tr.com.hacettepe.tams.auth_service.repository.UserRepository;

import java.util.List;
import java.util.UUID;

/**
 * Default password assigned to every new user and to reset-password operations.
 * Users are required to change it on first login (mustChangePassword=true).
 */
@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    static final String DEFAULT_PASSWORD = "Tams2026!";

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        if (request.role() == Role.ADMIN) {
            throw new UnauthorizedException("ADMIN accounts cannot be created via this endpoint");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("Email already in use: " + request.email());
        }
        if (userRepository.existsByUsername(request.username())) {
            throw new ConflictException("Username already in use: " + request.username());
        }
        if (request.role() == Role.STUDENT) {
            if (request.studentNumber() == null || request.studentNumber().isBlank()) {
                throw new IllegalArgumentException("studentNumber is required for STUDENT role");
            }
            if (userRepository.existsByStudentNumber(request.studentNumber())) {
                throw new ConflictException("Student number already in use: " + request.studentNumber());
            }
        }

        User user = User.builder()
                .username(request.username())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(DEFAULT_PASSWORD))
                .role(request.role())
                .studentNumber(request.studentNumber())
                .mustChangePassword(true)
                .build();

        return UserResponse.from(userRepository.save(user));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> listUsers() {
        return userRepository.findAll().stream()
                .filter(u -> u.getRole() != Role.ADMIN)
                .map(UserResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public UserResponse updateUser(UUID id, UpdateUserRequest request) {
        User user = findNonAdminUser(id);

        if (!user.getEmail().equals(request.email()) && userRepository.existsByEmail(request.email())) {
            throw new ConflictException("Email already in use: " + request.email());
        }
        if (!user.getUsername().equals(request.username()) && userRepository.existsByUsername(request.username())) {
            throw new ConflictException("Username already in use: " + request.username());
        }

        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setActive(request.isActive());

        return UserResponse.from(userRepository.save(user));
    }

    @Override
    @Transactional
    public void deleteUser(UUID id) {
        User user = findNonAdminUser(id);
        refreshTokenRepository.deleteAllByUser(user);
        userRepository.delete(user);
    }

    @Override
    @Transactional
    public void resetPassword(UUID id) {
        User user = findNonAdminUser(id);
        user.setPasswordHash(passwordEncoder.encode(DEFAULT_PASSWORD));
        user.setMustChangePassword(true);
        userRepository.save(user);
        refreshTokenRepository.deleteAllByUser(user);
    }

    private User findNonAdminUser(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
        if (user.getRole() == Role.ADMIN) {
            throw new UnauthorizedException("Admin accounts cannot be managed via this endpoint");
        }
        return user;
    }
}
