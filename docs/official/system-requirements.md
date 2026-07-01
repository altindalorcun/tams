**TAMS — Transcript Analysis and Graduation Control System**

**System-Wide Requirements Specification**

# Introduction

This document defines system-wide functional and non-functional requirements for TAMS — requirements that are not expressed as individual use cases. It serves as the technical complement to the product vision and provides verifiable criteria for development and acceptance testing.

## Purpose

To specify cross-cutting capabilities (authentication, infrastructure, PII handling, interfaces, and business rules) that apply across all user roles and microservices in Release 1 (MVP).

## Scope

The system covers:

- User authentication and role-based authorization (Admin, Teacher, Student)
- Graduation rule management (departments, courses, categories, category-course cohort bounds, curriculum equivalence)
- PDF transcript upload, asynchronous parsing, and graduation eligibility evaluation
- Masked result persistence and role-appropriate result retrieval
- Operational infrastructure (health checks, rate limiting, container deployment)

Release 2 scope (LDAP / Active Directory integration) is documented as a future requirement only.

## Reference Documents

| Document | Location |
| --- | --- |
| Vision | [`docs/official/vision.md`](../official/vision.md) |
| Architecture | [`docs/architecture.md`](../architecture.md) |
| Graduation Rules V2 | [`docs/graduation-rules-v2-architecture.md`](../graduation-rules-v2-architecture.md) |
| Category-Course Cohort Bounds | [`docs/category-course-cohort-bounds.md`](../category-course-cohort-bounds.md) |
| Production Deployment | [`docs/production-deployment.md`](../production-deployment.md) |

## Glossary

| Term | Definition |
| --- | --- |
| PII | Personally identifiable information (e.g., national identity number); masked or excluded from all persistent storage |
| ECTS | European Credit Transfer System credit unit used in graduation threshold calculations |
| Graduation Category | A department-scoped requirement group with credit, ECTS, and course-count thresholds plus an associated course pool |
| Curriculum Equivalence | A rule mapping a legacy or alternate course code to a current curriculum course code for evaluation purposes |
| Enrollment Cohort | A student's intake point derived from transcript `registration_date`: calendar year plus term (`GUZ` for months 09–12, `BAHAR` for months 01–08) |
| Category-Level Cohort Bounds | Year-only bounds on a graduation category (`applies_from_year` / `applies_to_year`); when excluded, the entire category is skipped |
| Course-Level Cohort Bounds | Year-and-term bounds on a category-course assignment (`appliesFromYear/Term`, `appliesToYear/Term`); when excluded, that assignment is ignored entirely |
| Enrollment Term | `GUZ` (fall intake) or `BAHAR` (spring intake); within the same calendar year, `BAHAR` precedes `GUZ` |

# System-Wide Functional Requirements

The following requirements are system-wide and not tied to a single use case. Each requirement is assigned a unique `FR-*` identifier for traceability.

## Authentication and Authorization

| ID | Requirement | Source |
| --- | --- | --- |
| FR-AUTH-001 | The system shall issue JWT access tokens and refresh tokens on successful login and support refresh token rotation | `services/auth-service/.../AuthController.java` |
| FR-AUTH-002 | The system shall enforce role-based access control with exactly three roles: ADMIN, TEACHER, and STUDENT | `services/auth-service/.../Role.java`, `frontend/src/router/AppRouter.tsx` |
| FR-AUTH-003 | The system shall require users flagged for password change to complete a mandatory password change before accessing the main application shell | `frontend/src/features/auth/ChangePasswordPage.tsx`, route `/change-password` |
| FR-AUTH-004 | The api-gateway shall validate JWT tokens on every inbound request (except public auth paths) using a shared secret | `services/api-gateway/.../JwtAuthenticationFilter.java` |
| FR-AUTH-005 | The system shall enforce teacher-student relationships so that a teacher may only access results for students linked to them, and a student may only access their own result | `services/auth-service/.../InternalController.java` |
| FR-AUTH-006 | The system shall support LDAP / Active Directory authentication *(Release 2 — TBD)* | `docs/official/vision.md` |

## System Infrastructure

| ID | Requirement | Source |
| --- | --- | --- |
| FR-SYS-001 | Every service shall expose a health check endpoint: `/actuator/health` (Spring Boot) or `/health` (FastAPI) | All services |
| FR-SYS-002 | The api-gateway shall enforce IP-based rate limiting with configurable requests-per-second (default 10) and burst capacity (default 20), returning HTTP 429 when exceeded | `services/api-gateway/.../RateLimitFilter.java`, `application.yml` |
| FR-SYS-003 | Each Spring Boot service shall manage its database schema through Flyway versioned migrations | `services/*/src/main/resources/db/migration/` |
| FR-SYS-004 | All services shall emit structured JSON logs and shall not log raw PII (national identity numbers, full transcript content) | `logstash-logback-encoder`, `services/parser-service/tests/test_pii_masker.py` |
| FR-SYS-005 | The api-gateway shall enforce a configurable CORS policy for allowed origins | `services/api-gateway/.../SecurityConfig.java` |
| FR-SYS-006 | All backend services shall expose OpenAPI 3.0 documentation (springdoc on Spring Boot, `/docs` on FastAPI) | springdoc config, `services/parser-service` |

## PII and Data Handling

| ID | Requirement | Source |
| --- | --- | --- |
| FR-PII-001 | PDF transcript bytes shall be processed entirely in memory and shall never be written to physical server disk | `docs/architecture.md` §4.4 |
| FR-PII-002 | National identity numbers shall be excluded from Kafka messages and all downstream persistent storage; student numbers may be retained in masked form | `services/parser-service/src/pii/pii_masker.py`, `docs/architecture.md` §4.4 |
| FR-PII-003 | Kafka topic retention shall limit exposure: `transcript.raw` 5 minutes, `transcript.parsed` 1 hour | `infrastructure/docker-compose.infra.yml`, `docs/todo.md` |

## Rule Management

| ID | Requirement | Source |
| --- | --- | --- |
| FR-RULE-001 | Admins shall perform CRUD operations on departments, the global course catalog, and graduation categories | `services/rule-service` controllers |
| FR-RULE-002 | Admins shall manage curriculum equivalence rules that map legacy course codes to current codes | `frontend/src/features/admin/CurriculumEquivalenceRulesPage.tsx`, rule-service |
| FR-RULE-003 | The rule-service shall expose an internal API returning the full rule set for a department to the analysis-service | `GET /internal/rules/{departmentId}` |
| FR-RULE-004 | All public rule-service endpoints under `/api/v1/**` shall require the ADMIN role | `services/rule-service` SecurityConfig |
| FR-RULE-005 | Admins shall define and update enrollment cohort bounds (year + `GUZ`/`BAHAR` term) on category-course assignments via add and update endpoints | `POST/PUT /api/v1/categories/{catId}/courses`, `CategoryService.java`, `CohortBoundaryFields.tsx` |
| FR-RULE-006 | The rule-service shall reject invalid cohort boundary combinations (invalid term values or start not strictly before end) with HTTP 409 | `EnrollmentCohortBoundaryValidator.java` |
| FR-RULE-007 | The graduation engine shall derive the student's enrollment cohort from parsed transcript `registration_date` metadata when evaluating applicability bounds | `EnrollmentYearParser.java`, `GraduationEngine.java` |

## Analysis

| ID | Requirement | Source |
| --- | --- | --- |
| FR-ANAL-001 | Teachers shall upload PDF transcripts via multipart form data with a maximum file size of 10 MB enforced at the gateway | `services/api-gateway/src/main/resources/application.yml` |
| FR-ANAL-002 | Transcript parsing shall be asynchronous via Kafka topics (`transcript.raw` → parser-service → `transcript.parsed`) | `docs/architecture.md` §4.1 |
| FR-ANAL-003 | The analysis-service shall evaluate parsed course data against the rule set fetched from rule-service using the graduation engine | `services/analysis-service/.../GraduationEngine.java` |
| FR-ANAL-004 | The system shall persist masked analysis results, per-category breakdowns, and deficiency records in the analysis database | `services/analysis-service` domain entities |
| FR-ANAL-005 | Teachers shall view a paginated, searchable list of previously analyzed students | `frontend/src/features/teacher/StudentHistoryPage.tsx`, `/teacher/history` |
| FR-ANAL-006 | Students shall have read-only access to their own most recent analysis result | `frontend/src/features/student/StudentResultPage.tsx`, `/student/results` |

# System Qualities

## Usability

- All user-visible interface text shall be in Turkish; all code, comments, and formal documentation shall be in English.
- The frontend shall follow the shadcn/ui design system: slate neutral palette, rose-700 primary accent, Inter typography, and semantic color tokens for success, warning, and error states.
- The application shall provide role-specific navigation shells: Admin (`/admin/*`), Teacher (`/teacher/*`), and Student (`/student/*`).
- Every data-fetching view shall handle loading (skeleton loaders or spinners), error (user-friendly messages), and success states.
- The student result page shall be fully responsive and mobile-first, as students predominantly access the system from smartphones and tablets.

## Reliability

- Kafka consumers shall retry failed message processing; parser failures shall be logged without including the message payload to prevent PII leakage.
- All services shall support liveness and readiness probes for Kubernetes and Docker Compose deployments.
- Auth, rule, and analysis data shall be persisted in three separate PostgreSQL instances for fault isolation.
- When PDF parsing fails, the analysis job status shall transition to FAILED and the teacher shall receive a visible error indication.

## Performance

- Synchronous CRUD API endpoints shall respond within 2 seconds under normal load *(acceptance target; formal benchmarking TBD)*.
- PDF parsing shall be offloaded asynchronously; end-to-end analysis (upload to result display) shall typically complete within seconds *(formal SLA TBD)*.
- Rate limiting at the api-gateway shall protect downstream services during graduation-period traffic spikes.
- Horizontal Pod Autoscalers shall be configured for parser-service and analysis-service in Kubernetes deployments.

## Supportability

- The codebase shall be organized as a monorepo with a shared Maven parent POM for Java services.
- All sensitive configuration (database URLs, JWT secrets, API keys) shall be supplied via environment variables documented in `.env.example`.
- Every service Dockerfile shall use a multi-stage build and run the final image as a non-root user.
- Deployment shall be supported via Docker Compose (local/single-server) and Kubernetes manifests under `infrastructure/k8s/`.

# System Interfaces

## User Interfaces

### Look & Feel

The interface shall convey a professional academic tool aesthetic. The color scheme uses a slate neutral base with rose-700 as the sole accent color for primary actions, eligibility badges, and active navigation. Success, warning, and destructive states use green, amber, and red semantic tokens respectively. Decorative color use is prohibited.

### Layout and Navigation Requirements

| Area | Routes | Required Role |
| --- | --- | --- |
| Login | `/login` | None (public) |
| Mandatory password change | `/change-password` | Authenticated (any role) |
| Admin dashboard | `/admin` | ADMIN |
| Department management | `/admin/departments` | ADMIN |
| Course catalog | `/admin/courses` | ADMIN |
| Graduation categories | `/admin/graduation-categories` | ADMIN |
| Curriculum equivalence rules | `/admin/curriculum-equivalence-rules` | ADMIN |
| User management | `/admin/users` | ADMIN |
| Teacher upload and results | `/teacher` | TEACHER |
| Teacher student history | `/teacher/history` | TEACHER |
| Student result view | `/student/results` | STUDENT |
| Unauthorized access | `/forbidden` | Any (redirect target) |

Login and mandatory password change shall render outside the main application shell. All other authenticated routes shall render inside the AppShell layout with role-filtered sidebar navigation.

### Consistency

All forms shall use shadcn/ui components wired to react-hook-form with zod validation. Navigation controls, input placement, button styles, and domain terminology shall remain consistent across Admin, Teacher, and Student areas.

### User Personalization & Customization Requirements

Displayed content shall be automatically filtered by the authenticated user's role and, where applicable, the teacher-student relationship. Users shall not be able to customize themes or layout in Release 1.

## Interfaces to External Systems or Devices

### Software Interfaces

| System | Protocol | Address / Topic | Purpose |
| --- | --- | --- | --- |
| postgres-auth | JDBC / PostgreSQL | Port 5432 | User accounts, refresh tokens, teacher-student map |
| postgres-rules | JDBC / PostgreSQL | Port 5432 | Departments, courses, categories, equivalence rules |
| postgres-analysis | JDBC / PostgreSQL | Port 5432 | Analysis jobs, results, deficiencies |
| Kafka (KRaft) | Kafka protocol | Port 9092 | Async transcript pipeline |
| transcript.raw | Kafka topic | — | Raw PDF bytes from analysis-service to parser-service |
| transcript.parsed | Kafka topic | — | PII-free parsed course data from parser-service to analysis-service |
| parser-service | HTTP + Kafka | Port 8000 | PDF parsing and PII masking |
| auth-service (internal) | HTTP | Port 8081 | Token validation (`POST /internal/auth/validate`) |
| rule-service (internal) | HTTP | Port 8082 | Rule set retrieval (`GET /internal/rules/{departmentId}`) |

### Hardware Interfaces

None. The client is a standards-compliant web browser; server components run in Linux containers.

### Communications Interfaces

- Client-to-server communication shall use HTTPS/TLS in production deployments.
- Inter-service communication shall occur over the Docker Compose or Kubernetes internal network.
- The api-gateway (port 8080) is the single external entry point for all client API requests.

# Business Rules

Business rules define graduation evaluation logic in technology-independent if-then form. Each rule is assigned a `BR-*` identifier. Implementation references: `GraduationEngine.java`, `EnrollmentCohortComparator.java`. See also [`docs/category-course-cohort-bounds.md`](../category-course-cohort-bounds.md).

## Enrollment Cohort Rules

### BR-COHORT-001 — Enrollment Cohort Derivation

If the transcript contains a `registration_date` in `DD.MM.YYYY` format, then derive the enrollment year as the calendar year of that date and the enrollment term as `GUZ` when the month is 09–12, or `BAHAR` when the month is 01–08.

### BR-COHORT-002 — Missing Registration Date

If the registration date is absent and the enrollment year cannot be derived, then ignore all course-level cohort bounds and treat every category-course assignment as applicable *(legacy safe default)*.

### BR-COHORT-003 — Term Ordering

If comparing two cohorts within the same calendar year, then `BAHAR` (spring intake) precedes `GUZ` (fall intake).

### BR-COHORT-004 — Null Term Default

If a stored cohort bound has a year but a null term, then treat the term as `GUZ` at evaluation time.

### BR-COHORT-005 — Category-Level Skip

If the student's enrollment year falls outside a category's year-only bounds (`applies_from_year` / `applies_to_year`), then skip evaluation of the entire category and mark it as `cohortSkipped` *(evaluated before per-course logic)*.

### BR-COHORT-006 — Course-Level Applicability (Inclusive Start)

If the student's enrollment cohort is before a course assignment's start bound (`appliesFromYear` + `appliesFromTerm`), then ignore that assignment entirely — it does not count toward thresholds and does not create a mandatory requirement.

### BR-COHORT-007 — Course-Level Applicability (Exclusive End)

If the student's enrollment cohort is at or after a course assignment's end bound (`appliesToYear` + `appliesToTerm`), then ignore that assignment entirely. For example, an end bound of `2017 GUZ` excludes students enrolled in 2017 Fall or later, while 2017 Spring enrollees remain eligible.

### BR-COHORT-008 — In-Range Course Behavior

If the student's enrollment cohort falls within a course assignment's applicability window, then apply the standard rules: passed courses count toward category thresholds; if `is_mandatory = true` and the course is not passed, mark the category as unsatisfied; if `is_mandatory = false` and not passed, the assignment has no effect.

## Graduation Category Rules

### BR-GRAD-001 — Minimum Credit Threshold

If the sum of credits from passed courses in a category's course pool is below the category's configured `min_credit`, then mark the category as unsatisfied.

### BR-GRAD-002 — Minimum ECTS Threshold

If the sum of ECTS from passed courses in a category's course pool is below the category's configured `min_ects`, then mark the category as unsatisfied.

### BR-GRAD-003 — Minimum Course Count Threshold

If the number of passed courses in a category's course pool is below the category's configured `min_course_count`, then mark the category as unsatisfied.

### BR-GRAD-004 — Mandatory Course Requirement

If a course in the category pool is flagged `is_mandatory = true`, the student's enrollment cohort falls within the course's applicability range per **BR-COHORT-006** through **BR-COHORT-008**, and the student has not passed that course, then mark the category as unsatisfied.

### BR-GRAD-005 — Category-Level Cohort Skip

See **BR-COHORT-005**. Category-level bounds are year-only and do not support term granularity; course-level term bounds are defined separately under Enrollment Cohort Rules.

## Global Department Rules

### BR-GRAD-006 — Minimum Total ECTS

If the department's `min_total_ects` is configured and the student's total passed ECTS across all courses is below that value, then the student fails the global ECTS check and is not eligible to graduate.

### BR-GRAD-007 — Fail Grade Block

If the department's `block_on_any_f_grade` is true and the student's transcript contains any course with a failing grade, then the student fails the global fail-grade check and is not eligible to graduate.

## Conditional and Prefix Rules

### BR-GRAD-008 — Conditional Threshold Substitution

If the student has passed at least one course whose code appears in the category's `conditionCourseCodes` list, then use `minCourseCountIfMet` and `minEctsIfMet` (when non-null) instead of the base `min_course_count` and `min_ects` thresholds.

### BR-GRAD-009 — Prefix Sub-Limit

If prefix limits are configured for a category, then count at most `maxCount` passed courses per course-code prefix toward the category's earned credit, ECTS, and course-count totals.

## Course Matching Rules

### BR-EQ-001 — Curriculum Equivalence Expansion

If a curriculum equivalence rule maps a passed course code A to a target course code B, then treat B as passed for all graduation evaluations. Equivalence chains shall be resolved iteratively until a fixpoint is reached.

## Eligibility Evaluation Rules

### BR-ELIG-001 — Overall Graduation Eligibility

A student is eligible to graduate if and only if all applicable global department checks pass and every non-skipped graduation category is satisfied.

# System Constraints

- **Languages and runtimes:** Java 21, Spring Boot 3.3.5, Python 3.12, Node.js 22.
- **Naming conventions:** kebab-case REST endpoints, dot-separated lowercase Kafka topics, snake_case database tables and columns.
- **Secrets:** No passwords, API keys, or JWT secrets shall be hardcoded; all sensitive values come from environment variables.
- **Containers:** Every Dockerfile shall use a multi-stage build and run as a non-root user.
- **Upload limit:** PDF transcript uploads are limited to 10 MB at the api-gateway.
- **Authentication (MVP):** Standalone JWT-based auth; LDAP / Active Directory integration is deferred to Release 2.

# System Compliance

## Licensing Requirements

TAMS is built on open-source components including Spring Boot, React, FastAPI, and shadcn/ui. All dependency license obligations shall be respected in distribution and deployment.

## Legal, Copyright, and Other Notices

The system processes university student academic data. PII masking and the prohibition on permanent PDF storage are designed to align with data privacy principles consistent with KVKK and GDPR. Formal legal review and institutional data-processing agreements are TBD.

## Applicable Standards

| Standard | Application |
| --- | --- |
| OpenAPI 3.0 | API documentation on all backend services |
| RFC 7519 (JWT) | Access and refresh token format |
| HTTPS/TLS | Encryption of data in transit between client and server |

# System Documentation

This section defines the documentation deliverables required for TAMS before and during Release 1 development. The formal document set is maintained under `docs/official/`.

| Document | Status | Audience | Responsibility |
| --- | --- | --- | --- |
| Vision Document — [`docs/official/vision.md`](vision.md) | Complete | Stakeholders, reviewers | Development team |
| System Requirements Specification — [`docs/official/system-requirements.md`](system-requirements.md) | Complete | Developers, QA, reviewers | Development team |
| Architecture Notebook — [`docs/official/architecture-notebook.md`](architecture-notebook.md) | Complete | Developers, architects | Development team |
| Use Case Definitions — [`docs/official/use-cases/`](use-cases/) | Complete | Developers, QA | Development team |
| Graphical User Interface Design — [`docs/official/graphical-user-interface-design.md`](graphical-user-interface-design.md) | Complete | Developers, UX reviewers | Development team |
| OpenAPI / Swagger API reference (all backend services) | Planned — Release 1 | Developers, integrators | Development team |
| User Manual for Admins and Teachers | TBD — Release 1 | End users (Admin, Teacher) | TBD |
| Deployment and operations guide | Planned — Release 1 | DevOps / IT | Development team |

# Traceability Table

| Section | Source | Author | Date |
| --- | --- | --- | --- |
| Introduction | `docs/official/vision.md` (Introduction, Scope); SRS template | Agent | 2026-07-01 |
| System-Wide Functional Requirements | `docs/official/vision.md` (Needs and Features, Other Product Requirements) | Agent | 2026-07-01 |
| System Qualities | `docs/official/vision.md` (User Environment, Other Product Requirements) | Agent | 2026-07-01 |
| System Interfaces | `docs/official/vision.md` (Stakeholder Descriptions, Product Overview) | Agent | 2026-07-01 |
| Business Rules | `docs/official/vision.md` (Introduction, Product Overview); detailed rules TBD — pending Use Case Definitions and Architecture Notebook | Agent | 2026-07-01 |
| System Constraints | `docs/official/vision.md` (Other Product Requirements) | Agent | 2026-07-01 |
| System Compliance | `docs/official/vision.md` (Introduction — PII/data privacy) | Agent | 2026-07-01 |
| System Documentation | `docs/official/vision.md` (Other Product Requirements — User Manual, OpenAPI) | Agent | 2026-07-01 |

# Prompts

1. "SRS dokümanına geçebiliriz. Bu noktada oluşturmul olduğun @docs/official/vision.md ve yazılıma bakarak yardım alabilirsin."

2. "Oluşturmuş olduğun @docs/official/system-requirements.md dosyasındaki Business Rules'ları oluştururken son yaptığımız @docs/category-course-cohort-bounds.md dosyasını da hesaba kattın mı? Bu verileri hesaba katmadıysan buna göre güncelleme yap."

3. "@docs/official/system-requirements.md dosyası içerisindeki System Documentation ve Traceability Table başlıklarının altını sanki daha yazılımı yazmamışız gibi doldur. Bu zamana kadar @docs/official/vision.md dokümanını yaptık. Bu sebep ile /docs/architecture.md ya da /docs/official gibi veridiğin path'lerin bir anlamı yok. Normalde SRS dokümanı bu süreçte nasıl doldurulması gerekiyorsa, bu iki başlığın altındaki tablolaro buna göre güncelle."

4. "Tamam şimdi aynı şekilde vision dokümanının altındaki Traceability Table'ı doldur. Dokümaları hazırlama sıram, Vision, SRS, architectural notebook, use-case ve graphical user interface. Bunların tracebility table'larını güncelle"

Conversation link: Current Cursor session.
