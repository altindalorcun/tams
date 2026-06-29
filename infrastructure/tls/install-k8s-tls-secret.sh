#!/usr/bin/env bash
# install-k8s-tls-secret.sh — Create or replace the tams-tls Kubernetes TLS secret.
#
# Use this for self-signed certificates or Hacettepe-provided institutional certificates.
# When using a manual secret, remove the cert-manager.io/cluster-issuer annotation from
# the Ingress manifest so cert-manager does not overwrite the secret.
#
# Usage (from repository root):
#   TLS_CERT_FILE=/path/to/fullchain.pem \
#   TLS_KEY_FILE=/path/to/privkey.pem \
#   ./infrastructure/tls/install-k8s-tls-secret.sh
#
# Environment variables:
#   TLS_CERT_FILE  — PEM-encoded certificate (required; may include intermediate chain)
#   TLS_KEY_FILE   — PEM-encoded private key (required)
#   K8S_NAMESPACE  — Target namespace (default: tams)
#   K8S_SECRET_NAME — TLS secret name (default: tams-tls)

set -euo pipefail

K8S_NAMESPACE="${K8S_NAMESPACE:-tams}"
K8S_SECRET_NAME="${K8S_SECRET_NAME:-tams-tls}"

GREEN="\033[0;32m"
RED="\033[0;31m"
RESET="\033[0m"

log()  { echo -e "${GREEN}[tls]${RESET} $*"; }
fail() { echo -e "${RED}[tls] FAIL${RESET} $*"; exit 1; }

command -v kubectl &>/dev/null || fail "'kubectl' is not installed or not in PATH."

[[ -n "${TLS_CERT_FILE:-}" ]] || fail "TLS_CERT_FILE is required."
[[ -n "${TLS_KEY_FILE:-}" ]]   || fail "TLS_KEY_FILE is required."
[[ -f "$TLS_CERT_FILE" ]]      || fail "Certificate file not found: $TLS_CERT_FILE"
[[ -f "$TLS_KEY_FILE" ]]        || fail "Private key file not found: $TLS_KEY_FILE"

if ! kubectl get namespace "$K8S_NAMESPACE" &>/dev/null; then
  fail "Namespace '$K8S_NAMESPACE' does not exist. Apply infrastructure/k8s/namespace.yaml first."
fi

log "Installing TLS secret '$K8S_SECRET_NAME' in namespace '$K8S_NAMESPACE'"

kubectl create secret tls "$K8S_SECRET_NAME" \
  --cert="$TLS_CERT_FILE" \
  --key="$TLS_KEY_FILE" \
  --namespace="$K8S_NAMESPACE" \
  --dry-run=client -o yaml | kubectl apply -f -

log "Secret '$K8S_SECRET_NAME' applied successfully."
log ""
log "Ensure the Ingress uses manual TLS (no cert-manager issuer annotation):"
log "  See infrastructure/k8s/api-gateway/ingress-manual-tls.yaml.example"
log ""
log "Verify:"
log "  kubectl get secret $K8S_SECRET_NAME -n $K8S_NAMESPACE"
log "  kubectl describe ingress tams-ingress -n $K8S_NAMESPACE"
