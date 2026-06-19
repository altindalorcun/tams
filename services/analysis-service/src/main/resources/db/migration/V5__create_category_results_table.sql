CREATE TABLE category_results (
    id                        UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    result_id                 UUID         NOT NULL REFERENCES analysis_results(id) ON DELETE CASCADE,
    category_id               UUID         NOT NULL,
    category_name             VARCHAR(255) NOT NULL,
    satisfied                 BOOLEAN      NOT NULL,
    required_credit           NUMERIC(5,2) NOT NULL DEFAULT 0,
    earned_credit             NUMERIC(5,2) NOT NULL DEFAULT 0,
    required_ects             NUMERIC(5,2) NOT NULL DEFAULT 0,
    earned_ects               NUMERIC(5,2) NOT NULL DEFAULT 0,
    required_course_count     INTEGER      NOT NULL DEFAULT 0,
    earned_course_count       INTEGER      NOT NULL DEFAULT 0,
    missing_mandatory_courses TEXT[]       NOT NULL DEFAULT '{}',
    created_at                TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_category_results_result_id ON category_results (result_id);
