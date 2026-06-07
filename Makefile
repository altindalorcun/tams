.PHONY: infra-up infra-down up down build-all logs-infra logs help

# All docker compose commands read .env from the repo root.
COMPOSE_INFRA := docker compose -f infrastructure/docker-compose.infra.yml --env-file .env
COMPOSE_FULL  := docker compose -f infrastructure/docker-compose.yml --env-file .env

# ─── Infrastructure only (Kafka + PostgreSQL) ─────────────────────────────────

infra-up:
	$(COMPOSE_INFRA) up -d
	@echo "Infrastructure started. Kafka UI: localhost:9092 | DBs: 5432-5434"

infra-down:
	$(COMPOSE_INFRA) down

# ─── Full stack (infra + all services + frontend) ─────────────────────────────

up:
	$(COMPOSE_FULL) up -d --build

down:
	$(COMPOSE_FULL) down

# ─── Build ────────────────────────────────────────────────────────────────────

build-all:
	mvn package -DskipTests

# ─── Logs ─────────────────────────────────────────────────────────────────────

logs-infra:
	$(COMPOSE_INFRA) logs -f

logs:
	$(COMPOSE_FULL) logs -f

# ─── Help ─────────────────────────────────────────────────────────────────────

help:
	@echo ""
	@echo "Usage: make <target>"
	@echo ""
	@echo "  infra-up    Start shared infrastructure (Kafka + 3x PostgreSQL)"
	@echo "  infra-down  Stop shared infrastructure"
	@echo "  up          Build and start the full stack"
	@echo "  down        Stop the full stack"
	@echo "  build-all   Build all Java services (mvn package -DskipTests)"
	@echo "  logs-infra  Tail infra container logs"
	@echo "  logs        Tail all container logs"
	@echo ""
	@echo "Prerequisites: copy .env.example → .env and fill in values first."
	@echo ""
