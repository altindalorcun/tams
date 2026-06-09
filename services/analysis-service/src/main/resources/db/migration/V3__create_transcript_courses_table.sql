-- Snapshot of every course found in the parsed transcript.
-- No PII: student identity is captured only as masked_student_ref in analysis_results.
CREATE TABLE transcript_courses (
    id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    result_id    UUID         NOT NULL REFERENCES analysis_results(id) ON DELETE CASCADE,
    course_code  VARCHAR(20)  NOT NULL,
    course_name  VARCHAR(255) NOT NULL,
    credit       NUMERIC(4,2) NOT NULL,
    ects         NUMERIC(4,2) NOT NULL,
    grade        VARCHAR(5),
    semester     VARCHAR(20),
    is_passed    BOOLEAN      NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_transcript_courses_result_id ON transcript_courses (result_id);
