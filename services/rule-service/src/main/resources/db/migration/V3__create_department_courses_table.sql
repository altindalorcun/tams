-- Junction table: which courses are offered by which department.
-- Admin populates this pool before assigning courses to graduation categories.
-- The same course may be offered by multiple departments.
CREATE TABLE department_courses (
    department_id  UUID NOT NULL REFERENCES departments(id) ON DELETE CASCADE,
    course_id      UUID NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    PRIMARY KEY (department_id, course_id)
);

CREATE INDEX idx_department_courses_department_id ON department_courses (department_id);
CREATE INDEX idx_department_courses_course_id     ON department_courses (course_id);
