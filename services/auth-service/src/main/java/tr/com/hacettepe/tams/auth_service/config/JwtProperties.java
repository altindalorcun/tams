package tr.com.hacettepe.tams.auth_service.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Binds the {@code app.jwt.*} block from {@code application.yml}.
 * All JWT signing and expiry parameters are read exclusively from
 * environment variables — never hardcoded.
 */
@Validated
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        @NotBlank String secret,
        @Positive long accessExpirationMs,
        @Positive long refreshExpirationMs
) {}
