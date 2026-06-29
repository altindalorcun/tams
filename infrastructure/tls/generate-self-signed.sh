#!/usr/bin/env bash
# generate-self-signed.sh — Create a self-signed TLS certificate for local or staging use.
#
# Outputs fullchain.pem and privkey.pem under infrastructure/tls/generated/<domain>/.
# These files are git-ignored; never commit them.
#
# Usage (from repository root):
#   TLS_DOMAIN=localhost ./infrastructure/tls/generate-self-signed.sh
#   TLS_DOMAIN=tams.hacettepe.edu.tr ./infrastructure/tls/generate-self-signed.sh
#
# Environment variables:
#   TLS_DOMAIN   — Common Name and SAN (default: localhost)
#   TLS_DAYS     — Certificate validity in days (default: 365)
#   TLS_OUTPUT_DIR — Override output directory (default: infrastructure/tls/generated/<domain>)

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

TLS_DOMAIN="${TLS_DOMAIN:-localhost}"
TLS_DAYS="${TLS_DAYS:-365}"
TLS_OUTPUT_DIR="${TLS_OUTPUT_DIR:-$SCRIPT_DIR/generated/$TLS_DOMAIN}"

GREEN="\033[0;32m"
YELLOW="\033[1;33m"
RED="\033[0;31m"
RESET="\033[0m"

log()  { echo -e "${GREEN}[tls]${RESET} $*"; }
warn() { echo -e "${YELLOW}[tls]${RESET} $*"; }
fail() { echo -e "${RED}[tls] FAIL${RESET} $*"; exit 1; }

command -v openssl &>/dev/null || fail "'openssl' is not installed or not in PATH."

mkdir -p "$TLS_OUTPUT_DIR"

FULLCHAIN="$TLS_OUTPUT_DIR/fullchain.pem"
PRIVKEY="$TLS_OUTPUT_DIR/privkey.pem"
CSR="$TLS_OUTPUT_DIR/request.csr"
OPENSSL_CNF="$TLS_OUTPUT_DIR/openssl.cnf"

if [[ -f "$FULLCHAIN" || -f "$PRIVKEY" ]]; then
  warn "Existing certificate files found in $TLS_OUTPUT_DIR"
  warn "Delete them first if you want a fresh certificate."
fi

cat > "$OPENSSL_CNF" <<EOF
[req]
default_bits = 2048
prompt = no
default_md = sha256
distinguished_name = dn
req_extensions = req_ext

[dn]
CN = ${TLS_DOMAIN}

[req_ext]
subjectAltName = @alt_names

[alt_names]
DNS.1 = ${TLS_DOMAIN}
EOF

if [[ "$TLS_DOMAIN" == "localhost" ]]; then
  cat >> "$OPENSSL_CNF" <<EOF
DNS.2 = localhost
IP.1 = 127.0.0.1
EOF
fi

log "Generating self-signed certificate for domain: $TLS_DOMAIN"
log "Output directory: $TLS_OUTPUT_DIR"

openssl req -x509 -nodes -newkey rsa:2048 \
  -keyout "$PRIVKEY" \
  -out "$FULLCHAIN" \
  -days "$TLS_DAYS" \
  -config "$OPENSSL_CNF" \
  -extensions req_ext

chmod 600 "$PRIVKEY"
chmod 644 "$FULLCHAIN"
rm -f "$CSR" "$OPENSSL_CNF"

log "Created:"
log "  Certificate: $FULLCHAIN"
log "  Private key: $PRIVKEY"
log ""
log "Next steps:"
log "  Docker Compose HTTPS:"
log "    TLS_DOMAIN=$TLS_DOMAIN docker compose -f infrastructure/docker-compose.yml \\"
log "      -f infrastructure/docker-compose.https.yml up --build"
log ""
log "  Kubernetes:"
log "    TLS_CERT_FILE=$FULLCHAIN TLS_KEY_FILE=$PRIVKEY \\"
log "      ./infrastructure/tls/install-k8s-tls-secret.sh"
