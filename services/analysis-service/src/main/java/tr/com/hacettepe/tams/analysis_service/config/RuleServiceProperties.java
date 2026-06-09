package tr.com.hacettepe.tams.analysis_service.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Binds the {@code app.rule-service.*} block from {@code application.yml}.
 */
@Validated
@ConfigurationProperties(prefix = "app.rule-service")
public record RuleServiceProperties(
        @NotBlank String baseUrl
) {}
