package tr.com.hacettepe.tams.api_gateway.config;

import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Binds the {@code app.rate-limit.*} block from {@code application.yml}.
 */
@Validated
@ConfigurationProperties(prefix = "app.rate-limit")
public record RateLimitProperties(
        @Positive int requestsPerSecond,
        @Positive int burstCapacity
) {}
