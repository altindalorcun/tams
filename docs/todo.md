# TAMS — MVP Development Roadmap

This checklist covers every step from an empty repository to a fully functional MVP running on Kubernetes. Complete phases in order; each phase builds on the previous one.

---

## Phase 0 — Repo & DevOps Bootstrap

- [x] Initialize the monorepo root (`tams/`) with a top-level `README.md` explaining the project and how to run it locally
- [x] Create the top-level directory structure as specified in `docs/architecture.md` (`services/`, `frontend/`, `infrastructure/`, `docs/`)
- [x] **[AI]** Create the root `pom.xml` (Maven parent POM): `<packaging>pom</packaging>`, inherit from `spring-boot-starter-parent 3.3.5`, declare `services/api-gateway`, `services/auth-service`, `services/rule-service`, `services/analysis-service` as `<modules>`; define shared `<properties>`: `java.version=21`, `spring-cloud.version`, `jjwt.version`, `springdoc.version`; add Spring Cloud BOM to `<dependencyManagement>`
- [x] Open the `tams/` root folder in IntelliJ IDEA Ultimate (`File → Open`); click **Trust Project** when prompted; confirm all four Java modules appear in the Maven side panel and IntelliJ shows no unresolved imports after `Reload All Maven Projects`
- [x] Add a `.gitignore` at the root covering Java (`target/`, `*.class`), Python (`__pycache__/`, `.venv/`, `*.pyc`), Node (`node_modules/`, `dist/`), Docker, and IDE files
- [x] Add a `.env.example` file at the root listing all required environment variables with placeholder values and comments (do not commit real secrets)
- [x] Create `infrastructure/docker-compose.infra.yml` that starts only the shared infrastructure: Kafka (KRaft), and three PostgreSQL instances (`postgres-auth`, `postgres-rules`, `postgres-analysis`)
- [x] Create `infrastructure/docker-compose.yml` that extends `docker-compose.infra.yml` and also starts all five backend services and the frontend (for full local integration testing)
- [x] Write a root-level `Makefile` with convenience targets: `make infra-up`, `make infra-down`, `make up`, `make down`, `make build-all`

---

## Phase 1 — Shared Infrastructure: Kafka & Databases

- [x] Configure Kafka in KRaft mode (no Zookeeper) in the Docker Compose infra file
- [x] Define Kafka topics via a startup script or environment config:
  - `transcript.raw` (retention: 5 min, cleanup: delete)
  - `transcript.parsed` (retention: 1 hr, cleanup: delete)
- [x] Configure three PostgreSQL containers, each with its own named volume, initial database, user, and password sourced from `.env`
- [x] Verify all infra services start cleanly with `docker compose -f infrastructure/docker-compose.infra.yml up -d` and pass basic health checks

---

## Phase 2 — auth-service (Spring Boot)

- [ ] **[User]** Generate `auth-service` via Spring Initializr (start.spring.io): Project=Maven, Language=Java, Spring Boot=3.3.5, Java=21, dependencies: Spring Web, Spring Security, Spring Data JPA, PostgreSQL Driver, Lombok, Flyway Migration. Place the generated files under `services/auth-service/`.
- [x] Update `services/auth-service/pom.xml`: replace the auto-generated `<parent>` block (which points to `spring-boot-starter-parent`) with one that points to the root `tams` parent POM using `<relativePath>../../pom.xml</relativePath>`; add JJWT dependency (`jjwt-api`, `jjwt-impl`, `jjwt-jackson`) referencing `${jjwt.version}` from root properties
- [x] Write `Dockerfile` for `auth-service` using a multi-stage build (build stage: `maven:3-eclipse-temurin-21`, final stage: `eclipse-temurin:21-jre-alpine`, non-root user)
- [x] Configure `application.yml` to read all sensitive values (DB URL, JWT secret) from environment variables
- [x] Write Flyway migration `V1__create_users_table.sql` creating the `users` table as defined in `docs/architecture.md`
- [x] Write Flyway migration `V2__create_teacher_student_map.sql` creating the `teacher_student_map` table
- [x] Implement `User` JPA entity and `UserRepository`
- [x] Implement `AuthService`: register, login, JWT issuance, refresh token rotation
- [x] Implement `POST /api/v1/auth/register` — create user with hashed password (`BCryptPasswordEncoder`)
- [x] Implement `POST /api/v1/auth/login` — return access token + refresh token
- [x] Implement `POST /api/v1/auth/refresh` — exchange refresh token for new access token
- [x] Implement `POST /api/v1/auth/logout` — invalidate refresh token
- [x] Implement `POST /internal/auth/validate` — token introspection endpoint (not exposed externally)
- [x] Implement `POST /internal/teacher-student` — create teacher-student relationship
- [x] Implement `GET /internal/teacher-student/{studentId}` — verify student is linked to a teacher
- [x] Configure Spring Security: permit `/api/v1/auth/**` without auth, require auth on all other endpoints
- [x] Write unit tests for `AuthService` (token generation, password hashing, role extraction)
- [x] Write integration tests for login and register endpoints
- [x] Add Swagger / OpenAPI 3.0 documentation (`springdoc-openapi-starter-webmvc-ui`)
- [x] Seed an initial Admin user on startup via `ApplicationRunner` (credentials from env vars)

---

## Phase 3 — rule-service (Spring Boot)

- [x] **[User]** Generate `rule-service` via Spring Initializr: same settings as auth-service but omit JJWT; dependencies: Spring Web, Spring Security, Spring Data JPA, PostgreSQL Driver, Lombok, Flyway Migration. Place under `services/rule-service/`.
- [x] Update `services/rule-service/pom.xml`: replace the auto-generated `<parent>` block with one pointing to the root `tams` parent POM using `<relativePath>../../pom.xml</relativePath>`
- [x] Write `Dockerfile` (multi-stage, non-root user)
- [x] Configure `application.yml` with DB connection from environment variables
- [x] Delete old V1/V2 migrations; write 5 new Flyway migrations:
  - `V1__create_departments_table.sql`
  - `V2__create_courses_table.sql` (global catalog, `course_code UNIQUE`)
  - `V3__create_department_courses_table.sql` (junction: department ↔ course)
  - `V4__create_categories_table.sql` (`department_id FK`, `min_course_count`)
  - `V5__create_category_courses_table.sql` (junction: category ↔ course, `is_mandatory`)
- [x] Implement JPA entities and repositories: `Department`, `Course`, `Category`; junction embeddables `DepartmentCourseId`, `CategoryCourseId` and their entities
- [x] Implement `DepartmentService`: create, read all, read by id, update, delete; add/remove courses from department pool
- [x] Implement `CourseService`: create, read all, read by id, update, delete (global catalog)
- [x] Implement `CategoryService`: create under department, read by department, read by id, update, delete; add/remove courses from category
- [x] Implement REST endpoints:
  - `POST   /api/v1/departments` — Admin only
  - `GET    /api/v1/departments` — Admin only
  - `GET    /api/v1/departments/{id}` — Admin only
  - `PUT    /api/v1/departments/{id}` — Admin only
  - `DELETE /api/v1/departments/{id}` — Admin only
  - `POST   /api/v1/departments/{id}/courses` — Admin only (add course to department pool)
  - `GET    /api/v1/departments/{id}/courses` — Admin only
  - `DELETE /api/v1/departments/{id}/courses/{courseId}` — Admin only
  - `POST   /api/v1/courses` — Admin only
  - `GET    /api/v1/courses` — Admin only
  - `GET    /api/v1/courses/{id}` — Admin only
  - `PUT    /api/v1/courses/{id}` — Admin only
  - `DELETE /api/v1/courses/{id}` — Admin only
  - `POST   /api/v1/departments/{deptId}/categories` — Admin only
  - `GET    /api/v1/departments/{deptId}/categories` — Admin only
  - `GET    /api/v1/departments/{deptId}/categories/{catId}` — Admin only
  - `PUT    /api/v1/departments/{deptId}/categories/{catId}` — Admin only
  - `DELETE /api/v1/departments/{deptId}/categories/{catId}` — Admin only
  - `POST   /api/v1/categories/{catId}/courses` — Admin only (add course to category)
  - `GET    /api/v1/categories/{catId}/courses` — Admin only
  - `DELETE /api/v1/categories/{catId}/courses/{courseId}` — Admin only
  - `GET    /internal/rules/{departmentId}` — internal only, returns full rule set for analysis-service
- [x] Configure Spring Security: JWT filter extracts role from shared-secret token; enforce `ADMIN` on all `/api/v1/**`; permit `/internal/**` and `/actuator/health` without auth
- [x] Write unit tests for `DepartmentService`, `CourseService`, `CategoryService`
- [x] Write integration tests for department and category CRUD endpoints
- [x] Add Swagger / OpenAPI 3.0 documentation

---

## Phase 4 — parser-service (Python / FastAPI)

- [x] **[User]** Create the `services/parser-service/` directory.
- [x] **[AI]** Scaffold the full Python FastAPI project inside `services/parser-service/`: `requirements.txt` (fastapi, uvicorn, pdfplumber, pypdf, confluent-kafka, pydantic, pydantic-settings, python-dotenv; pytest for dev), complete `src/` directory layout (`main.py`, `config.py`, `consumer.py`, `producer.py`, `parser/pdf_parser.py`, `parser/models.py`, `parser/grades.py`, `pii/pii_masker.py`), `Dockerfile` (multi-stage: `python:3.12-slim`, non-root user `appuser`), `.env.example`, and `tests/` with a `fixtures/` directory for sample PDFs
- [ ] In IntelliJ IDEA Ultimate: create a virtual environment (`python -m venv services/parser-service/.venv`); go to `File → Project Structure → SDKs → +` and add the `.venv/bin/python` interpreter; right-click `services/parser-service/src/` → **Mark Directory As → Sources Root**
- [x] Implement `pii_masker.py`:
  - Detect TC Kimlik No (11-digit number pattern) and Öğrenci No from parsed text
  - Replace each with `sha256(PII_HASH_SALT + raw_value)` truncated to 16 hex chars
  - Unit-test this module in complete isolation — it must never log raw PII values
- [x] Implement `pdf_parser.py`:
  - Accept PDF as `bytes` (never as file path)
  - Use `pdfplumber` to extract text and table data (`pypdf` as fallback engine)
  - Parse student & program metadata: full name, TC Kimlik No, Öğrenci No, faculty, program name + program code, study duration (years), program type (e.g. Lisans), graduation GPA, registration/graduation dates
  - Parse course rows grouped by semester (`1. Sınıf Güz`, etc.): code, name, credit, grade, ECTS, academic year (`Başarı Yılı`), and a derived `is_passed` flag
  - Call `pii_masker` before returning any data (TC + Öğrenci No collapsed into `student_ref`; raw name kept only in the in-memory full model, never published)
  - Return a `ParsedTranscript` Pydantic model
- [x] Implement `consumer.py`: subscribe to `transcript.raw`, deserialize message, call parser, call producer
- [x] Implement `producer.py`: publish `ParsedTranscript` JSON to `transcript.parsed`
- [x] Implement health check endpoint `GET /health` in `main.py`
- [x] Write unit tests for `pdf_parser.py` using sample transcript PDFs (anonymized test fixtures)
- [x] Write unit tests for `pii_masker.py` verifying no raw PII appears in output
- [x] Write integration test: send a message to `transcript.raw`, assert a correctly structured message appears on `transcript.parsed`
- [x] Configure all secrets and Kafka broker URL via environment variables (`.env` / K8s Secret)

---

## Phase 5 — analysis-service (Spring Boot)

- [x] **[User]** Generate `analysis-service` via Spring Initializr: Project=Maven, Language=Java, Spring Boot=3.3.5, Java=21, dependencies: Spring Web, Spring Security, Spring Data JPA, PostgreSQL Driver, Lombok, Flyway Migration, Spring for Apache Kafka. Place under `services/analysis-service/`.
- [x] Update `services/analysis-service/pom.xml`: replace the auto-generated `<parent>` block with one pointing to the root `tams` parent POM; add `spring-boot-starter-webflux` for WebClient (or keep `spring-boot-starter-web` and use `RestTemplate`)
- [x] Write `Dockerfile` (multi-stage, non-root user)
- [x] Configure `application.yml` with DB, Kafka broker, and rule-service URL from environment variables
- [x] Write Flyway migrations for `analysis_results` (include `department_id UUID NOT NULL`), `deficiencies`, and `transcript_courses` tables
- [x] Implement JPA entities and repositories for all three tables
- [x] Implement `TranscriptUploadController`:
  - `POST /api/v1/transcripts` — accept multipart PDF + `departmentId` (UUID) request parameter; teacher selects which department's rules to apply; store jobId, publish to `transcript.raw` Kafka topic, return `202 Accepted` with jobId
- [x] Implement Kafka producer: serialize PDF bytes + metadata (jobId, teacherId, departmentId) to `transcript.raw`
- [x] Implement `TranscriptParsedConsumer`: consume from `transcript.parsed`, trigger the graduation engine
- [x] Implement `RuleServiceClient` (WebClient): fetch full rule set from `GET http://rule-service/internal/rules/{departmentId}`
- [x] Implement `GraduationEngine`:
  - For each category in the rule set, count how many courses from the pool the student passed (`min_course_count` check)
  - Sum credits and ECTS of passed courses that match course codes (`min_credit` / `min_ects` check)
  - Verify all `is_mandatory=true` courses are passed regardless of count/credit thresholds
  - Return `AnalysisResult` with overall eligibility flag and per-category deficiencies
- [x] Implement `ResultService`: persist `AnalysisResult` and all `Deficiency` rows to the database
- [x] Implement result query endpoints:
  - `GET /api/v1/results` — Teacher: list all results for their uploaded students (paginated, searchable by student_ref)
  - `GET /api/v1/results/{id}` — Teacher or Student: get full result with deficiency details
  - `GET /api/v1/results/me` — Student: get own latest result (matched by masked_student_ref)
  - `GET /api/v1/transcripts/{jobId}/status` — poll analysis status (PENDING / COMPLETED / FAILED)
- [x] Enforce Spring Security role guards on all endpoints
- [x] Write unit tests for `GraduationEngine` covering: fully eligible, credit deficit, missing mandatory course, multiple category deficits
- [x] Write integration tests for upload and result retrieval
- [x] Add Swagger / OpenAPI 3.0 documentation

---

## Phase 6 — api-gateway (Spring Cloud Gateway)

- [x] **[User]** Generate `api-gateway` via Spring Initializr: Project=Maven, Language=Java, Spring Boot=3.3.5, Java=21, dependencies: Gateway, Spring Security. Place under `services/api-gateway/`.
- [x] Update `services/api-gateway/pom.xml`: replace the auto-generated `<parent>` block with one pointing to the root `tams` parent POM; the Spring Cloud Gateway version is managed via the Spring Cloud BOM already declared in the root `<dependencyManagement>` — no extra version pin needed
- [x] Write `Dockerfile` (multi-stage, non-root user)
- [x] Configure routes in `application.yml`:
  - `/api/v1/auth/**` → auth-service (no JWT required)
  - `/api/v1/departments/**`, `/api/v1/courses/**`, `/api/v1/categories/**` → rule-service
  - `/api/v1/transcripts/**`, `/api/v1/results/**` → analysis-service
- [x] Implement `JwtAuthenticationFilter` as a `GlobalFilter`: validate JWT signature and expiry on all routes except `/api/v1/auth/**`; reject with `401` if invalid
- [x] Propagate user identity headers (`X-User-Id`, `X-User-Role`) to downstream services so they can enforce role checks without re-validating the JWT
- [x] Configure CORS: allow the frontend origin, standard methods and headers
- [x] Configure `RequestRateLimiter` filter using Redis or a simple in-memory implementation for MVP
- [x] Configure maximum request body size (`10MB`) for the transcript upload route
- [x] Write integration tests: valid JWT routes through, invalid JWT returns 401, unknown path returns 404
- [x] Add Swagger / OpenAPI 3.0 documentation (or aggregate all service docs via Springdoc)

---

## Phase 7 — frontend (React / Vite)

- [x] **[User]** Create the `frontend/` directory.
- [x] **[AI]** Scaffold the React + Vite + TypeScript project inside `frontend/`: run `npm create vite@latest . -- --template react-ts`, install all runtime dependencies (`react-router-dom`, `axios`, `@tanstack/react-query`, `tailwindcss`, `@tailwindcss/vite`, `zustand`, `next-themes`, `lucide-react`, `react-hook-form`, `@hookform/resolvers`, `zod`, `clsx`, `tailwind-merge`, `class-variance-authority`), initialise shadcn/ui (`npx shadcn@latest init` with base-color `slate` and CSS variables enabled), create `Dockerfile` (multi-stage: `node:22-alpine` build stage, `nginx:alpine` serve stage, non-root user), and set up the full `src/` folder structure (`api/`, `features/auth/`, `features/admin/`, `features/teacher/`, `features/student/`, `components/ui/`, `components/brand/`, `hooks/`, `router/`, `types/`, `assets/logos/`)
- [x] Copy Hacettepe logo assets into `frontend/src/assets/logos/`: `hacettepe-logo.png` (shield emblem) and `hacettepe-logo-horizontal.png` (shield + wordmark). The TAMS logo design reference is saved as `tams-logo-reference.png` in the same directory — it contains both variants side by side.
- [x] **[User]** Export `tams-logo-full.png` (right half of reference — graduation cap + "TAMS" text + subtitle) and `tams-logo-icon.png` (left half — graduation cap only) as separate PNG files into `frontend/src/assets/logos/`; white/transparent background, minimum 2× resolution
- [x] Implement `LogoFull` and `LogoIcon` brand components in `src/components/brand/` as typed `<img>` wrappers with explicit `alt` text and size props; these are the only permitted references to the logo assets
- [x] Wrap the React root with `ThemeProvider` from `next-themes` (`attribute="class"`, `defaultTheme="system"`, `enableSystem`); implement a `ThemeToggle` button component (`src/components/ThemeToggle.tsx`) using shadcn `Button` variant `ghost` with `Sun` / `Moon` icons from `lucide-react`
- [x] Implement `AppShell` layout component (`src/components/AppShell.tsx`): collapsible left sidebar (`LogoIcon` + role-based navigation links), top header bar (`ThemeToggle` + user avatar menu with logout), and `<Outlet />` main content area; sidebar collapses to a drawer on mobile
- [x] In IntelliJ IDEA Ultimate: `frontend/` is auto-detected as a Node.js project when `package.json` is present; run `npm install` from the built-in terminal to restore packages; start the Vite dev server from the **npm** side panel (`View → Tool Windows → npm → dev`)
- [x] Implement Axios instance with base URL from `VITE_API_URL` env variable
- [x] Implement JWT interceptor: attach `Authorization: Bearer <token>` to every request; on 401, redirect to login
- [x] Implement `ProtectedRoute` component that redirects to `/login` if no valid token; also checks role for role-gated pages
- [x] Implement login page (`/login`): full-screen two-column layout — left panel with `LogoFull` centred, right panel with email + password form; `hacettepe-logo-horizontal.png` in the bottom-left corner at reduced opacity; calls `POST /api/v1/auth/login`, stores token in `sessionStorage`; collapse to single-column on mobile
- [x] Implement Admin dashboard (`/admin`):
  - Department list table with add/edit/delete actions
  - Course catalog management (add/edit/delete global courses; assign to departments)
  - Category list per department with add/edit/delete actions; course pool management per category
  - Confirmation modals for destructive actions
- [x] Implement Teacher dashboard (`/teacher`):
  - PDF file upload component with drag-and-drop support; department selector (dropdown from `/api/v1/departments`) to choose which rules apply
  - Job status polling after upload (show spinner until COMPLETED or FAILED)
  - Analysis result display: eligibility badge, category breakdown table, deficiency list
  - Student history table: paginated, searchable by student reference
- [x] Implement Student result page (`/student/results`):
  - Eligibility status banner
  - Per-category credit and ECTS progress (earned vs. required)
  - List of missing mandatory courses
  - Fully responsive / mobile-first layout
- [x] Handle all loading states (skeleton loaders or spinners) and error states (toast notifications or inline messages)
- [x] Write component unit tests with Vitest + React Testing Library for critical components (login form, upload flow, result display)

---

## Phase 8 — Kubernetes Manifests

- [x] Create `infrastructure/k8s/namespace.yaml` defining the `tams` namespace
- [x] For each of the 5 backend services + frontend, create:
  - `deployment.yaml` — image, replicas (min), resource requests/limits, env vars referencing Secrets and ConfigMaps, liveness and readiness probes
  - `service.yaml` — `ClusterIP` type for backend services; `NodePort` or `ClusterIP` for frontend
- [x] Create `infrastructure/k8s/api-gateway/ingress.yaml` as described in `docs/architecture.md`
- [x] Create `infrastructure/k8s/kafka/` manifests for Kafka in KRaft mode (StatefulSet + Service + ConfigMap)
- [x] Create `infrastructure/k8s/postgres/` manifests for each PostgreSQL instance (StatefulSet + Service + PersistentVolumeClaim + Secret)
- [x] Create HPA manifests for `parser-service` and `analysis-service` (and optionally `api-gateway`)
- [x] Create `infrastructure/k8s/tams-config.yaml` ConfigMap with non-sensitive application config
- [x] Create Kubernetes Secret templates (`.yaml.example` files — real secrets managed outside the repo or via a secrets manager)
- [x] Write a `README` in `infrastructure/k8s/` explaining how to apply the manifests in order (`kubectl apply -f namespace.yaml` first, then infra, then services)
- [ ] Smoke-test the full stack on a local Kubernetes cluster (e.g., Minikube or kind)

---

## Phase 9 — Cross-Cutting Concerns

- [ ] Ensure all five services expose `GET /actuator/health` (Spring Boot) or `GET /health` (FastAPI) and that Kubernetes readiness/liveness probes point to these endpoints
- [ ] Configure structured JSON logging on all Spring Boot services (`logstash-logback-encoder`) and Python service (`python-json-logger`)
- [ ] Verify Swagger UI is accessible at `/swagger-ui.html` (Spring) and `/docs` (FastAPI) on each service when running locally
- [ ] Write an end-to-end test scenario (can be manual or automated): Admin creates rules → Teacher uploads transcript → Result appears → Student views result
- [ ] Install cert-manager into the cluster: `kubectl apply -f https://github.com/cert-manager/cert-manager/releases/latest/download/cert-manager.yaml`
- [ ] Verify cert-manager pods are running in the `cert-manager` namespace before proceeding (`kubectl get pods -n cert-manager`)
- [ ] Create a `ClusterIssuer` manifest (`infrastructure/k8s/cert-manager/cluster-issuer-staging.yaml`) pointing to the Let's Encrypt **staging** ACME endpoint (`acme-staging-v02.api.letsencrypt.org`) — use staging first to avoid the production rate limit (5 duplicate certificates per week)
- [ ] Deploy the staging `ClusterIssuer` and confirm a test certificate is issued successfully: `kubectl describe certificate tams-tls -n tams` should show `Ready: True`
- [ ] Once staging succeeds, create `infrastructure/k8s/cert-manager/cluster-issuer-prod.yaml` pointing to the Let's Encrypt **production** endpoint (`acme-v02.api.letsencrypt.org`); delete the staging certificate secret (`kubectl delete secret tams-tls -n tams`) before switching so a fresh production cert is issued
- [ ] Confirm the Ingress manifest includes both `nginx.ingress.kubernetes.io/ssl-redirect: "true"` and `nginx.ingress.kubernetes.io/force-ssl-redirect: "true"` annotations
- [ ] Verify forced HTTP-to-HTTPS redirect is active: `curl -I http://tams.example.com` must return `HTTP/1.1 301` with `Location: https://tams.example.com`
- [ ] Verify the certificate in a browser: no security warnings, correct domain, certificate issued by Let's Encrypt
- [ ] Add a note to `README.md` that cert-manager automatically renews certificates 30 days before expiry — no manual renewal process is required
- [ ] Review all services for accidental PII logging — search codebase for any log statements that might output raw TC or Öğrenci No
- [ ] Add `CONTRIBUTING.md` with instructions for running locally, environment variable setup, and branch/commit conventions
- [ ] Final review: ensure no secrets are committed to the repository (run `git log --all -p | grep -i password` as a sanity check)
