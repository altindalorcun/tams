package tr.com.hacettepe.tams.analysis_service.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configures a {@link WebClient} bean pointed at rule-service.
 */
@Configuration
@RequiredArgsConstructor
public class WebClientConfig {

    private final RuleServiceProperties ruleServiceProperties;

    @Bean
    public WebClient ruleServiceWebClient() {
        return WebClient.builder()
                .baseUrl(ruleServiceProperties.baseUrl())
                .build();
    }
}
