-- Creates the curriculum_equivalence_rules table to replace the simpler exemption_rules table.
-- This model supports bi-directional and group equivalencies arising from curriculum changes
-- (e.g. HAS222↔MUH103, BBM419↔BBM479+BBM480, FIZ103+FIZ104→FIZ117).
CREATE TABLE curriculum_equivalence_rules (
    id                       UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    department_id            UUID        NOT NULL REFERENCES departments(id) ON DELETE CASCADE,
    rule_type                VARCHAR(30) NOT NULL,
    legacy_course_codes      TEXT[]      NOT NULL,
    replacement_course_codes TEXT[]      NOT NULL,
    effective_from_year      INTEGER,
    effective_from_term      VARCHAR(10),
    created_at               TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_curriculum_equivalence_rules_department_id ON curriculum_equivalence_rules (department_id);

-- Migrate existing exemption_rules rows to the new table as GROUP_LEGACY_TO_REPLACEMENT rules.
-- required_course_codes → legacy_course_codes (already "the old courses to pass")
-- exempted_course_code  → replacement_course_codes (a single-element array of the new course)
INSERT INTO curriculum_equivalence_rules
    (department_id, rule_type, legacy_course_codes, replacement_course_codes, created_at)
SELECT
    department_id,
    'GROUP_LEGACY_TO_REPLACEMENT',
    required_course_codes,
    ARRAY[exempted_course_code],
    created_at
FROM exemption_rules;

DROP TABLE exemption_rules;
