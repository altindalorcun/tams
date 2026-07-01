| Use Case Number: | 004 | | |
| --- | --- | | | --- | --- |
| Use Case Code: | UC-ADMIN-004 | | |
| --- | --- | | | --- | --- |
| Use Case Name: | Manage Curriculum Equivalence Rules | | |
| --- | --- | | | --- | --- |

| Depicter:         | Agent | Last Modificatory:      | — |
| ----------------- | --- | ----------------------- | --- |
| Description Date: | 2026-06-30 | Last Modification Date: | — |
| ---               | --- | ---                     | --- |

| Actor(s):                | ADMIN |
| ------------------------ | ---------------------------------------------------------------------------------------------------------------------------------------------- |
| Short Definition:        | An Admin defines rules that map legacy or alternate course codes to current curriculum course codes for graduation evaluation. |
| ---                      | ---                                                                                                                                            |
| Pre-condition:           | The actor is authenticated with role ADMIN. Target department exists (UC-ADMIN-001). Source and target course codes refer to valid catalog entries or known codes. |
| ---                      | ---                                                                                                                                            |
| Post-condition:          | Curriculum equivalence rules are persisted per department. During analysis, passed source courses expand to target codes per BR-EQ-001 (fixpoint resolution in GraduationEngine). |
| ---                      | ---                                                                                                                                            |
| Priority:                | Must |
| ---                      | ---                                                                                                                                            |
| Frequency of Occurrence: | Low — when curriculum revisions introduce code changes or legacy mappings. |
| ---                      | ---                                                                                                                                            |
| Main Path:               | 1. The Admin navigates to `/admin/curriculum-equivalence-rules` (`CurriculumEquivalenceRulesPage.tsx`).<br><br>2. The Admin selects a department and loads rules via `GET /api/v1/departments/{departmentId}/curriculum-equivalence-rules`.<br><br>3. The Admin creates a rule mapping source course code A to target course code B (`POST /api/v1/departments/{departmentId}/curriculum-equivalence-rules`) (FR-RULE-002).<br><br>4. rule-service validates and persists the equivalence rule.<br><br>5. The Admin deletes an obsolete rule via `DELETE /api/v1/departments/{departmentId}/curriculum-equivalence-rules/{id}`.<br><br>6. The UI refreshes the rule list. |
| ---                      | ---                                                                                                                                            |
| Alternative Path:        | A.01 (alternative to step 3): Duplicate mapping for the same source code — HTTP 409.<br><br>A.02 (alternative to step 3): Invalid or empty course codes — HTTP 400 validation error.<br><br>A.03 (alternative to step 2): Non-ADMIN access — HTTP 403 (FR-RULE-004).<br><br>A.04 (alternative to step 5): Rule not found — HTTP 404. |
| ---                      | ---                                                                                                                                            |
| Containing Use-Case(s)   | Rules applied during UC-SYS-002 via `CurriculumEquivalenceExpander`. Depends on UC-ADMIN-001. |
| ---                      | ---                                                                                                                                            |
| Special Requirements:    | FR-RULE-002, FR-RULE-004. Business rule BR-EQ-001 (iterative equivalence chain resolution). |
| ---                      | ---                                                                                                                                            |
| Assumptions:             | Equivalence is department-scoped. Chains (A→B→C) resolve to fixpoint at evaluation time. |
| ---                      | ---                                                                                                                                            |
| Note:                    | Controller: `CurriculumEquivalenceRuleController`. Related: BR-EQ-001, AD-006, FR-RULE-002. |
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
