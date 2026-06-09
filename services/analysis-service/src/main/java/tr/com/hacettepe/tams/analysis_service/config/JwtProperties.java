package tr.com.hacettepe.tams.analysis_service.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Binds the {@code app.jwt.*} block from {@code application.yml}.
 * analysis-service only validates tokens (never issues them), so only the shared
 * secret is required.
 */
@Validated
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        @NotBlank String secret
) {}
