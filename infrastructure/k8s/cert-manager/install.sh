#!/usr/bin/env bash
# install.sh — Install cert-manager into the cluster and verify the pods are running.
#
# This script covers Phase 9 items:
#   • Install cert-manager (kubectl apply from the official release manifest)
#   • Verify cert-manager pods are running in the cert-manager namespace
#
# Usage (run from the repository root):
#   chmod +x infrastructure/k8s/cert-manager/install.sh
#   ./infrastructure/k8s/cert-manager/install.sh
#
# Prerequisites:
#   - kubectl is installed and configured to point at the target cluster
#   - The tams namespace already exists (kubectl apply -f infrastructure/k8s/namespace.yaml)
#
# After this script completes successfully:
#   1. Apply the staging ClusterIssuer and request a test certificate:
#        kubectl apply -f infrastructure/k8s/cert-manager/cluster-issuer-staging.yaml
#   2. Once a staging certificate is confirmed, switch to production:
#        kubectl apply -f infrastructure/k8s/cert-manager/cluster-issuer-prod.yaml
#        kubectl delete secret tams-tls -n tams   # force fresh prod cert issuance

set -euo pipefail

CERT_MANAGER_VERSION="${CERT_MANAGER_VERSION:-latest}"
CERT_MANAGER_URL="https://github.com/cert-manager/cert-manager/releases/${CERT_MANAGER_VERSION}/download/cert-manager.yaml"
READY_TIMEOUT="${READY_TIMEOUT:-120s}"

GREEN="\033[0;32m"
YELLOW="\033[1;33m"
RED="\033[0;31m"
RESET="\033[0m"

log()  { echo -e "${GREEN}[cert-manager]${RESET} $*"; }
warn() { echo -e "${YELLOW}[cert-manager]${RESET} $*"; }
fail() { echo -e "${RED}[cert-manager] FAIL${RESET} $*"; exit 1; }

# ── Prerequisite check ─────────────────────────────────────────────────────────
command -v kubectl &>/dev/null || fail "'kubectl' is not installed or not in PATH."

log "Installing cert-manager from: $CERT_MANAGER_URL"

# ── Step 1: Apply cert-manager CRDs and controllers ───────────────────────────
kubectl apply -f "$CERT_MANAGER_URL"
log "cert-manager manifest applied."

# ── Step 2: Wait for cert-manager pods to be ready ────────────────────────────
log "Waiting for cert-manager pods to be ready (timeout: $READY_TIMEOUT)..."

# The three core deployments cert-manager creates:
for deploy in cert-manager cert-manager-cainjector cert-manager-webhook; do
  log "  Waiting for deployment/$deploy ..."
  kubectl rollout status deployment/"$deploy" \
    -n cert-manager \
    --timeout="$READY_TIMEOUT"
done

log "All cert-manager deployments are ready."

# ── Step 3: Verify pods ────────────────────────────────────────────────────────
log "Current cert-manager pod status:"
kubectl get pods -n cert-manager

# ── Done ───────────────────────────────────────────────────────────────────────
echo ""
log "cert-manager is installed and running."
log ""
log "Next steps:"
log "  1. Apply the staging ClusterIssuer (avoids Let's Encrypt rate limits):"
log "       kubectl apply -f infrastructure/k8s/cert-manager/cluster-issuer-staging.yaml"
log "  2. Verify a test certificate is issued:"
log "       kubectl describe certificate tams-tls -n tams"
log "  3. Once staging succeeds, apply the production ClusterIssuer:"
log "       kubectl apply -f infrastructure/k8s/cert-manager/cluster-issuer-prod.yaml"
log "       kubectl delete secret tams-tls -n tams"
