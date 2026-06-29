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

For HTTPS with Docker Compose, [OpenSSL](https://www.openssl.org/) must also be available on the host.

---

## Project Structure

```
tams/
├── pom.xml                  ← Maven parent POM (multi-module)
├── docs/
│   ├── architecture.md           ← Full system architecture
│   ├── https-migration-guide.md  ← HTTPS setup (Compose and Kubernetes)
│   └── production-deployment.md  ← Kubernetes production deployment
├── services/
│   ├── api-gateway/         ← Spring Cloud Gateway
│   ├── auth-service/        ← Spring Boot — authentication & authorization
│   ├── rule-service/        ← Spring Boot — graduation rule management
│   ├── analysis-service/    ← Spring Boot — transcript analysis & results
│   └── parser-service/      ← Python / FastAPI — PDF parsing & PII masking
├── frontend/                ← React + Vite + TypeScript
└── infrastructure/
    ├── docker-compose.infra.yml   ← Kafka + 3x PostgreSQL (infrastructure only)
    ├── docker-compose.yml         ← Full stack (local dev and single-server deploy)
    ├── docker-compose.https.yml   ← HTTPS override (reverse-proxy)
    ├── reverse-proxy/             ← TLS termination for Docker Compose
    ├── tls/                       ← Certificate scripts (self-signed, K8s import)
    └── k8s/                       ← Kubernetes manifests (optional)
```

---

## Running the Application

TAMS runs as a full Docker Compose stack. **Kubernetes is not required** — a single server
with Docker Compose is sufficient for most deployments.

### 1. Configure environment variables

Copy the example file to the **repository root** and fill in your values:

```bash
cp .env.example .env
```

Required values include `KAFKA_CLUSTER_ID`, database passwords, `JWT_SECRET`, and admin
seed credentials. See [`.env.example`](.env.example) for the full list.

### 2. Docker Compose — HTTP (development / internal network)

Start shared infrastructure (Kafka + databases):

```bash
make infra-up
# or: docker compose -f infrastructure/docker-compose.infra.yml --env-file .env up -d
```

Start the full stack:

```bash
make up
# or: docker compose -f infrastructure/docker-compose.yml --env-file .env up -d --build
```

Stop everything:

```bash
make down
```

Open the frontend at **http://localhost:5173** and the API gateway at **http://localhost:8080**.

### 3. Docker Compose — HTTPS (single-server, external access)

Use this path when the stack must be reachable over HTTPS (e.g. on a university server).
TLS is terminated at the `reverse-proxy` container; backend services stay on plain HTTP
inside the Docker network.

**Step 1 — Generate a self-signed certificate** (for testing; use an institutional
certificate in production):

```bash
TLS_DOMAIN=localhost ./infrastructure/tls/generate-self-signed.sh
```

**Step 2 — Set HTTPS values in `.env`:**

```bash
VITE_API_URL=https://localhost
CORS_ALLOWED_ORIGINS=https://localhost
```

**Step 3 — Start the stack with the HTTPS override:**

```bash
docker compose -f infrastructure/docker-compose.yml \
  -f infrastructure/docker-compose.https.yml \
  --env-file .env up -d --build
```

Open **https://localhost** in the browser. Accept the self-signed certificate warning
during testing.

For a Hacettepe institutional certificate, place `fullchain.pem` and `privkey.pem` under
`infrastructure/tls/generated/<your-domain>/`, set `TLS_DOMAIN` accordingly, and start
with the same override command. See [`docs/https-migration-guide.md`](docs/https-migration-guide.md)
Section 7 for import details.

### 4. Kubernetes (optional)

Use Kubernetes when you need horizontal auto-scaling, zero-downtime rolling updates, or
Let's Encrypt certificate automation via cert-manager.

- [`infrastructure/k8s/README.md`](infrastructure/k8s/README.md) — manifest apply order
- [`docs/production-deployment.md`](docs/production-deployment.md) — full production guide

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

## Service Endpoints

### HTTP mode (default)

| Service | URL |
|---------|-----|
| Frontend | http://localhost:5173 |
| api-gateway | http://localhost:8080 |
| auth-service | http://localhost:8081 |
| rule-service | http://localhost:8082 |
| analysis-service | http://localhost:8083 |
| parser-service | http://localhost:8084 |

### HTTPS mode (`docker-compose.https.yml`)

| Service | URL |
|---------|-----|
| Application (single entry point) | https://localhost |
| API (via reverse-proxy) | https://localhost/api/... |

Direct ports (8080, 5173) remain reachable internally; use **https://localhost** in the
browser when the HTTPS override is active.

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

## HTTPS Support

TLS is terminated at the **edge layer only**. Microservices communicate over plain HTTP
inside the Docker network or Kubernetes cluster.

| Layer | TLS termination |
|-------|-----------------|
| Docker Compose | `reverse-proxy` container (host port 443) |
| Kubernetes | `ingress-nginx` controller (port 443) |

### Certificate options

| Scenario | Environment | Method |
|----------|-------------|--------|
| Local / test | Docker Compose | Self-signed + `docker-compose.https.yml` |
| Single server / institutional | Docker Compose or Kubernetes | Hacettepe `.crt` / `.key` import |
| Public internet domain | Kubernetes | cert-manager + Let's Encrypt |

### Tools

| Script | Purpose |
|--------|---------|
| `infrastructure/tls/generate-self-signed.sh` | Generate self-signed PEM files |
| `infrastructure/tls/install-k8s-tls-secret.sh` | Create `tams-tls` Kubernetes secret |
| `infrastructure/tls/renew-manual-cert.sh` | Replace manual secret on certificate renewal |

For the complete step-by-step migration guide (Compose and Kubernetes paths), see
[`docs/https-migration-guide.md`](docs/https-migration-guide.md).

When using Let's Encrypt on Kubernetes, [cert-manager](https://cert-manager.io/) automatically
renews certificates **30 days before expiry** — no manual renewal is required.

---

## Security Notes

- All sensitive values (passwords, JWT secrets, PII hash salt) must come from environment variables. Never commit real secrets.
- PII (TC Kimlik No, Öğrenci No) is never stored. It is replaced with a deterministic SHA-256 hash inside `parser-service` before any data leaves that service.
- Every service endpoint requires explicit authorization. Default stance: deny all.
- Do **not** expose the Docker Compose stack to the public internet without the HTTPS override. Use `docker-compose.https.yml` with a valid TLS certificate for external access.
- Self-signed certificates are for testing only. Use a Hacettepe institutional certificate or a publicly trusted CA in production.

---

## Further Reading

| Document | Description |
|----------|-------------|
| [`docs/architecture.md`](docs/architecture.md) | Full system architecture |
| [`docs/https-migration-guide.md`](docs/https-migration-guide.md) | HTTPS migration steps (Compose and Kubernetes) |
| [`docs/production-deployment.md`](docs/production-deployment.md) | Kubernetes production deployment |
| [`infrastructure/k8s/README.md`](infrastructure/k8s/README.md) | Kubernetes manifest apply order |
| [`.env.example`](.env.example) | All environment variables |
