-- Rename year-only mandatory bounds to category-course applicability bounds
-- and add term (GUZ/BAHAR) granularity for enrollment cohort filtering.
ALTER TABLE category_courses
    RENAME COLUMN mandatory_from_year TO applies_from_year;

ALTER TABLE category_courses
    RENAME COLUMN mandatory_to_year TO applies_to_year;

ALTER TABLE category_courses
    ADD COLUMN applies_from_term VARCHAR(10),
    ADD COLUMN applies_to_term   VARCHAR(10);
