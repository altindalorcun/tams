#!/usr/bin/env bash
# renew-manual-cert.sh — Replace an existing manual tams-tls secret with renewed certificate files.
#
# Use when Hacettepe IT provides renewed .crt/.key files or when rotating a self-signed cert.
# This script deletes the old secret and recreates it; ingress-nginx picks up changes automatically.
#
# Usage (from repository root):
#   TLS_CERT_FILE=/path/to/new-fullchain.pem \
#   TLS_KEY_FILE=/path/to/new-privkey.pem \
#   ./infrastructure/tls/renew-manual-cert.sh
#
# Environment variables: same as install-k8s-tls-secret.sh

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

K8S_NAMESPACE="${K8S_NAMESPACE:-tams}"
K8S_SECRET_NAME="${K8S_SECRET_NAME:-tams-tls}"

GREEN="\033[0;32m"
YELLOW="\033[1;33m"
RED="\033[0;31m"
RESET="\033[0m"

log()  { echo -e "${GREEN}[tls]${RESET} $*"; }
warn() { echo -e "${YELLOW}[tls]${RESET} $*"; }
fail() { echo -e "${RED}[tls] FAIL${RESET} $*"; exit 1; }

command -v kubectl &>/dev/null || fail "'kubectl' is not installed or not in PATH."

if kubectl get secret "$K8S_SECRET_NAME" -n "$K8S_NAMESPACE" &>/dev/null; then
  warn "Deleting existing secret '$K8S_SECRET_NAME' in namespace '$K8S_NAMESPACE'..."
  kubectl delete secret "$K8S_SECRET_NAME" -n "$K8S_NAMESPACE"
else
  warn "Secret '$K8S_SECRET_NAME' not found; creating a new one."
fi

exec "$SCRIPT_DIR/install-k8s-tls-secret.sh"
