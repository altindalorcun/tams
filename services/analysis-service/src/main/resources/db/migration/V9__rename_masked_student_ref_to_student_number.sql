-- Rename masked_student_ref to student_number; store plain Öğrenci No for JWT matching.
ALTER TABLE analysis_results RENAME COLUMN masked_student_ref TO student_number;
ALTER TABLE analysis_results ALTER COLUMN student_number TYPE VARCHAR(20);
DROP INDEX IF EXISTS idx_analysis_results_student_ref;
CREATE INDEX idx_analysis_results_student_number ON analysis_results (student_number);
