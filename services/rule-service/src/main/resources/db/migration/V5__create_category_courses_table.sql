-- Junction table: courses that belong to a graduation category.
-- is_mandatory = true means the student must pass this specific course regardless
-- of whether the min_course_count threshold would otherwise be satisfied by other courses.
CREATE TABLE category_courses (
    category_id  UUID    NOT NULL REFERENCES categories(id) ON DELETE CASCADE,
    course_id    UUID    NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    is_mandatory BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (category_id, course_id)
);

-- Supports fast lookups when fetching the full rule set for analysis-service
CREATE INDEX idx_category_courses_category_id ON category_courses (category_id);

-- Supports fast matching of transcript course codes against category rules
CREATE INDEX idx_category_courses_course_id ON category_courses (course_id);
