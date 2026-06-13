# TAMS — Transkript Analiz ve Mezuniyet Kontrol Sistemi

A microservices-based system that allows university teachers to upload student PDF transcripts, automatically parse them, compare extracted course data against admin-defined graduation rules, and produce eligibility reports — without permanently storing any personally identifiable information (PII).

---

## Architecture Overview

```
Client (React)
    │
    ▼
api-gateway          ← Single entry point; JWT validation, routing, rate limiting
    │
    ├──▶ auth-service       ← User management, JWT issuance, role assignment
    ├──▶ rule-service       ← Graduation rule CRUD (categories, courses, credits)
    └──▶ analysis-service   ← PDF upload, Kafka publish, graduation engine, results
                                        │
                            Kafka (KRaft)
                                        │
                            parser-service (Python/FastAPI)
                                ← Consumes PDF bytes, parses transcript, masks PII,
                                   publishes structured course data back to Kafka
```

**User roles:** Admin (manage rules), Teacher (upload transcripts, view results), Student (view own result)

For a full architecture description see [`docs/architecture.md`](docs/architecture.md).

---

## Prerequisites

| Tool | Version |
|------|---------|
| Java | 21 |
| Maven | 3.9+ |
| Python | 3.12 |
| Node.js | 22 |
| Docker & Docker Compose | Latest |

---

## Project Structure

```
tams/
├── pom.xml                  ← Maven parent POM (multi-module)
├── docs/                    ← Architecture document and roadmap
├── services/
│   ├── api-gateway/         ← Spring Cloud Gateway
│   ├── auth-service/        ← Spring Boot — authentication & authorization
│   ├── rule-service/        ← Spring Boot — graduation rule management
│   ├── analysis-service/    ← Spring Boot — transcript analysis & results
│   └── parser-service/      ← Python / FastAPI — PDF parsing & PII masking
├── frontend/                ← React + Vite + TypeScript
└── infrastructure/
    ├── docker-compose.infra.yml  ← Kafka + 3x PostgreSQL (infrastructure only)
    ├── docker-compose.yml        ← Full stack for local integration testing
    └── k8s/                      ← Kubernetes manifests
```

---

## Running Locally

### 1. Configure environment variables

Copy the example file and fill in your values:

```bash
cp .env.example .env
```

### 2. Start shared infrastructure (Kafka + databases)

```bash
make infra-up
# or: docker compose -f infrastructure/docker-compose.infra.yml up -d
```

### 3. Start all services (full stack)

```bash
make up
# or: docker compose -f infrastructure/docker-compose.yml up -d
```

### 4. Stop everything

```bash
make down
```

---

## Building

### All Java services (from repo root)

```bash
mvn package -DskipTests
```

### A single Java service

```bash
mvn -pl services/auth-service -am package -DskipTests
```

### Python parser-service

```bash
cd services/parser-service
python -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt
uvicorn src.main:app --reload
```

### Frontend

```bash
cd frontend
npm install
npm run dev
```

---

## Service Endpoints (local)

| Service | URL |
|---------|-----|
| api-gateway | http://localhost:8080 |
| auth-service | http://localhost:8081 |
| rule-service | http://localhost:8082 |
| analysis-service | http://localhost:8083 |
| parser-service | http://localhost:8084 |
| Frontend | http://localhost:5173 |

### Health checks

- Spring Boot services: `GET /actuator/health`
- Python parser-service: `GET /health`

### API Documentation

- Spring Boot services: `/swagger-ui.html`
- Python parser-service: `/docs`

---

## Environment Variables

See [`.env.example`](.env.example) for the full list of required variables with descriptions.

---

## Security Notes

- All sensitive values (passwords, JWT secrets, PII hash salt) must come from environment variables. Never commit real secrets.
- PII (TC Kimlik No, Öğrenci No) is never stored. It is replaced with a deterministic SHA-256 hash inside `parser-service` before any data leaves that service.
- Every service endpoint requires explicit authorization. Default stance: deny all.

## TLS Certificate Management

HTTPS is enforced by the Kubernetes Ingress (`nginx.ingress.kubernetes.io/force-ssl-redirect: "true"`). TLS certificates are issued and managed by [cert-manager](https://cert-manager.io/) with Let's Encrypt.

**cert-manager automatically renews certificates 30 days before expiry — no manual renewal process is required.**

To set up TLS on a new cluster, follow the steps in [`infrastructure/k8s/NOTES.md`](infrastructure/k8s/NOTES.md) (sections P9-8 and P9-11).
