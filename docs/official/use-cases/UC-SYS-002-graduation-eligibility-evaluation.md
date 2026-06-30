| Use Case Number: | 002 | | |
| --- | --- | | | --- | --- |
| Use Case Code: | UC-SYS-002 | | |
| --- | --- | | | --- | --- |
| Use Case Name: | Graduation Eligibility Evaluation | | |
| --- | --- | | | --- | --- |

| Depicter:         | Agent | Last Modificatory:      | — |
| ----------------- | --- | ----------------------- | --- |
| Description Date: | 2026-06-30 | Last Modification Date: | — |
| ---               | --- | ---                     | --- |

| Actor(s):                | System (analysis-service Kafka consumer and GraduationEngine) |
| ------------------------ | ---------------------------------------------------------------------------------------------------------------------------------------------- |
| Short Definition:        | The system evaluates parsed transcript courses against the Admin-defined graduation rule set for the target department and persists a masked eligibility result with per-category breakdown and deficiencies. |
| ---                      | ---                                                                                                                                            |
| Pre-condition:           | UC-SYS-001 has published a `ParsedTranscriptMessage` to `transcript.parsed`. A PENDING `AnalysisResult` row exists for the jobId (created by UC-TEACH-001). Graduation rules exist for the department (UC-ADMIN-003). |
| ---                      | ---                                                                                                                                            |
| Post-condition:          | The analysis job status transitions to COMPLETED (or FAILED on error). Eligibility outcome, GPA, category results, global checks, deficiency records, and transcript course snapshot are persisted in postgres-analysis (FR-ANAL-004). Teacher and Student can retrieve results via UC-TEACH-002 / UC-STUD-001. |
| ---                      | ---                                                                                                                                            |
| Priority:                | Must |
| ---                      | ---                                                                                                                                            |
| Frequency of Occurrence: | Once per successfully parsed transcript; concurrent with graduation-period upload spikes. |
| ---                      | ---                                                                                                                                            |
| Main Path:               | 1. `TranscriptParsedConsumer` receives a message from `transcript.parsed` (manual Kafka acknowledgement).<br><br>2. The consumer deserializes `ParsedTranscriptMessage` and extracts jobId and departmentId.<br><br>3. analysis-service fetches the full rule set via `GET /internal/rules/{departmentId}` from rule-service (FR-RULE-003).<br><br>4. `EnrollmentYearParser` derives enrollment year and term (`GUZ`/`BAHAR`) from `registration_date` metadata (FR-RULE-007, BR-COHORT-001).<br><br>5. `GraduationEngine.evaluate()` applies curriculum equivalence expansion (BR-EQ-001), category-level cohort skip (BR-COHORT-005), course-level applicability (BR-COHORT-006–008), category thresholds (BR-GRAD-001–005), conditional thresholds (BR-GRAD-008), prefix sub-limits (BR-GRAD-009), and global department checks (BR-GRAD-006–007).<br><br>6. Overall eligibility is determined per BR-ELIG-001.<br><br>7. `ResultService.completeResult()` updates the PENDING row to COMPLETED with masked fields, category results, global checks, and transcript courses.<br><br>8. Kafka offset is acknowledged after successful persistence. |
| ---                      | ---                                                                                                                                            |
| Alternative Path:        | A.01 (alternative to step 3): rule-service unavailable — job marked FAILED with error message; teacher sees failure on status poll (FR-ANAL-002 reliability).<br><br>A.02 (alternative to step 2): Missing or invalid departmentId — job marked FAILED.<br><br>A.03 (alternative to step 2): Deserialization error — job marked FAILED; offset acknowledged to avoid poison-message retry loop.<br><br>A.04 (alternative to step 4): Missing registration date — per BR-COHORT-002, course-level cohort bounds are ignored; all assignments treated as applicable. |
| ---                      | ---                                                                                                                                            |
| Containing Use-Case(s)   | Included by UC-TEACH-001. Depends on UC-SYS-001 output and UC-ADMIN-003 rule definitions. |
| ---                      | ---                                                                                                                                            |
| Special Requirements:    | FR-ANAL-003, FR-ANAL-004, FR-RULE-003, FR-RULE-007. AD-006 (Graduation Rule Engine v2), AD-007 (Enrollment Cohort Comparator). All business rules BR-COHORT-* through BR-ELIG-001. No PII in persisted results beyond masked student number. |
| ---                      | ---                                                                                                                                            |
| Assumptions:             | Rule snapshot is fetched at evaluation time (no direct DB access to postgres-rules from analysis-service). Engine runs synchronously within the Kafka consumer callback. |
| ---                      | ---                                                                                                                                            |
| Note:                    | Components: `TranscriptParsedConsumer`, `GraduationEngine`, `EnrollmentCohortComparator`, `ResultService`, `RuleServiceClient`. Related: AD-006, AD-007, BR-ELIG-001. |
| ---                      | ---                                                                                                                                            |

## APPENDIX

### Prompts

1. "Tamam şimdi vision.md, system-requirements.md ve architecture-notebook.md dokümanlarını tamamladık. Geriye use-case dokümanı kaldı. Bu dokümanı oluşturmanı istiyorum."

Conversation link: Current Cursor session.
