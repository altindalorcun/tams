CREATE TABLE exemption_rules (
    id                    UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    department_id         UUID        NOT NULL REFERENCES departments(id) ON DELETE CASCADE,
    required_course_codes TEXT[]      NOT NULL,
    exempted_course_code  VARCHAR(20) NOT NULL,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_exemption_rules_department_id ON exemption_rules (department_id);
