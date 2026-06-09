package tr.com.hacettepe.tams.analysis_service;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.DockerClientFactory;

/**
 * JUnit 5 {@link ExecutionCondition} that disables a test class entirely
 * when a Docker daemon is not reachable, preventing Spring context loading
 * failures in CI environments that lack Docker.
 */
public class DockerAvailableCondition implements ExecutionCondition {

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        try {
            DockerClientFactory.instance().client();
            return ConditionEvaluationResult.enabled("Docker daemon is available");
        } catch (Exception e) {
            return ConditionEvaluationResult.disabled(
                    "Docker daemon not available — integration test skipped: " + e.getMessage());
        }
    }
}
