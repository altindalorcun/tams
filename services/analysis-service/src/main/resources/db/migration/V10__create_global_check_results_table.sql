CREATE TABLE global_check_results (
    id                  UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    result_id           UUID         NOT NULL REFERENCES analysis_results(id) ON DELETE CASCADE,
    check_type          VARCHAR(20)  NOT NULL,
    passed              BOOLEAN      NOT NULL,
    detail              TEXT         NOT NULL,
    required_min_ects   NUMERIC(6,2),
    earned_ects         NUMERIC(6,2),
    failed_course_codes TEXT[]       NOT NULL DEFAULT '{}',
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_global_check_results_result_id ON global_check_results (result_id);
