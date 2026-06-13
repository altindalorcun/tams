# Contributing to TAMS

This guide explains how to set up a local development environment, configure required environment variables, and follow the project's branch and commit conventions.

---

## Table of Contents

1. [Prerequisites](#1-prerequisites)
2. [Environment Variable Setup](#2-environment-variable-setup)
3. [Running the Stack Locally](#3-running-the-stack-locally)
4. [Running Individual Services](#4-running-individual-services)
5. [Running Tests](#5-running-tests)
6. [Branch Conventions](#6-branch-conventions)
7. [Commit Conventions](#7-commit-conventions)
8. [Pull Request Checklist](#8-pull-request-checklist)

---

## 1. Prerequisites

| Tool | Version | Install |
|------|---------|---------|
| Java | 21 | `brew install openjdk@21` |
| Maven | 3.9+ | `brew install maven` |
| Python | 3.12 | `brew install python@3.12` |
| Node.js | 22 | `brew install node@22` |
| Docker & Docker Compose | Latest | [docker.com](https://www.docker.com/products/docker-desktop/) |
| `curl` + `jq` | Any | `brew install curl jq` (needed for `docs/e2e-test.sh`) |

---

## 2. Environment Variable Setup

All secrets and configuration values come from a `.env` file that is never committed.

```bash
# 1. Copy the example file
cp .env.example .env

# 2. Generate required secrets and fill them in
#    JWT secret (minimum 256-bit):
openssl rand -base64 32

#    PII hash salt (must not change after first use — existing hashes would break):
openssl rand -base64 32

#    Kafka Cluster ID (generate once, keep stable):
docker run --rm confluentinc/cp-kafka:7.8.0 kafka-storage random-uuid

# 3. Edit .env with the generated values
```

Key variables to fill in:

| Variable | Description |
|----------|-------------|
| `JWT_SECRET` | HMAC-SHA256 signing key — min 32 bytes |
| `PII_HASH_SALT` | Salt for TC / Öğrenci No hashing — never change after first use |
| `KAFKA_CLUSTER_ID` | Base64-encoded UUID for KRaft Kafka |
| `POSTGRES_*_PASSWORD` | Database passwords for the three PostgreSQL instances |
| `ADMIN_SEED_EMAIL/USERNAME/PASSWORD` | Initial admin account created on first startup |

---

## 3. Running the Stack Locally

### Infrastructure only (Kafka + databases)

```bash
make infra-up
# or:
docker compose -f infrastructure/docker-compose.infra.yml up -d
```

### Full stack (all services + frontend)

```bash
make up
# or:
docker compose -f infrastructure/docker-compose.yml up -d
```

### Stop everything

```bash
make down
```

### Verify all services are healthy

```bash
curl -s http://localhost:8080/actuator/health | jq .   # api-gateway
curl -s http://localhost:8081/actuator/health | jq .   # auth-service
curl -s http://localhost:8082/actuator/health | jq .   # rule-service
curl -s http://localhost:8083/actuator/health | jq .   # analysis-service
curl -s http://localhost:8000/health                   # parser-service
```

### Run the end-to-end test

```bash
# Requires the full stack to be running
./docs/e2e-test.sh
```

---

## 4. Running Individual Services

### Java services (Spring Boot)

```bash
# Build a single service (skip tests)
mvn -pl services/auth-service -am package -DskipTests

# Build all Java services
mvn package -DskipTests

# Run directly (DB + Kafka must be up from make infra-up)
java -jar services/auth-service/target/auth-service-*.jar
```

### Python parser-service

```bash
cd services/parser-service
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt

# Start (Kafka must be running; set ENABLE_CONSUMER=false to skip consumer for local dev)
ENABLE_CONSUMER=false uvicorn src.main:app --reload --port 8000
```

### Frontend

```bash
cd frontend
npm install
npm run dev   # starts at http://localhost:5173
```

### API Documentation (local)

| Service | Swagger UI |
|---------|------------|
| auth-service | http://localhost:8081/swagger-ui.html |
| rule-service | http://localhost:8082/swagger-ui.html |
| analysis-service | http://localhost:8083/swagger-ui.html |
| api-gateway | http://localhost:8080/swagger-ui.html |
| parser-service | http://localhost:8000/docs |

---

## 5. Running Tests

### Java unit and integration tests

```bash
# All tests (requires Docker for Testcontainers)
mvn test

# Single service
mvn -pl services/auth-service test
```

### Python tests

```bash
cd services/parser-service
source .venv/bin/activate
pytest tests/ -v
```

### Frontend tests

```bash
cd frontend
npm run test
```

---

## 6. Branch Conventions

| Prefix | Use for |
|--------|---------|
| `feature/` | New features |
| `fix/` | Bug fixes |
| `chore/` | Maintenance, dependency updates, tooling |
| `docs/` | Documentation only |
| `refactor/` | Code restructuring without behavior change |

Branch name format: `<prefix>/<short-description-in-kebab-case>`

```
feature/student-result-export
fix/pii-masker-null-student-ref
chore/update-spring-boot-3.4
```

---

## 7. Commit Conventions

Use the [Conventional Commits](https://www.conventionalcommits.org/) format:

```
<type>(<scope>): <short description>

[optional body]
```

**Types:** `feat`, `fix`, `chore`, `docs`, `refactor`, `test`, `style`, `ci`

**Scopes:** service name or area (`auth`, `rule`, `analysis`, `parser`, `gateway`, `frontend`, `k8s`, `infra`)

Examples:

```
feat(auth): add refresh token rotation on login
fix(parser): handle null grade field in transcript row
chore(deps): update logstash-logback-encoder to 8.1
docs(k8s): add cert-manager setup notes to NOTES.md
test(analysis): add graduation engine edge case for zero credit category
```

Rules:
- Subject line ≤ 72 characters
- Write in **English** (user-visible UI strings in the frontend may be Turkish)
- Use the imperative mood: "add" not "added", "fix" not "fixed"

---

## 8. Pull Request Checklist

Before opening a PR, ensure:

- [ ] All unit and integration tests pass (`mvn test` / `pytest`)
- [ ] No new linter errors introduced
- [ ] No secrets or `.env` file committed (`git diff --name-only | grep -v ".example"`)
- [ ] New endpoints have Swagger / OpenAPI documentation
- [ ] New Spring Boot services have `logback-spring.xml` and `spring-boot-starter-actuator`
- [ ] Any new environment variables are added to `.env.example` with a comment
- [ ] Database schema changes are in a new Flyway migration file (`V{n}__description.sql`)
- [ ] Kubernetes manifests updated if a new service or config is added
