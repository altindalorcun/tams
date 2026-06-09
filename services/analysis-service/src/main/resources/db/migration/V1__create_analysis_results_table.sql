-- One row per analysis run. job_id correlates with the Kafka message published
-- to transcript.raw and is used for async status polling by the caller.
CREATE TABLE analysis_results (
    id                  UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id              VARCHAR(36)  NOT NULL UNIQUE,
    masked_student_ref  VARCHAR(80),                        -- populated after parsing; null while PENDING
    teacher_id          UUID         NOT NULL,
    department_id       UUID         NOT NULL,
    status              VARCHAR(20)  NOT NULL DEFAULT 'PENDING'
                            CHECK (status IN ('PENDING', 'COMPLETED', 'FAILED')),
    is_eligible         BOOLEAN,                            -- null until analysis completes
    total_credit        NUMERIC(6,2) NOT NULL DEFAULT 0,
    total_ects          NUMERIC(6,2) NOT NULL DEFAULT 0,
    error_message       TEXT,                               -- populated on FAILED status
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    completed_at        TIMESTAMPTZ
);

CREATE INDEX idx_analysis_results_teacher_id   ON analysis_results (teacher_id);
CREATE INDEX idx_analysis_results_student_ref  ON analysis_results (masked_student_ref);
