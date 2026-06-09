package tr.com.hacettepe.tams.analysis_service.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import tr.com.hacettepe.tams.analysis_service.client.dto.RuleSetResponse;
import tr.com.hacettepe.tams.analysis_service.exception.ResourceNotFoundException;

import java.util.UUID;

/**
 * Calls rule-service's internal endpoint to retrieve the full graduation rule set
 * for a given department. Requests are made synchronously (blocking) because the
 * Kafka consumer thread already runs outside the web request lifecycle.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RuleServiceClient {

    private final WebClient ruleServiceWebClient;

    /**
     * Fetches the complete rule set (categories + course pools) for the given department.
     *
     * @param departmentId the UUID of the department
     * @return the rule set response from rule-service
     * @throws ResourceNotFoundException if rule-service returns 404 for this department
     * @throws RuleServiceUnavailableException if rule-service is unreachable or returns a 5xx error
     */
    public RuleSetResponse getRuleSet(UUID departmentId) {
        log.debug("Fetching rule set for departmentId={}", departmentId);
        try {
            RuleSetResponse response = ruleServiceWebClient.get()
                    .uri("/internal/rules/{departmentId}", departmentId)
                    .retrieve()
                    .bodyToMono(RuleSetResponse.class)
                    .block();

            if (response == null) {
                throw new RuleServiceUnavailableException(
                        "rule-service returned empty body for departmentId=" + departmentId);
            }
            log.debug("Rule set fetched: departmentId={}, categories={}",
                    departmentId, response.categories().size());
            return response;
        } catch (WebClientResponseException.NotFound e) {
            throw new ResourceNotFoundException(
                    "No graduation rules defined for departmentId=" + departmentId);
        } catch (WebClientResponseException e) {
            throw new RuleServiceUnavailableException(
                    "rule-service responded with HTTP " + e.getStatusCode() + " for departmentId=" + departmentId);
        } catch (Exception e) {
            if (e instanceof ResourceNotFoundException || e instanceof RuleServiceUnavailableException) {
                throw e;
            }
            throw new RuleServiceUnavailableException(
                    "Failed to reach rule-service for departmentId=" + departmentId + ": " + e.getMessage());
        }
    }

    /**
     * Thrown when rule-service is unreachable, times out, or returns a 5xx error.
     */
    public static class RuleServiceUnavailableException extends RuntimeException {
        public RuleServiceUnavailableException(String message) {
            super(message);
        }
    }
}
