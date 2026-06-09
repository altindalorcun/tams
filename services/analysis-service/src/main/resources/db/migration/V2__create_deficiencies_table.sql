-- One row per unfulfilled graduation requirement category within a result.
-- missing_courses holds the course codes the student has not yet passed.
CREATE TABLE deficiencies (
    id               UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    result_id        UUID         NOT NULL REFERENCES analysis_results(id) ON DELETE CASCADE,
    category_name    VARCHAR(255) NOT NULL,
    required_credit  NUMERIC(5,2) NOT NULL,
    earned_credit    NUMERIC(5,2) NOT NULL,
    required_ects    NUMERIC(5,2) NOT NULL,
    earned_ects      NUMERIC(5,2) NOT NULL,
    missing_courses  TEXT[]
);

CREATE INDEX idx_deficiencies_result_id ON deficiencies (result_id);
