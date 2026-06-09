package tr.com.hacettepe.tams.analysis_service.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Binds the {@code app.kafka.topics.*} block from {@code application.yml}.
 */
@Validated
@ConfigurationProperties(prefix = "app.kafka.topics")
public record KafkaTopicProperties(
        @NotBlank String transcriptRaw,
        @NotBlank String transcriptParsed
) {}
