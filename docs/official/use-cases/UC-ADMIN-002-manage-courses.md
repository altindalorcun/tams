| Use Case Number: | 002 | | |
| --- | --- | | | --- | --- |
| Use Case Code: | UC-ADMIN-002 | | |
| --- | --- | | | --- | --- |
| Use Case Name: | Manage Courses | | |
| --- | --- | | | --- | --- |

| Depicter:         | Agent | Last Modificatory:      | — |
| ----------------- | --- | ----------------------- | --- |
| Description Date: | 2026-06-30 | Last Modification Date: | — |
| ---               | --- | ---                     | --- |

| Actor(s):                | ADMIN |
| ------------------------ | ---------------------------------------------------------------------------------------------------------------------------------------------- |
| Short Definition:        | An Admin maintains the global course catalog with course codes, credits, and ECTS values used across all graduation rule definitions. |
| ---                      | ---                                                                                                                                            |
| Pre-condition:           | The actor is authenticated with role ADMIN. |
| ---                      | ---                                                                                                                                            |
| Post-condition:          | Course records in the global catalog (`courses` table) reflect the Admin's CRUD operations. Unique course codes are enforced. |
| ---                      | ---                                                                                                                                            |
| Priority:                | Must |
| ---                      | ---                                                                                                                                            |
| Frequency of Occurrence: | Low to medium — when curriculum changes or new courses are added to the catalog. |
| ---                      | ---                                                                                                                                            |
| Main Path:               | 1. The Admin navigates to `/admin/courses` (`CoursesPage.tsx`).<br><br>2. The frontend loads courses via `GET /api/v1/courses`.<br><br>3. The Admin creates a course by entering code, name, credit, and ECTS (`POST /api/v1/courses`).<br><br>4. rule-service validates and persists the course (FR-RULE-001).<br><br>5. The Admin updates a course via `PUT /api/v1/courses/{id}` or deletes via `DELETE /api/v1/courses/{id}`.<br><br>6. The UI refreshes the table and shows a success toast. |
| ---                      | ---                                                                                                                                            |
| Alternative Path:        | A.01 (alternative to step 3): Duplicate course code — HTTP 409; error displayed.<br><br>A.02 (alternative to step 5): Delete course referenced by category or department assignments — HTTP 409; deletion blocked.<br><br>A.03 (alternative to step 2): Unauthorized role — HTTP 403 from rule-service (FR-RULE-004).<br><br>A.04 (alternative to step 3): Invalid credit or ECTS (negative or null) — HTTP 400 validation error. |
| ---                      | ---                                                                                                                                            |
| Containing Use-Case(s)   | Prerequisite for UC-ADMIN-003 (category course pool assignments reference catalog courses). |
| ---                      | ---                                                                                                                                            |
| Special Requirements:    | FR-RULE-001, FR-RULE-004. Course code uniqueness at database level. shadcn/ui forms with react-hook-form + zod validation. |
| ---                      | ---                                                                                                                                            |
| Assumptions:             | Courses exist independently of departments; department and category pools reference catalog entries by ID. |
| ---                      | ---                                                                                                                                            |
| Note:                    | Controller: `CourseController`. Related: FR-RULE-001, UC-ADMIN-003. |
| ---                      | ---                                                                                                                                            |

## APPENDIX

### Prompts

1. "Tamam şimdi vision.md, system-requirements.md ve architecture-notebook.md dokümanlarını tamamladık. Geriye use-case dokümanı kaldı. Bu dokümanı oluşturmanı istiyorum."

Conversation link: Current Cursor session.
