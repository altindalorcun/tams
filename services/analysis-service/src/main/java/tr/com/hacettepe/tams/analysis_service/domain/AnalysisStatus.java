package tr.com.hacettepe.tams.analysis_service.domain;

/**
 * Lifecycle states of a transcript analysis job.
 * Transitions: PENDING → COMPLETED or PENDING → FAILED.
 */
public enum AnalysisStatus {
    PENDING,
    COMPLETED,
    FAILED
}
