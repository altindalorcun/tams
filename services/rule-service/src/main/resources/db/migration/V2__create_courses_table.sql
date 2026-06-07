-- Institution-wide course catalog.
-- course_code is unique across the university (e.g. MAT101 always refers to the
-- same course regardless of which departments offer it).
CREATE TABLE courses (
    id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    course_code  VARCHAR(20)  NOT NULL UNIQUE,
    course_name  VARCHAR(255) NOT NULL,
    credit       NUMERIC(4,2) NOT NULL,
    ects         NUMERIC(4,2) NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_courses_course_code ON courses (course_code);
