#!/usr/bin/env bash
# smoke-test.sh — Deploys the full TAMS stack on a local Kubernetes cluster (Minikube or kind)
# and verifies every service health endpoint is reachable.
#
# Usage:
#   chmod +x infrastructure/k8s/smoke-test.sh
#   ./infrastructure/k8s/smoke-test.sh [--driver minikube|kind] [--skip-build] [--teardown]
#
# Run from the repository root (tams/).

set -euo pipefail

# ── Constants ──────────────────────────────────────────────────────────────────
REPO_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
K8S_DIR="$REPO_ROOT/infrastructure/k8s"
NAMESPACE="tams"
DRIVER="${DRIVER:-minikube}"
SKIP_BUILD="${SKIP_BUILD:-false}"
TEARDOWN="${TEARDOWN:-false}"

IMAGE_TAG="smoke"
IMAGES=(
  "api-gateway:services/api-gateway/Dockerfile"
  "auth-service:services/auth-service/Dockerfile"
  "rule-service:services/rule-service/Dockerfile"
  "analysis-service:services/analysis-service/Dockerfile"
  "parser-service:services/parser-service/Dockerfile"
  "frontend:frontend/Dockerfile"
)

GREEN="\033[0;32m"
YELLOW="\033[1;33m"
RED="\033[0;31m"
RESET="\033[0m"

log()  { echo -e "${GREEN}[smoke-test]${RESET} $*"; }
warn() { echo -e "${YELLOW}[smoke-test]${RESET} $*"; }
fail() { echo -e "${RED}[smoke-test] FAIL${RESET} $*"; exit 1; }

# ── Argument parsing ───────────────────────────────────────────────────────────
for arg in "$@"; do
  case $arg in
    --driver=*) DRIVER="${arg#*=}" ;;
    --skip-build) SKIP_BUILD=true ;;
    --teardown) TEARDOWN=true ;;
  esac
done

# ── Prerequisite checks ────────────────────────────────────────────────────────
check_prerequisites() {
  log "Checking prerequisites..."
  for cmd in kubectl docker; do
    command -v "$cmd" &>/dev/null || fail "'$cmd' is not installed."
  done

  if [[ "$DRIVER" == "minikube" ]]; then
    command -v minikube &>/dev/null || fail "'minikube' is not installed. See https://minikube.sigs.k8s.io/docs/start/"
  elif [[ "$DRIVER" == "kind" ]]; then
    command -v kind &>/dev/null || fail "'kind' is not installed. See https://kind.sigs.k8s.io/docs/user/quick-start/"
  else
    fail "Unknown driver '$DRIVER'. Use 'minikube' or 'kind'."
  fi
  log "All prerequisites found."
}

# ── Cluster setup ──────────────────────────────────────────────────────────────
start_cluster() {
  if [[ "$DRIVER" == "minikube" ]]; then
    if minikube status --profile tams-smoke &>/dev/null; then
      log "Minikube cluster 'tams-smoke' already running."
    else
      log "Starting Minikube cluster..."
      minikube start --profile tams-smoke --cpus 4 --memory 6144 --driver docker
    fi
    kubectl config use-context tams-smoke

  elif [[ "$DRIVER" == "kind" ]]; then
    if kind get clusters 2>/dev/null | grep -q "^tams-smoke$"; then
      log "kind cluster 'tams-smoke' already exists."
    else
      log "Creating kind cluster..."
      kind create cluster --name tams-smoke
    fi
    kubectl cluster-info --context kind-tams-smoke
    kubectl config use-context kind-tams-smoke
  fi
  log "Cluster is ready."
}

# ── Build Docker images ────────────────────────────────────────────────────────
build_images() {
  if [[ "$SKIP_BUILD" == "true" ]]; then
    warn "Skipping image build (--skip-build)."
    return
  fi

  log "Building Docker images (tag: $IMAGE_TAG)..."

  for entry in "${IMAGES[@]}"; do
    name="${entry%%:*}"
    dockerfile_path="${entry##*:}"
    image_name="ghcr.io/tams/${name}:${IMAGE_TAG}"

    log "  Building $image_name..."
    if [[ "$name" == "parser-service" ]]; then
      docker build -t "$image_name" \
        -f "$REPO_ROOT/$dockerfile_path" \
        "$REPO_ROOT/services/parser-service"
    elif [[ "$name" == "frontend" ]]; then
      docker build -t "$image_name" \
        --build-arg VITE_API_URL="http://localhost:8080" \
        -f "$REPO_ROOT/$dockerfile_path" \
        "$REPO_ROOT/frontend"
    else
      docker build -t "$image_name" \
        -f "$REPO_ROOT/$dockerfile_path" \
        "$REPO_ROOT"
    fi
    log "  Built $image_name."
  done
  log "All images built."
}

# ── Load images into cluster ───────────────────────────────────────────────────
load_images() {
  log "Loading images into cluster..."
  for entry in "${IMAGES[@]}"; do
    name="${entry%%:*}"
    image_name="ghcr.io/tams/${name}:${IMAGE_TAG}"

    if [[ "$DRIVER" == "minikube" ]]; then
      minikube image load "$image_name" --profile tams-smoke
    elif [[ "$DRIVER" == "kind" ]]; then
      kind load docker-image "$image_name" --name tams-smoke
    fi
    log "  Loaded $image_name."
  done
  log "All images loaded."
}

# ── Patch image tags in manifests (in-memory, no file modification) ────────────
apply_manifests_with_smoke_tag() {
  # Server-side apply is used throughout for true idempotency across repeated runs.
  # --force-conflicts ensures re-runs don't fail on field-manager conflicts.
  ksa() { kubectl apply --server-side --force-conflicts "$@"; }

  # Replace :latest tag with :smoke and apply via server-side apply
  apply_patched() {
    local file="$1"
    sed "s|:latest|:${IMAGE_TAG}|g" "$file" | kubectl apply --server-side --force-conflicts -f -
  }

  log "Step 1 — Namespace..."
  ksa -f "$K8S_DIR/namespace.yaml"

  log "Step 2 — Smoke secrets..."
  create_smoke_secrets

  log "Step 3 — ConfigMap..."
  ksa -f "$K8S_DIR/tams-config.yaml"

  log "Step 4 — PostgreSQL..."
  ksa -f "$K8S_DIR/postgres/postgres-auth-pvc.yaml"
  ksa -f "$K8S_DIR/postgres/postgres-rules-pvc.yaml"
  ksa -f "$K8S_DIR/postgres/postgres-analysis-pvc.yaml"
  ksa -f "$K8S_DIR/postgres/postgres-auth-statefulset.yaml"
  ksa -f "$K8S_DIR/postgres/postgres-rules-statefulset.yaml"
  ksa -f "$K8S_DIR/postgres/postgres-analysis-statefulset.yaml"
  ksa -f "$K8S_DIR/postgres/postgres-auth-service.yaml"
  ksa -f "$K8S_DIR/postgres/postgres-rules-service.yaml"
  ksa -f "$K8S_DIR/postgres/postgres-analysis-service.yaml"
  wait_rollout statefulset postgres-auth
  wait_rollout statefulset postgres-rules
  wait_rollout statefulset postgres-analysis

  log "Step 5 — Kafka..."
  # Delete any pre-existing Kafka StatefulSet and PVC so each run starts with a clean volume.
  # Without this, the metadata log retains old broker incarnations that trigger
  # DuplicateBrokerRegistrationException on restart, causing CrashLoopBackOff delays.
  kubectl delete statefulset kafka -n "$NAMESPACE" --ignore-not-found=true
  kubectl delete pvc kafka-data-kafka-0 -n "$NAMESPACE" --ignore-not-found=true
  kubectl wait --for=delete pvc/kafka-data-kafka-0 -n "$NAMESPACE" --timeout=60s 2>/dev/null || true
  ksa -f "$K8S_DIR/kafka/configmap.yaml"
  ksa -f "$K8S_DIR/kafka/service.yaml"
  ksa -f "$K8S_DIR/kafka/statefulset.yaml"
  log "  Waiting for statefulset/kafka (up to 360s)..."
  kubectl rollout status statefulset/kafka -n "$NAMESPACE" --timeout=360s
  # Delete a completed/failed init job before re-applying so the Job can be re-run.
  kubectl delete job kafka-init -n "$NAMESPACE" --ignore-not-found=true
  ksa -f "$K8S_DIR/kafka/init-job.yaml"
  kubectl wait --for=condition=complete job/kafka-init -n "$NAMESPACE" --timeout=120s

  log "Step 6 — Backend services..."
  for svc in auth-service rule-service parser-service analysis-service; do
    apply_patched "$K8S_DIR/$svc/deployment.yaml"
    ksa -f "$K8S_DIR/$svc/service.yaml"
  done
  for svc in parser-service analysis-service; do
    ksa -f "$K8S_DIR/$svc/hpa.yaml"
  done
  wait_rollout deployment auth-service
  wait_rollout deployment rule-service
  wait_rollout deployment parser-service
  wait_rollout deployment analysis-service

  log "Step 7 — api-gateway and frontend..."
  apply_patched "$K8S_DIR/api-gateway/deployment.yaml"
  ksa -f "$K8S_DIR/api-gateway/service.yaml"
  ksa -f "$K8S_DIR/api-gateway/hpa.yaml"
  apply_patched "$K8S_DIR/frontend/deployment.yaml"
  ksa -f "$K8S_DIR/frontend/service.yaml"
  wait_rollout deployment api-gateway
  wait_rollout deployment frontend
}

# ── Create smoke-test secrets with dummy values ────────────────────────────────
create_smoke_secrets() {
  kubectl apply --server-side --force-conflicts -n "$NAMESPACE" -f - <<EOF
apiVersion: v1
kind: Secret
metadata:
  name: tams-jwt-secret
  namespace: $NAMESPACE
type: Opaque
stringData:
  jwt-secret: "smoke-test-jwt-secret-minimum-256bit-placeholder-value-here-xx"
---
apiVersion: v1
kind: Secret
metadata:
  name: tams-pii-salt
  namespace: $NAMESPACE
type: Opaque
stringData:
  PII_HASH_SALT: "smoke-test-pii-salt-minimum-256bit-placeholder-value-here-xxx"
---
apiVersion: v1
kind: Secret
metadata:
  name: tams-admin-creds
  namespace: $NAMESPACE
type: Opaque
stringData:
  email: "admin@smoke.local"
  username: "admin"
  password: "SmokeTestPassword1!"
---
apiVersion: v1
kind: Secret
metadata:
  name: tams-db-auth
  namespace: $NAMESPACE
type: Opaque
stringData:
  host: "postgres-auth"
  user: "tams_auth_user"
  password: "smoke_auth_pass"
---
apiVersion: v1
kind: Secret
metadata:
  name: tams-db-rules
  namespace: $NAMESPACE
type: Opaque
stringData:
  host: "postgres-rules"
  user: "tams_rules_user"
  password: "smoke_rules_pass"
---
apiVersion: v1
kind: Secret
metadata:
  name: tams-db-analysis
  namespace: $NAMESPACE
type: Opaque
stringData:
  host: "postgres-analysis"
  user: "tams_analysis_user"
  password: "smoke_analysis_pass"
EOF
}

# ── Wait helper ────────────────────────────────────────────────────────────────
wait_rollout() {
  local kind="$1"
  local name="$2"
  log "  Waiting for $kind/$name..."
  kubectl rollout status "$kind/$name" -n "$NAMESPACE" --timeout=180s
}

# ── Health checks ──────────────────────────────────────────────────────────────
run_health_checks() {
  log "Running health checks..."

  if [[ "$DRIVER" == "minikube" ]]; then
    CLUSTER_IP=$(minikube ip --profile tams-smoke)
  else
    CLUSTER_IP="localhost"
  fi

  check_health() {
    local label="$1"
    local svc="$2"
    local port="$3"
    local path="$4"

    local url
    # Port-forward in background, check, then kill
    kubectl port-forward "svc/$svc" "$port:$port" -n "$NAMESPACE" &>/dev/null &
    local pf_pid=$!
    sleep 2

    if curl -sf "http://localhost:${port}${path}" -o /dev/null; then
      log "  ${GREEN}PASS${RESET} $label ($url)"
    else
      warn "  ${RED}FAIL${RESET} $label — GET http://localhost:${port}${path} returned non-2xx"
    fi
    kill "$pf_pid" 2>/dev/null || true
    sleep 1
  }

  check_health "auth-service"     auth-service     8081 "/actuator/health"
  check_health "rule-service"     rule-service     8082 "/actuator/health"
  check_health "analysis-service" analysis-service 8083 "/actuator/health"
  check_health "parser-service"   parser-service   8000 "/health"
  check_health "api-gateway"      api-gateway      8080 "/actuator/health"
  check_health "frontend"         frontend         80   "/"
}

# ── Teardown ───────────────────────────────────────────────────────────────────
teardown() {
  log "Tearing down smoke-test cluster..."
  if [[ "$DRIVER" == "minikube" ]]; then
    minikube delete --profile tams-smoke
  elif [[ "$DRIVER" == "kind" ]]; then
    kind delete cluster --name tams-smoke
  fi
  log "Cluster deleted."
}

# ── Main ───────────────────────────────────────────────────────────────────────
main() {
  log "=== TAMS Smoke Test (driver: $DRIVER) ==="

  if [[ "$TEARDOWN" == "true" ]]; then
    teardown
    exit 0
  fi

  check_prerequisites
  start_cluster
  build_images
  load_images
  apply_manifests_with_smoke_tag
  run_health_checks

  log ""
  log "=== Smoke test complete ==="
  log "All services deployed. Run the following to inspect the cluster:"
  log "  kubectl get pods -n $NAMESPACE"
  log "  kubectl get services -n $NAMESPACE"
  log ""
  log "To tear down: $0 --teardown --driver=$DRIVER"
}

main "$@"
