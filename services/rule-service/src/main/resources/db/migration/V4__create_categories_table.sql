-- Graduation requirement groups scoped to a single department.
-- Examples: "Out-of-Department Mandatory", "Technical Elective Group A".
--
-- min_course_count: student must pass at least this many courses from the pool.
-- min_credit / min_ects: cumulative thresholds across passed courses in the pool.
-- Name is unique per department; the same category name may exist in different departments.
CREATE TABLE categories (
    id               UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    department_id    UUID         NOT NULL REFERENCES departments(id) ON DELETE CASCADE,
    name             VARCHAR(255) NOT NULL,
    description      TEXT,
    min_credit       NUMERIC(5,2) NOT NULL DEFAULT 0,
    min_ects         NUMERIC(5,2) NOT NULL DEFAULT 0,
    min_course_count INTEGER      NOT NULL DEFAULT 0,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    UNIQUE (department_id, name)
);

CREATE INDEX idx_categories_department_id ON categories (department_id);
