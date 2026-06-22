ALTER TABLE categories
    ADD COLUMN condition_course_codes  TEXT[]       NOT NULL DEFAULT '{}',
    ADD COLUMN min_course_count_if_met INTEGER,
    ADD COLUMN min_ects_if_met         NUMERIC(5,2);
