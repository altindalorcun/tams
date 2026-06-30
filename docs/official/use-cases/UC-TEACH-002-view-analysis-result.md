| Use Case Number: | 002 | | |
| --- | --- | | | --- | --- |
| Use Case Code: | UC-TEACH-002 | | |
| --- | --- | | | --- | --- |
| Use Case Name: | View Analysis Result | | |
| --- | --- | | | --- | --- |

| Depicter:         | Agent | Last Modificatory:      | — |
| ----------------- | --- | ----------------------- | --- |
| Description Date: | 2026-06-30 | Last Modification Date: | — |
| ---               | --- | ---                     | --- |

| Actor(s):                | TEACHER |
| ------------------------ | ---------------------------------------------------------------------------------------------------------------------------------------------- |
| Short Definition:        | A Teacher views the graduation eligibility outcome for an analyzed transcript, including per-category progress, global checks, and deficiency details. |
| ---                      | ---                                                                                                                                            |
| Pre-condition:           | A COMPLETED analysis result exists for a job the Teacher uploaded (UC-TEACH-001) or appears in their history (UC-TEACH-003). The Teacher is authenticated with role TEACHER. |
| ---                      | ---                                                                                                                                            |
| Post-condition:          | The Teacher sees eligibility status (eligible / not eligible), GPA, category breakdowns, missing mandatory courses, and global check results. No modification of data occurs. |
| ---                      | ---                                                                                                                                            |
| Priority:                | Must |
| ---                      | ---                                                                                                                                            |
| Frequency of Occurrence: | After each upload and when reviewing historical analyses; several times per day during graduation periods. |
| ---                      | ---                                                                                                                                            |
| Main Path:               | 1. **Immediate post-upload path:** After UC-TEACH-001 completes, `TeacherPage` calls `GET /api/v1/results/by-job/{jobId}` and opens a dialog with `ResultCard`.<br><br>2. **History detail path:** From UC-TEACH-003, the Teacher selects a row and the frontend calls `GET /api/v1/results/{id}`.<br><br>3. analysis-service returns `AnalysisResultDetailResponse` with eligibility flag, totals, GPA, category results, global checks, and deficiencies (FR-ANAL-004).<br><br>4. `ResultCard` renders eligibility badge, category progress, unsatisfied categories with missing credits/courses, and global check status in Turkish.<br><br>5. The Teacher dismisses the dialog or navigates away; data remains persisted for future access. |
| ---                      | ---                                                                                                                                            |
| Alternative Path:        | A.01 (alternative to step 1): Job still PENDING — skeleton loader shown until result available.<br><br>A.02 (alternative to step 2): Teacher requests another teacher's result by ID — HTTP 403 ownership check (`teacherId` must match) (FR-AUTH-005 intent via ResultQueryService).<br><br>A.03 (alternative to step 1): Result not found for jobId — HTTP 404; error state in dialog.<br><br>A.04 (alternative to step 3): Job status FAILED — no result detail; error indicated on upload page instead. |
| ---                      | ---                                                                                                                                            |
| Containing Use-Case(s)   | Follows UC-TEACH-001. Detail view also accessible from UC-TEACH-003. Evaluation logic in UC-SYS-002. |
| ---                      | ---                                                                                                                                            |
| Special Requirements:    | FR-ANAL-004 (masked persisted results), FR-ANAL-006 (teacher views own uploads). Transparent deficiency display per Vision "Transparent Result Display". Turkish UI strings. |
| ---                      | ---                                                                                                                                            |
| Assumptions:             | Result detail includes cohort-skipped categories flagged via `cohortSkipped`. Student PII beyond masked student number is not displayed. |
| ---                      | ---                                                                                                                                            |
| Note:                    | Endpoints: `GET /api/v1/results/by-job/{jobId}`, `GET /api/v1/results/{id}`. Component: `ResultCard.tsx`. Related: FR-ANAL-004, BR-ELIG-001. |
| ---                      | ---                                                                                                                                            |

## APPENDIX

### Prompts

1. "Tamam şimdi vision.md, system-requirements.md ve architecture-notebook.md dokümanlarını tamamladık. Geriye use-case dokümanı kaldı. Bu dokümanı oluşturmanı istiyorum."

Conversation link: Current Cursor session.
