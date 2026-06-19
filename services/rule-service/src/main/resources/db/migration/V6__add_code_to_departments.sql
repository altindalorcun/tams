ALTER TABLE departments
    ADD COLUMN code VARCHAR(20) NOT NULL DEFAULT '',
    ADD CONSTRAINT uq_departments_code UNIQUE (code);
ALTER TABLE departments ALTER COLUMN code DROP DEFAULT;
CREATE INDEX idx_departments_code ON departments (code);
