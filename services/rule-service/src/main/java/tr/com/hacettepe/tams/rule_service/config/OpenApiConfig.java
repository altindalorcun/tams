package tr.com.hacettepe.tams.rule_service.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures the OpenAPI 3.0 specification for rule-service.
 *
 * <p>Declares a Bearer JWT security scheme so Swagger UI presents an
 * "Authorize" button. All {@code /api/v1/**} endpoints require this
 * scheme; {@code /internal/**} and {@code /actuator/health} are
 * intentionally excluded as they are not exposed through the api-gateway.
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("TAMS — rule-service API")
                        .description(
                                "Manages university departments, the institution-wide course catalog, " +
                                "and department-scoped graduation categories. All write endpoints require " +
                                "an ADMIN JWT. The `/internal/rules/{departmentId}` endpoint is consumed " +
                                "exclusively by analysis-service within the cluster network.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("TAMS Team")
                                .url("https://github.com/altindalorcun/tams")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                                .name(BEARER_SCHEME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT access token issued by auth-service. " +
                                             "Include as: Authorization: Bearer <token>")));
    }
}
