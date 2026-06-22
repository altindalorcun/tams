ALTER TABLE departments
    ADD COLUMN min_total_ects       NUMERIC(6,2),
    ADD COLUMN block_on_any_f_grade BOOLEAN NOT NULL DEFAULT FALSE;
