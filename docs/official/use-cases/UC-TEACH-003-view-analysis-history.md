| Use Case Number: | 003 | | |
| --- | --- | | | --- | --- |
| Use Case Code: | UC-TEACH-003 | | |
| --- | --- | | | --- | --- |
| Use Case Name: | View Analysis History | | |
| --- | --- | | | --- | --- |

| Depicter:         | Agent | Last Modificatory:      | — |
| ----------------- | --- | ----------------------- | --- |
| Description Date: | 2026-06-30 | Last Modification Date: | — |
| ---               | --- | ---                     | --- |

| Actor(s):                | TEACHER |
| ------------------------ | ---------------------------------------------------------------------------------------------------------------------------------------------- |
| Short Definition:        | A Teacher browses a paginated, searchable list of previously analyzed students and opens individual result details. |
| ---                      | ---                                                                                                                                            |
| Pre-condition:           | The Teacher is authenticated with role TEACHER. At least one analysis result exists that was uploaded by this Teacher. |
| ---                      | ---                                                                                                                                            |
| Post-condition:          | The Teacher sees a filtered page of analysis summaries sorted by creation date. Selecting an entry leads to UC-TEACH-002 for full detail. |
| ---                      | ---                                                                                                                                            |
| Priority:                | Should |
| ---                      | ---                                                                                                                                            |
| Frequency of Occurrence: | Several times per week during graduation periods; lower during mid-semester. |
| ---                      | ---                                                                                                                                            |
| Main Path:               | 1. The Teacher navigates to `/teacher/history` (`StudentHistoryPage.tsx`).<br><br>2. The frontend requests `GET /api/v1/results?page=0&size=20&sort=createdAt,desc` (FR-ANAL-005).<br><br>3. analysis-service returns a page of summaries filtered to the authenticated teacher's uploads (`teacherId` match).<br><br>4. The Teacher optionally enters a partial student number in the search field.<br><br>5. The frontend re-queries with `GET /api/v1/results?studentNumber={partial}&page=...`.<br><br>6. The Teacher clicks a row to view full detail (UC-TEACH-002 via `GET /api/v1/results/{id}`).<br><br>7. The Teacher navigates pages using pagination controls (default page size 20). |
| ---                      | ---                                                                                                                                            |
| Alternative Path:        | A.01 (alternative to step 2): No results yet — empty state with centred message displayed.<br><br>A.02 (alternative to step 5): Search returns no matches — empty table with filter active.<br><br>A.03 (alternative to step 2): Non-TEACHER role — HTTP 403.<br><br>A.04 (alternative to step 6): Result deleted or not found — HTTP 404 on detail fetch. |
| ---                      | ---                                                                                                                                            |
| Containing Use-Case(s)   | Extends to UC-TEACH-002 for result detail. Depends on UC-TEACH-001 (prior uploads). |
| ---                      | ---                                                                                                                                            |
| Special Requirements:    | FR-ANAL-005 (paginated searchable list). Default sort: newest first. Teacher sees only own uploads. shadcn Table with pagination component. |
| ---                      | ---                                                                                                                                            |
| Assumptions:             | Summaries include student number, eligibility status, department name, and timestamps. Only COMPLETED and FAILED jobs appear with meaningful summary data. |
| ---                      | ---                                                                                                                                            |
| Note:                    | Route: `/teacher/history`. Endpoint: `ResultController.listResults`. Related: FR-ANAL-005, Vision "Student History Tracking". |
| ---                      | ---                                                                                                                                            |

## APPENDIX

### Prompts

1. "Tamam şimdi vision.md, system-requirements.md ve architecture-notebook.md dokümanlarını tamamladık. Geriye use-case dokümanı kaldı. Bu dokümanı oluşturmanı istiyorum."

Conversation link: Current Cursor session.
