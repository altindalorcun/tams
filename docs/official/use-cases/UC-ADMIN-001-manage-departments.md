| Use Case Number: | 001 | | |
| --- | --- | | | --- | --- |
| Use Case Code: | UC-ADMIN-001 | | |
| --- | --- | | | --- | --- |
| Use Case Name: | Manage Departments | | |
| --- | --- | | | --- | --- |

| Depicter:         | Agent | Last Modificatory:      | — |
| ----------------- | --- | ----------------------- | --- |
| Description Date: | 2026-06-30 | Last Modification Date: | — |
| ---               | --- | ---                     | --- |

| Actor(s):                | ADMIN |
| ------------------------ | ---------------------------------------------------------------------------------------------------------------------------------------------- |
| Short Definition:        | An Admin creates, views, updates, and deletes academic departments and manages department-level global graduation settings. |
| ---                      | ---                                                                                                                                            |
| Pre-condition:           | The actor is authenticated with role ADMIN (FR-AUTH-002). Graduation rule data may or may not already exist. |
| ---                      | ---                                                                                                                                            |
| Post-condition:          | Department records reflect the Admin's changes in postgres-rules. Global fields such as `min_total_ects` and `block_on_any_f_grade` are persisted when configured. |
| ---                      | ---                                                                                                                                            |
| Priority:                | Must |
| ---                      | ---                                                                                                                                            |
| Frequency of Occurrence: | Low — typically at system setup or when academic structure changes; occasional updates during rule revisions. |
| ---                      | ---                                                                                                                                            |
| Main Path:               | 1. The Admin navigates to `/admin/departments` (`DepartmentsPage.tsx`).<br><br>2. The frontend loads the department list via `GET /api/v1/departments`.<br><br>3. The Admin creates a department by submitting name and optional global thresholds (`POST /api/v1/departments`).<br><br>4. rule-service validates input and persists the department (FR-RULE-001).<br><br>5. The Admin optionally adds courses to the department pool via `POST /api/v1/departments/{id}/courses`.<br><br>6. The Admin updates a department via `PUT /api/v1/departments/{id}` or deletes via `DELETE /api/v1/departments/{id}`.<br><br>7. The UI refreshes the list and confirms success with a toast notification. |
| ---                      | ---                                                                                                                                            |
| Alternative Path:        | A.01 (alternative to step 2): Non-ADMIN role accesses route — ProtectedRoute redirects to `/forbidden`.<br><br>A.02 (alternative to step 3): Validation failure (blank name) — HTTP 400; inline form errors displayed.<br><br>A.03 (alternative to step 6): Delete department referenced by categories — HTTP 409 conflict; error message shown.<br><br>A.04 (alternative to step 2): rule-service unavailable — loading error state with user-friendly message. |
| ---                      | ---                                                                                                                                            |
| Containing Use-Case(s)   | Prerequisite for UC-ADMIN-003 (graduation categories are scoped to a department). Supports BR-GRAD-006 and BR-GRAD-007 via department global fields. |
| ---                      | ---                                                                                                                                            |
| Special Requirements:    | FR-RULE-001, FR-RULE-004 (ADMIN role required on all `/api/v1/**` rule-service endpoints). CRUD response within 2 seconds under normal load. Turkish UI labels. |
| ---                      | ---                                                                                                                                            |
| Assumptions:             | MVP supports a single-university scope; departments represent academic units (e.g., BBM). Department course pool is distinct from the global course catalog (UC-ADMIN-002). |
| ---                      | ---                                                                                                                                            |
| Note:                    | Global department checks evaluated at runtime: BR-GRAD-006 (min total ECTS), BR-GRAD-007 (fail-grade block). Related: FR-RULE-001, AD-004, AD-006. |
| ---                      | ---                                                                                                                                            |

## APPENDIX

### Traceability Table

| Section | Source | Author | Date |
| --- | --- | --- | --- |
| Use Case Definition | `docs/official/vision.md` (Needs and Features — Rule Management); `docs/official/system-requirements.md` (FR-RULE-001–007; Business Rules); `docs/official/architecture-notebook.md` (AD-006, AD-007; Key Abstractions) | Agent | 2026-07-01 |

### Prompts

1. "Tamam şimdi vision.md, system-requirements.md ve architecture-notebook.md dokümanlarını tamamladık. Geriye use-case dokümanı kaldı. Bu dokümanı oluşturmanı istiyorum."

2. "Tamam şimdi aynı şekilde vision dokümanının altındaki Traceability Table'ı doldur. Dokümaları hazırlama sıram, Vision, SRS, architectural notebook, use-case ve graphical user interface. Bunların tracebility table'larını güncelle"

Conversation link: Current Cursor session.
