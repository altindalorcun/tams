| Use Case Number: | 003 | | |
| --- | --- | | | --- | --- |
| Use Case Code: | UC-ADMIN-003 | | |
| --- | --- | | | --- | --- |
| Use Case Name: | Manage Graduation Categories | | |
| --- | --- | | | --- | --- |

| Depicter:         | Agent | Last Modificatory:      | — |
| ----------------- | --- | ----------------------- | --- |
| Description Date: | 2026-06-30 | Last Modification Date: | — |
| ---               | --- | ---                     | --- |

| Actor(s):                | ADMIN |
| ------------------------ | ---------------------------------------------------------------------------------------------------------------------------------------------- |
| Short Definition:        | An Admin defines department-scoped graduation categories with credit/ECTS/course-count thresholds, course pool assignments (including mandatory flags and cohort bounds), conditional thresholds, and prefix sub-limits. |
| ---                      | ---                                                                                                                                            |
| Pre-condition:           | The actor is authenticated with role ADMIN. At least one department (UC-ADMIN-001) and courses in the global catalog (UC-ADMIN-002) exist. |
| ---                      | ---                                                                                                                                            |
| Post-condition:          | Graduation category definitions and their course assignments are persisted in postgres-rules. Invalid cohort boundary combinations are rejected. The rule set is available to analysis-service via `GET /internal/rules/{departmentId}` (FR-RULE-003). |
| ---                      | ---                                                                                                                                            |
| Priority:                | Must |
| ---                      | ---                                                                                                                                            |
| Frequency of Occurrence: | Medium during initial rule setup; occasional updates when curriculum or cohort rules change. |
| ---                      | ---                                                                                                                                            |
| Main Path:               | 1. The Admin navigates to `/admin/graduation-categories` (`CategoriesPage.tsx` / `CategoriesTab.tsx`).<br><br>2. The Admin selects a department and loads categories via `GET /api/v1/departments/{deptId}/categories`.<br><br>3. The Admin creates a category with name, `min_credit`, `min_ects`, `min_course_count`, optional category-level year bounds (`applies_from_year`, `applies_to_year`), and optional conditional fields (`POST /api/v1/departments/{deptId}/categories`).<br><br>4. The Admin assigns courses to the category pool via `POST /api/v1/categories/{catId}/courses` with `is_mandatory` and course-level cohort bounds (`appliesFromYear/Term`, `appliesToYear/Term`) using `CohortBoundaryFields.tsx` (FR-RULE-005).<br><br>5. The Admin updates or removes course assignments via `PUT` or `DELETE /api/v1/categories/{catId}/courses/{courseId}`.<br><br>6. The Admin optionally adds prefix sub-limits via `POST /api/v1/categories/{catId}/prefix-limits` (BR-GRAD-009).<br><br>7. The Admin updates or deletes categories via `PUT` or `DELETE /api/v1/departments/{deptId}/categories/{catId}`. |
| ---                      | ---                                                                                                                                            |
| Alternative Path:        | A.01 (alternative to step 4): Invalid cohort bounds (start not strictly before end, invalid term) — HTTP 409 from `EnrollmentCohortBoundaryValidator` (FR-RULE-006).<br><br>A.02 (alternative to step 4): Course not in department pool — HTTP 404 or 409.<br><br>A.03 (alternative to step 3): Missing required threshold fields — HTTP 400 validation error.<br><br>A.04 (alternative to step 7): Delete category with active references — HTTP 409; operation blocked with error message. |
| ---                      | ---                                                                                                                                            |
| Containing Use-Case(s)   | Depends on UC-ADMIN-001 and UC-ADMIN-002. Rule set consumed by UC-SYS-002 (graduation evaluation). |
| ---                      | ---                                                                                                                                            |
| Special Requirements:    | FR-RULE-001, FR-RULE-003, FR-RULE-005, FR-RULE-006, FR-RULE-007. Business rules: BR-COHORT-005 (category-level skip), BR-COHORT-006–008 (course-level applicability), BR-GRAD-001–005, BR-GRAD-008–009. Architectural decision AD-007 (course-level cohort bounds with GUZ/BAHAR term granularity). |
| ---                      | ---                                                                                                                                            |
| Assumptions:             | Category names are flexible and department-scoped (not a fixed global enum). Term values are `GUZ` (fall) or `BAHAR` (spring). End bound on course assignments is exclusive (BR-COHORT-007). |
| ---                      | ---                                                                                                                                            |
| Note:                    | Most complex Admin use case. Internal API: `GET /internal/rules/{departmentId}`. Related: AD-006, AD-007, FR-RULE-005–007. |
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
