-- Links a student account to the teacher who uploaded their transcript.
-- A student can only log in and view results if a row exists here.
CREATE TABLE teacher_student_map (
    teacher_id UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    student_id UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (teacher_id, student_id)
);
