package tr.com.hacettepe.tams.auth_service.config;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Binds the {@code app.admin.*} block from {@code application.yml}.
 * Used by {@code AdminSeedRunner} to create the initial admin account.
 */
@Validated
@ConfigurationProperties(prefix = "app.admin")
public record AdminProperties(
        @NotBlank @Email String seedEmail,
        @NotBlank String seedUsername,
        @NotBlank String seedPassword
) {}
