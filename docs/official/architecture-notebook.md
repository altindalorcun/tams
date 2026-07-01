**TAMS — Transcript Analysis and Graduation Control System**

**Architecture Notebook**

# Purpose

This notebook records the architectural philosophy, significant decisions, constraints, and mechanisms that shape the design and implementation of TAMS. It answers **why** the system is structured the way it is — complementing the separate **TAMS Architecture Document**, which answers **what** the system contains and **how** its components interact.

Developers, reviewers, and future maintainers should consult this document when evaluating changes that affect service boundaries, data flows, privacy guarantees, or graduation rule evaluation semantics. Sections 2 through 6 of the architecture notebook template are addressed below, along with architectural views, traceability, and prompts.

# Architectural Goals and Philosophy

TAMS architecture is driven by the constraints of processing sensitive university student data under unpredictable graduation-period load. The following goals guide every structural and behavioral decision.

## PII-by-Design

Student transcripts contain personally identifiable information (national identity numbers, student IDs). The architecture treats PII as a liability to be minimized rather than an asset to be stored. PDF bytes are processed entirely in memory, national identity numbers never reach persistent storage or downstream Kafka topics, and Kafka topic retention windows limit transient exposure. This goal directly supports the **Vision Document** and requirements FR-PII-001 through FR-PII-003.

## Async Decoupling

PDF parsing is CPU-intensive and variable in duration. Blocking the upload HTTP request until parsing completes would degrade teacher experience and tie analysis-service thread pools to parser latency. The architecture offloads parsing to a dedicated consumer via Kafka, allowing analysis-service to accept uploads quickly and process results asynchronously.

## Independent Scalability

Different components face different load profiles: the parser is CPU-bound during batch uploads, the gateway is I/O-bound under concurrent browser sessions, and rule-service traffic is comparatively steady. Microservice decomposition with separate Horizontal Pod Autoscaler targets (parser-service, analysis-service) allows each component to scale independently during graduation-week spikes.

## Graduation-Period Resilience

System usage follows the academic calendar, with sudden traffic spikes at semester ends and during graduation weeks. The architecture addresses this through api-gateway rate limiting (FR-SYS-002), Kafka buffering between upload and parse stages, and Kubernetes autoscaling — rather than over-provisioning a monolithic deployment year-round.

## Long-Term Maintainability

The monorepo structure, Flyway-managed schemas per service, OpenAPI documentation on all backends, and consistent package layout (`controller → service → repository` in Spring Boot; feature folders in React) reduce onboarding cost and enable independent schema evolution without cross-service database coupling.

# Assumptions and Dependencies

The following assumptions underpin architectural decisions. If any assumption changes, this notebook and the technical Architecture Document must be revised accordingly.

| Assumption | Implication |
| --- | --- |
| Single-university MVP scope | No external Student Information System (SIS) integration; standalone JWT authentication (LDAP deferred to Release 2) |
| Standard Hacettepe PDF transcript format | parser-service uses regex-based extraction tuned to a known layout; other formats are out of scope |
| Kafka (KRaft mode) available in deployment | Async transcript pipeline depends on message broker availability |
| Three PostgreSQL instances available | One database per bounded context (auth, rules, analysis); no shared schema |
| Stateless JWT authentication | No server-side session store; gateway validates tokens with shared secret |
| Team skills: Java/Spring Boot, Python, React | JVM services for CRUD and orchestration; Python for PDF libraries; React for web UI |
| Registration date extractable from transcript | Cohort bounds (AD-007) require `registration_date` in parsed metadata; missing date disables per-course filtering (BR-COHORT-002) |

# Architecturally Significant Requirements

The following requirements from the Vision Document and System-Wide Requirements Specification (SRS) directly drive architectural structure.

| Requirement | Source IDs | Architectural Impact |
| --- | --- | --- |
| PII must not be persisted on disk | FR-PII-001, FR-PII-002 | Stateless parser-service; in-memory PDF handling; PII masking before Kafka publish |
| Short-lived Kafka transcript topics | FR-PII-003 | `transcript.raw` 5 min retention; `transcript.parsed` 1 hr retention |
| Async PDF processing pipeline | FR-ANAL-002 | Kafka decoupling; separate parser-service (AD-002, AD-003) |
| Graduation rule engine with v2 capabilities | FR-RULE-003, BR-GRAD-*, BR-EQ-001 | rule-service internal API; GraduationEngine in analysis-service (AD-006) |
| Course-level cohort bounds with term granularity | FR-RULE-005–007, BR-COHORT-* | EnrollmentCohortComparator; V14 migration; admin UI cohort fields (AD-007) |
| Role-based access for three user types | FR-AUTH-002 | Gateway JWT validation; frontend ProtectedRoute; service-level role checks |
| Rate limiting during peak load | FR-SYS-002 | api-gateway RateLimitFilter (AD-005) |
| Teacher-student result access control | FR-AUTH-005 | auth-service internal teacher-student map |
| Health checks on all services | FR-SYS-001 | Kubernetes liveness/readiness probes |

# Decisions, Constraints, and Justifications

Each architectural decision is assigned an `AD-*` identifier for cross-document traceability.

## AD-001 — Microservice Decomposition

**Decision:** Split the platform into five backend microservices (api-gateway, auth-service, rule-service, analysis-service, parser-service) plus a React frontend.

**Justification:** Each service maps to a distinct bounded context with different scaling and technology needs. Auth, rules, and analysis require persistent state and CRUD APIs; the parser is stateless and CPU-bound; the gateway handles cross-cutting ingress concerns. A monolith would force uniform scaling and prevent using Python for PDF parsing.

**Trade-off:** Operational complexity increases (more containers, inter-service communication, distributed tracing). Accepted because graduation-period scaling and technology heterogeneity outweigh monolith simplicity for this domain.

## AD-002 — Kafka for Transcript Pipeline

**Decision:** Use Kafka topics (`transcript.raw`, `transcript.parsed`) for asynchronous communication between analysis-service and parser-service.

**Justification:** Decouples upload acceptance from parse completion; absorbs latency spikes when many teachers upload simultaneously; allows parser-service replicas to consume in parallel.

**Trade-off:** Adds Kafka as infrastructure dependency and introduces eventual consistency between upload and result. Accepted because synchronous parse would block HTTP threads and fail under graduation-week load.

## AD-003 — parser-service in Python / FastAPI

**Decision:** Implement PDF parsing as a Python FastAPI microservice using pdfplumber (primary) and PyPDF2 (fallback).

**Justification:** Mature PDF text extraction libraries exist in the Python ecosystem but are limited or immature on the JVM. Keeping parsing in a dedicated Python service avoids compromising the Spring Boot codebase with native bindings or subprocess hacks.

**Trade-off:** Polyglot stack increases build and deployment surface. Accepted because parsing quality and development velocity depend on library availability.

## AD-004 — Three Separate PostgreSQL Databases

**Decision:** Deploy independent PostgreSQL instances for auth (`tams_auth`), rules (`tams_rules`), and analysis (`tams_analysis`).

**Justification:** Data isolation per bounded context; independent Flyway migration lifecycles; failure or migration in one database does not affect others. analysis-service fetches rules via REST rather than direct DB access, preserving service boundaries.

**Trade-off:** No cross-context SQL joins; rule snapshots require an API call at evaluation time. Accepted to enforce encapsulation and independent deployability.

## AD-005 — api-gateway as Single Entry Point

**Decision:** Route all external client traffic through Spring Cloud Gateway MVC.

**Justification:** Centralizes JWT validation, CORS enforcement, rate limiting (10 RPS default, burst 20), and the 10 MB PDF upload size limit. Downstream services do not expose public endpoints directly.

**Trade-off:** Gateway becomes a single point of failure and a potential bottleneck. Mitigated by gateway replication and rate limiting to protect backends.

## AD-006 — Graduation Rule Engine v2

**Decision:** Evolve the graduation engine beyond simple category thresholds to support global department checks (minimum total ECTS, fail-grade block), curriculum equivalence, conditional thresholds, and prefix sub-limits.

**Justification:** Real Hacettepe BBM graduation requirements demand dimensions that fixed per-category credit/ECTS thresholds cannot express — including global department ECTS checks, curriculum equivalence, conditional thresholds, and prefix sub-limits.

**Trade-off:** Increased engine complexity and rule-service data model surface. Accepted because incorrect eligibility results have higher stakeholder cost than engine maintenance.

## AD-007 — Course-Level Cohort Bounds with Term Granularity

**Decision:** Separate course assignment **applicability** (when a pool entry applies to a student's enrollment cohort) from the **mandatory** flag, with year-and-term bounds (`GUZ`/`BAHAR`) on each category-course assignment.

**Justification:** Courses such as BBM487 (elective lab for pre-2017 Fall cohorts) and BBM384 (mandatory from 2017 Fall) require within-year term boundaries (`GUZ`/`BAHAR`) that year-only fields cannot express; applicability is independent of the mandatory flag.

**Trade-off:** Admin UI and validation complexity increase; engine requires registration date metadata. Accepted because incorrect cohort filtering produces false graduation results.

## Developer Guidelines (DO / DON'T)

| DO | DON'T |
| --- | --- |
| Route all external client traffic through api-gateway | Expose backend service ports directly to the public internet |
| Process PDF bytes in memory via Kafka messages | Write raw PDF files to disk or object storage |
| Mask or exclude PII before Kafka publish and DB persistence | Log transcript payloads or national identity numbers |
| Fetch graduation rules via rule-service REST API | Query `tams_rules` database directly from analysis-service |
| Use Flyway migrations for all schema changes | Apply manual SQL patches to production databases |
| Keep business logic in service layer, not controllers or gateway | Add domain logic to api-gateway filters |

# Architectural Mechanisms

Each mechanism describes a recurring architectural pattern, its purpose, and its current implementation state.

## JWT Authentication

**Purpose:** Stateless authentication and role propagation across microservices.

**Attributes:** Access token + refresh token rotation; roles embedded in JWT claims (ADMIN, TEACHER, STUDENT); shared secret validation at gateway.

**Current state:** auth-service issues tokens via `AuthController`; api-gateway `JwtAuthenticationFilter` validates on every request except `/api/v1/auth/**`; mandatory password change enforced before AppShell access (FR-AUTH-003).

**Key components:** `auth-service`, `JwtAuthenticationFilter`, `ProtectedRoute` (frontend).

## Kafka Pub/Sub Pipeline

**Purpose:** Asynchronous, decoupled transcript processing between upload and parse/evaluate stages.

**Attributes:** KRaft mode (no Zookeeper); short retention for PII safety; at-least-once delivery with consumer retry.

**Current state:** analysis-service publishes to `transcript.raw`; parser-service consumes, parses, publishes to `transcript.parsed`; analysis-service consumes parsed data and triggers GraduationEngine. Retention: 5 min (raw), 1 hr (parsed).

**Key components:** `analysis-service` Kafka producers/consumers, `parser-service` consumer/producer, Kafka KRaft broker.

## PII Masking Pipeline

**Purpose:** Prevent sensitive identity data from entering persistent storage or logs.

**Attributes:** In-memory-only PDF processing; national identity number excluded from all downstream payloads; parser failures logged without message body.

**Current state:** `pii_masker.py` validates published payloads; student number retained in masked form for result correlation; TC Kimlik No never published to `transcript.parsed`.

**Key components:** `parser-service/src/pii/pii_masker.py`, `pdf_parser.py`.

## Graduation Rule Engine v2

**Purpose:** Evaluate parsed transcript courses against admin-defined graduation requirements.

**Attributes:** Global department checks; per-category credit/ECTS/count/mandatory evaluation; curriculum equivalence fixpoint expansion; conditional thresholds; prefix sub-limits.

**Current state:** `GraduationEngine.java` orchestrates evaluation; fetches rule set from `GET /internal/rules/{departmentId}`; persists masked results and deficiencies to `tams_analysis`.

**Key components:** `GraduationEngine`, `CurriculumEquivalenceExpander`, `GpaCalculator`, rule-service internal API.

## Enrollment Cohort Comparator

**Purpose:** Determine whether a category or course assignment applies to a student's enrollment cohort.

**Attributes:** Enrollment year and term (`GUZ`/`BAHAR`) derived from `registration_date`; inclusive start bound; exclusive end bound; BAHAR precedes GUZ within the same calendar year; null term defaults to GUZ.

**Current state:** `EnrollmentCohortComparator.java` used in `GraduationEngine.evaluateCategory()`; category-level year-only skip evaluated first; missing registration date disables per-course filtering (BR-COHORT-002).

**Key components:** `EnrollmentCohortComparator`, `EnrollmentYearParser`, `CategoryCourse` entity (rule-service), `CohortBoundaryFields.tsx` (admin UI).

## Flyway Schema Migration

**Purpose:** Version-controlled, repeatable database schema evolution per Spring Boot service.

**Attributes:** SQL migration files under `db/migration/`; applied on service startup; independent per database.

**Current state:** auth-service (users, teacher-student map), rule-service (departments through V14 cohort term bounds), analysis-service (jobs, results, deficiencies).

**Key components:** Flyway plugin in each Spring Boot service `pom.xml`.

## Multi-Stage Docker Build

**Purpose:** Secure, reproducible container images for all services.

**Attributes:** Separate build and runtime stages; non-root user in final image; environment-based configuration.

**Current state:** Java services use Maven build + JRE Alpine runtime; parser-service uses Python slim; frontend uses Node build + nginx serve. Deployed via Docker Compose (local) and Kubernetes manifests (`infrastructure/k8s/`).

**Key components:** Service `Dockerfile`s, `docker-compose.yml`, K8s Deployments.

# Key Abstractions

The following concepts define the domain model across services. They map to entities, DTOs, or Kafka message shapes in the codebase.

| Abstraction | Owning Service | Description |
| --- | --- | --- |
| User / Role | auth-service | Authenticated actor with exactly one role: ADMIN, TEACHER, or STUDENT |
| TeacherStudentMap | auth-service | Links a teacher to students they may upload transcripts for and view results of |
| Department | rule-service | Academic unit with optional global thresholds (`min_total_ects`, `block_on_any_f_grade`) |
| Course | rule-service | Global course catalog entry (code, credit, ECTS) |
| GraduationCategory | rule-service | Department-scoped requirement group with credit/ECTS/count thresholds and optional year-only cohort bounds |
| CategoryCourseAssignment | rule-service | Course in a category pool with `is_mandatory` flag and course-level year+term cohort bounds |
| CurriculumEquivalenceRule | rule-service | Maps legacy or alternate course codes to current curriculum codes |
| ParsedCourse / Transcript | parser-service → analysis-service | PII-free course data extracted from PDF, delivered via Kafka |
| AnalysisJob | analysis-service | Tracks upload-to-result lifecycle (PENDING, COMPLETED, FAILED) |
| AnalysisResult | analysis-service | Graduation eligibility outcome with per-category breakdown and deficiency list |

# Layers or Architectural Framework

TAMS follows a layered microservices architecture with an event-driven parsing tier.

```
┌─────────────────────────────────────────────────────────────┐
│  Presentation Layer — React (Vite), role-protected routes   │
└────────────────────────────┬────────────────────────────────┘
                             │ HTTPS
┌────────────────────────────▼────────────────────────────────┐
│  Edge Layer — api-gateway (JWT, CORS, rate limit, routing)    │
└──────┬──────────────────┬──────────────────┬────────────────┘
       │                  │                  │
┌──────▼──────┐  ┌────────▼────────┐  ┌─────▼──────────────┐
│ auth-service│  │  rule-service   │  │ analysis-service   │
│  (Spring)   │  │   (Spring)      │  │    (Spring)        │
└──────┬──────┘  └────────┬────────┘  └───┬────────┬───────┘
       │                  │               │        │
┌──────▼──────┐  ┌────────▼────────┐      │   ┌────▼─────┐
│ postgres-   │  │ postgres-rules  │      │   │  Kafka   │
│ auth        │  │                 │      │   └────┬─────┘
└─────────────┘  └─────────────────┘      │        │
                                   ┌──────▼────────▼───────┐
                                   │ postgres-analysis     │
                                   └───────────────────────┘
                                            ▲
                                     ┌──────┴──────┐
                                     │parser-service│
                                     │  (Python)   │
                                     └─────────────┘
```

**Presentation layer:** Feature-based React folders (`auth`, `admin`, `teacher`, `student`); TanStack Query for server state; Axios with JWT interceptors.

**Edge layer:** api-gateway routes by path prefix; no business logic.

**Application layer:** Spring Boot services follow controller → service → repository; Python parser is a Kafka consumer/producer with no database.

**Data layer:** Three isolated PostgreSQL instances; Kafka for transient transcript messages only.

# Architectural Views

## Logical View

The logical structure of TAMS comprises five backend microservices, one frontend application, and a Kafka message broker. At a high level:

- **Client** communicates only with api-gateway and the static frontend.
- **auth-service** owns identity and teacher-student relationships.
- **rule-service** owns graduation rule definitions; exposes public Admin CRUD and an internal read API.
- **analysis-service** orchestrates upload, consumes parsed transcripts, runs GraduationEngine, persists results.
- **parser-service** is stateless; transforms raw PDF bytes into structured, PII-free course data.

Critical inter-service interfaces:

| Interface | Direction | Purpose |
| --- | --- | --- |
| `POST /internal/auth/validate` | gateway → auth-service | Token introspection (optional; gateway uses shared secret) |
| `GET /internal/rules/{departmentId}` | analysis-service → rule-service | Full rule set for evaluation |
| `GET /internal/teacher-student/{studentId}` | analysis-service → auth-service | Access control for result queries |
| `transcript.raw` / `transcript.parsed` | analysis ↔ parser via Kafka | Async parse pipeline |

## Operational View

Deployment supports two topologies:

| Environment | Mechanism | Notes |
| --- | --- | --- |
| Local / single-server | Docker Compose (`infrastructure/docker-compose.yml`) | Full stack for development and integration testing |
| Production | Kubernetes (`infrastructure/k8s/`) | HPA, Ingress, cert-manager, StatefulSets for Kafka and PostgreSQL |

Processes per deployment unit:

| Component | Replicas (min) | Scaling trigger |
| --- | --- | --- |
| api-gateway | 1+ | Optional HPA on request rate |
| auth-service | 1+ | Steady; low burst |
| rule-service | 1+ | Steady; admin CRUD only |
| analysis-service | 1+ | HPA on CPU (graduation spikes) |
| parser-service | 1+ | HPA on CPU (PDF parse load) |
| frontend (nginx) | 1+ | Static assets; low resource |
| Kafka KRaft | 1 broker (MVP) | StatefulSet in K8s |
| PostgreSQL × 3 | 1 each | StatefulSet with PVC |

HTTPS termination via Ingress and cert-manager in Kubernetes; Docker Compose HTTPS via reverse-proxy override.

## Use Case View

Formal use case definitions live under [`docs/official/use-cases/`](use-cases/). The table below lists all MVP use cases; entries marked **significant** drive architectural structure directly.

| UC Code | Name | Document | Significance |
| --- | --- | --- | --- |
| UC-AUTH-001 | User login | [UC-AUTH-001-login.md](use-cases/UC-AUTH-001-login.md) | **Significant** — JWT issuance; gateway validation; role embedding |
| UC-AUTH-002 | Token refresh | [UC-AUTH-002-token-refresh.md](use-cases/UC-AUTH-002-token-refresh.md) | Refresh token rotation (FR-AUTH-001) |
| UC-AUTH-003 | Mandatory password change | [UC-AUTH-003-mandatory-password-change.md](use-cases/UC-AUTH-003-mandatory-password-change.md) | First-login gate before AppShell (FR-AUTH-003) |
| UC-AUTH-004 | User logout | [UC-AUTH-004-logout.md](use-cases/UC-AUTH-004-logout.md) | Session termination |
| UC-ADMIN-001 | Manage departments | [UC-ADMIN-001-manage-departments.md](use-cases/UC-ADMIN-001-manage-departments.md) | Global department thresholds (BR-GRAD-006/007) |
| UC-ADMIN-002 | Manage courses | [UC-ADMIN-002-manage-courses.md](use-cases/UC-ADMIN-002-manage-courses.md) | Global course catalog |
| UC-ADMIN-003 | Manage graduation categories | [UC-ADMIN-003-manage-graduation-categories.md](use-cases/UC-ADMIN-003-manage-graduation-categories.md) | **Significant** — rule model; cohort bounds admin UI (AD-007) |
| UC-ADMIN-004 | Manage curriculum equivalence rules | [UC-ADMIN-004-manage-curriculum-equivalence-rules.md](use-cases/UC-ADMIN-004-manage-curriculum-equivalence-rules.md) | Equivalence expansion input (BR-EQ-001) |
| UC-ADMIN-005 | Manage users | [UC-ADMIN-005-manage-users.md](use-cases/UC-ADMIN-005-manage-users.md) | Admin-provisioned accounts; mandatory password change |
| UC-TEACH-001 | Upload transcript PDF | [UC-TEACH-001-upload-transcript-pdf.md](use-cases/UC-TEACH-001-upload-transcript-pdf.md) | **Significant** — triggers Kafka pipeline; 10 MB limit; async job model |
| UC-TEACH-002 | View analysis result | [UC-TEACH-002-view-analysis-result.md](use-cases/UC-TEACH-002-view-analysis-result.md) | Masked result retrieval |
| UC-TEACH-003 | View analysis history | [UC-TEACH-003-view-analysis-history.md](use-cases/UC-TEACH-003-view-analysis-history.md) | Paginated teacher-scoped queries |
| UC-STUD-001 | View own analysis result | [UC-STUD-001-view-own-analysis-result.md](use-cases/UC-STUD-001-view-own-analysis-result.md) | JWT studentNumber claim matching |
| UC-SYS-001 | Async PDF parsing | [UC-SYS-001-async-pdf-parsing.md](use-cases/UC-SYS-001-async-pdf-parsing.md) | **Significant** — in-memory processing; PII masking; Kafka retention |
| UC-SYS-002 | Graduation eligibility evaluation | [UC-SYS-002-graduation-eligibility-evaluation.md](use-cases/UC-SYS-002-graduation-eligibility-evaluation.md) | **Significant** — GraduationEngine; rule-service fetch; result persistence |

# Traceability Table

| Section | Source | Author | Date |
| --- | --- | --- | --- |
| Purpose | Architecture Notebook template; `docs/official/vision.md` | Agent | 2026-07-01 |
| Architectural Goals and Philosophy | `docs/official/vision.md` (Introduction, Other Product Requirements — PII, microservices, peak load) | Agent | 2026-07-01 |
| Assumptions and Dependencies | `docs/official/vision.md` (User Environment, Other Product Requirements) | Agent | 2026-07-01 |
| Architecturally Significant Requirements | `docs/official/system-requirements.md` (System-Wide Functional Requirements — FR-*) | Agent | 2026-07-01 |
| Decisions, Constraints, and Justifications | `docs/official/vision.md` (microservices, Docker/K8s); `docs/official/system-requirements.md` (System Constraints, FR-PII-*, FR-ANAL-002, FR-SYS-002) | Agent | 2026-07-01 |
| Architectural Mechanisms | `docs/official/system-requirements.md` (FR-AUTH-*, FR-PII-*, FR-ANAL-002, FR-SYS-003); `docs/official/vision.md` (Kafka, PII handling) | Agent | 2026-07-01 |
| Key Abstractions | `docs/official/vision.md` (Stakeholder Descriptions — Admin rule domains); `docs/official/system-requirements.md` (Glossary, Business Rules) | Agent | 2026-07-01 |
| Layers and Architectural Views | `docs/official/vision.md` (technology stack); `docs/official/system-requirements.md` (System Interfaces — Software Interfaces) | Agent | 2026-07-01 |
| Use Case View | `docs/official/vision.md` (Product Overview — Needs and Features); `docs/official/system-requirements.md` (System-Wide Functional Requirements); Use Case Definitions — Planned | Agent | 2026-07-01 |

# Prompts

1. "O zaman şimdi Architecture Notebook'a geçelim. architectural notebook template kullanarak bu dokümanı yaz."

2. "Doküman içerisinde /docs altındaki dosyalardan bahsetme. architecture-notebook dokümanı buna göre güncelle."

3. "Tamam şimdi aynı şekilde vision dokümanının altındaki Traceability Table'ı doldur. Dokümaları hazırlama sıram, Vision, SRS, architectural notebook, use-case ve graphical user interface. Bunların tracebility table'larını güncelle"

Conversation link: Current Cursor session.
