package tr.com.hacettepe.tams.auth_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import tr.com.hacettepe.tams.auth_service.config.AdminProperties;
import tr.com.hacettepe.tams.auth_service.domain.Role;
import tr.com.hacettepe.tams.auth_service.domain.User;
import tr.com.hacettepe.tams.auth_service.repository.UserRepository;

/**
 * Creates the initial Admin account on first startup if it does not exist.
 * Credentials are supplied via environment variables — never hardcoded.
 * Subsequent restarts are idempotent: the runner exits early when the
 * admin email is already present.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminSeedRunner implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminProperties adminProperties;

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.existsByEmail(adminProperties.seedEmail())) {
            log.info("Admin account already exists — skipping seed");
            return;
        }

        User admin = User.builder()
                .username(adminProperties.seedUsername())
                .email(adminProperties.seedEmail())
                .passwordHash(passwordEncoder.encode(adminProperties.seedPassword()))
                .role(Role.ADMIN)
                .build();

        userRepository.save(admin);
        log.info("Admin account created: {}", adminProperties.seedEmail());
    }
}
