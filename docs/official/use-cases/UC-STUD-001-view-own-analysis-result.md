| Use Case Number: | 001 | | |
| --- | --- | | | --- | --- |
| Use Case Code: | UC-STUD-001 | | |
| --- | --- | | | --- | --- |
| Use Case Name: | View Own Analysis Result | | |
| --- | --- | | | --- | --- |

| Depicter:         | Agent | Last Modificatory:      | — |
| ----------------- | --- | ----------------------- | --- |
| Description Date: | 2026-06-30 | Last Modification Date: | — |
| ---               | --- | ---                     | --- |

| Actor(s):                | STUDENT |
| ------------------------ | ---------------------------------------------------------------------------------------------------------------------------------------------- |
| Short Definition:        | A Student views their most recent graduation analysis result in read-only mode, including eligibility status and category deficiencies. |
| ---                      | ---                                                                                                                                            |
| Pre-condition:           | The Student is authenticated (UC-AUTH-001) with role STUDENT. A Teacher has previously uploaded and analyzed the student's transcript (UC-TEACH-001 → UC-SYS-002). The student's JWT contains a valid `studentNumber` claim matching the persisted result. |
| ---                      | ---                                                                                                                                            |
| Post-condition:          | The Student sees their latest analysis outcome. No create, update, or delete operations are available. |
| ---                      | ---                                                                                                                                            |
| Priority:                | Must |
| ---                      | ---                                                                                                                                            |
| Frequency of Occurrence: | Occasional — when checking graduation status, often from mobile devices. |
| ---                      | ---                                                                                                                                            |
| Main Path:               | 1. After login, the Student is redirected to `/student/results` (`StudentResultPage.tsx`).<br><br>2. The frontend calls `GET /api/v1/results/me` with the Bearer access token (FR-ANAL-006).<br><br>3. analysis-service extracts `studentNumber` from the JWT claim and queries the latest result via `findFirstByStudentNumberOrderByCreatedAtDesc`.<br><br>4. The service returns full result detail (same shape as teacher detail, read-only).<br><br>5. The page renders eligibility badge, category breakdown, deficiencies, and global checks using responsive mobile-first layout.<br><br>6. The Student reviews the information; no further action is required. |
| ---                      | ---                                                                                                                                            |
| Alternative Path:        | A.01 (alternative to step 3): No analysis exists for this student number — HTTP 404; empty or informational state displayed.<br><br>A.02 (alternative to step 2): JWT missing `studentNumber` claim — HTTP 401 UnauthorizedException.<br><br>A.03 (alternative to step 2): STUDENT attempts `GET /api/v1/results/{id}` for another student's result — HTTP 403 ownership enforcement.<br><br>A.04 (alternative to step 2): Access token expired — redirect to `/login`. |
| ---                      | ---                                                                                                                                            |
| Containing Use-Case(s)   | Depends on UC-TEACH-001 and UC-SYS-002 producing a result. Student account created via UC-ADMIN-005. |
| ---                      | ---                                                                                                                                            |
| Special Requirements:    | FR-ANAL-006 (read-only own result). Fully responsive mobile-first UI (Vision, SRS Usability). Read-only — no upload capability. Turkish UI strings. |
| ---                      | ---                                                                                                                                            |
| Assumptions:             | Student cannot upload transcripts. Access is matched by `studentNumber` from JWT to persisted analysis result, not via teacher-student map API. If multiple analyses exist, only the most recent is shown. |
| ---                      | ---                                                                                                                                            |
| Note:                    | Route: `/student/results`. Endpoint: `ResultController.getMyResult`. Related: FR-ANAL-006, FR-AUTH-002, Vision Student stakeholder description. |
| ---                      | ---                                                                                                                                            |

## APPENDIX

### Traceability Table

| Section | Source | Author | Date |
| --- | --- | --- | --- |
| Use Case Definition | `docs/official/vision.md` (Stakeholder Descriptions — Student; User Environment); `docs/official/system-requirements.md` (FR-ANAL-006, FR-AUTH-005); `docs/official/architecture-notebook.md` (Architecturally Significant Requirements — teacher-student access) | Agent | 2026-07-01 |

### Prompts

1. "Tamam şimdi vision.md, system-requirements.md ve architecture-notebook.md dokümanlarını tamamladık. Geriye use-case dokümanı kaldı. Bu dokümanı oluşturmanı istiyorum."

2. "Tamam şimdi aynı şekilde vision dokümanının altındaki Traceability Table'ı doldur. Dokümaları hazırlama sıram, Vision, SRS, architectural notebook, use-case ve graphical user interface. Bunların tracebility table'larını güncelle"

Conversation link: Current Cursor session.
