# TAMS — Production Deployment Guide (Hacettepe University)

This document covers every step required to go from the current local/smoke-test state to a
fully operational production deployment once Hacettepe University obtains a real domain and
a managed Kubernetes cluster.

> **HTTPS setup:** For certificate source selection (self-signed, Hacettepe CA, Let's Encrypt)
> and the ordered migration steps, see [https-migration-guide.md](./https-migration-guide.md).

---

## Prerequisites

| Requirement | Details |
|---|---|
| Domain | e.g. `tams.hacettepe.edu.tr` (or a subdomain under `hacettepe.edu.tr`) |
| Kubernetes cluster | Managed (e.g. AWS EKS, GCP GKE, Azure AKS) or bare-metal — minimum 3 nodes, 4 vCPU + 8 GB RAM each |
| Container registry | GitHub Container Registry (`ghcr.io`), Docker Hub, or private registry |
| DNS access | Ability to add an `A` record pointing the domain to the cluster's Ingress IP |
| `kubectl` access | Cluster admin context configured locally |
| `helm` (optional) | Simplifies cert-manager and ingress-nginx installation |

---

## Step 1 — Container Registry Setup

All six service images must be pushed to a registry the cluster can pull from.

```bash
# Log in to GitHub Container Registry (replace with your registry if different)
echo $GITHUB_PAT | docker login ghcr.io -u <github-username> --password-stdin

# Build and push each service (run from repo root)
# Replace <VERSION> with a semantic version or commit SHA — never use :latest in production

docker build -t ghcr.io/<org>/tams/api-gateway:<VERSION>      -f services/api-gateway/Dockerfile      .
docker build -t ghcr.io/<org>/tams/auth-service:<VERSION>     -f services/auth-service/Dockerfile     .
docker build -t ghcr.io/<org>/tams/rule-service:<VERSION>     -f services/rule-service/Dockerfile     .
docker build -t ghcr.io/<org>/tams/analysis-service:<VERSION> -f services/analysis-service/Dockerfile .
docker build -t ghcr.io/<org>/tams/parser-service:<VERSION>   -f services/parser-service/Dockerfile   services/parser-service
docker build -t ghcr.io/<org>/tams/frontend:<VERSION>         \
  --build-arg VITE_API_URL=https://tams.hacettepe.edu.tr      \
  -f frontend/Dockerfile frontend

docker push ghcr.io/<org>/tams/api-gateway:<VERSION>
docker push ghcr.io/<org>/tams/auth-service:<VERSION>
docker push ghcr.io/<org>/tams/rule-service:<VERSION>
docker push ghcr.io/<org>/tams/analysis-service:<VERSION>
docker push ghcr.io/<org>/tams/parser-service:<VERSION>
docker push ghcr.io/<org>/tams/frontend:<VERSION>
```

> **Important:** Update the `image:` field in each `infrastructure/k8s/<service>/deployment.yaml`
> from the placeholder `ghcr.io/tams/<service>:latest` to the actual versioned tag before
> applying manifests.

If the registry is private, create an `imagePullSecret` in the `tams` namespace:

```bash
kubectl create secret docker-registry ghcr-pull-secret \
  --docker-server=ghcr.io \
  --docker-username=<github-username> \
  --docker-password=<PAT> \
  -n tams
```

Then add `imagePullSecrets: [{name: ghcr-pull-secret}]` to each Deployment spec.

---

## Step 2 — Domain and DNS Configuration

1. Obtain the external IP address of the Ingress controller's LoadBalancer service after
   installing `ingress-nginx` (see Step 4):

   ```bash
   kubectl get svc -n ingress-nginx ingress-nginx-controller
   # Note the EXTERNAL-IP value
   ```

2. In the university's DNS management panel, add:

   | Type | Name | Value |
   |------|------|-------|
   | `A` | `tams.hacettepe.edu.tr` | `<EXTERNAL-IP>` |

3. Wait for DNS propagation (typically 5–30 minutes). Verify with:

   ```bash
   dig +short tams.hacettepe.edu.tr
   # Should return the Ingress external IP
   ```

---

## Step 3 — Update Domain References in Manifests

Several files contain `tams.example.com` as a placeholder. Replace all occurrences with the
real domain before applying:

```bash
# Find all placeholder references
grep -r "tams.example.com" infrastructure/k8s/

# Replace (adjust to actual domain)
find infrastructure/k8s/ -type f -name "*.yaml" \
  -exec sed -i '' 's/tams\.example\.com/tams.hacettepe.edu.tr/g' {} +
```

Files that contain the placeholder:

| File | Field |
|------|-------|
| `infrastructure/k8s/api-gateway/ingress.yaml` | `spec.rules[].host` and `spec.tls[].hosts[]` |
| `infrastructure/k8s/tams-config.yaml` | `CORS_ALLOWED_ORIGINS` |
| `infrastructure/k8s/cert-manager/cluster-issuer-staging.yaml` | `spec.acme.email` (also update email) |
| `infrastructure/k8s/cert-manager/cluster-issuer-prod.yaml` | `spec.acme.email` (also update email) |

---

## Step 4 — Install Ingress-NGINX Controller

```bash
helm repo add ingress-nginx https://kubernetes.github.io/ingress-nginx
helm repo update
helm install ingress-nginx ingress-nginx/ingress-nginx \
  --namespace ingress-nginx --create-namespace \
  --set controller.replicaCount=2
```

Verify:

```bash
kubectl get pods -n ingress-nginx
kubectl get svc -n ingress-nginx ingress-nginx-controller
```

---

## Step 5 — Install cert-manager

```bash
./infrastructure/k8s/cert-manager/install.sh
# or manually:
kubectl apply -f https://github.com/cert-manager/cert-manager/releases/latest/download/cert-manager.yaml
kubectl rollout status deployment/cert-manager -n cert-manager --timeout=120s
```

---

## Step 6 — Create Real Kubernetes Secrets

The `.yaml.example` secret templates in `infrastructure/k8s/` must be populated with real
values and applied. **Never commit real secret values to the repository.**

Generate strong values for each secret:

```bash
# JWT secret (minimum 256-bit)
JWT_SECRET=$(openssl rand -base64 32)
```

Apply each secret to the cluster (example for `tams-jwt-secret`):

```bash
kubectl create secret generic tams-jwt-secret \
  --from-literal=jwt-secret="$JWT_SECRET" \
  -n tams
```

Secrets to create (refer to `infrastructure/k8s/NOTES.md` — Section 2 for full key list):

| Secret name | Values needed |
|---|---|
| `tams-jwt-secret` | `jwt-secret` |
| `tams-db-auth` | `host`, `user`, `password` |
| `tams-db-rules` | `host`, `user`, `password` |
| `tams-db-analysis` | `host`, `user`, `password` |
| `tams-admin-creds` | `email`, `username`, `password` |

> **Recommendation:** Use a secrets manager (HashiCorp Vault, AWS Secrets Manager, or
> Kubernetes External Secrets Operator) for long-term secret management rather than
> `kubectl create secret` commands.

---

## Step 7 — Apply All Manifests (in order)

```bash
cd /path/to/tams

kubectl apply -f infrastructure/k8s/namespace.yaml

# PostgreSQL
kubectl apply -f infrastructure/k8s/postgres/

# Kafka
kubectl apply -f infrastructure/k8s/kafka/configmap.yaml
kubectl apply -f infrastructure/k8s/kafka/service.yaml
kubectl apply -f infrastructure/k8s/kafka/statefulset.yaml
kubectl wait --for=condition=ready pod/kafka-0 -n tams --timeout=180s
kubectl apply -f infrastructure/k8s/kafka/init-job.yaml

# ConfigMap (non-sensitive config)
kubectl apply -f infrastructure/k8s/tams-config.yaml

# Backend services
kubectl apply -f infrastructure/k8s/auth-service/
kubectl apply -f infrastructure/k8s/rule-service/
kubectl apply -f infrastructure/k8s/parser-service/
kubectl apply -f infrastructure/k8s/analysis-service/

# Gateway and frontend
kubectl apply -f infrastructure/k8s/api-gateway/
kubectl apply -f infrastructure/k8s/frontend/

# Ingress (last — after all backends are healthy)
kubectl apply -f infrastructure/k8s/api-gateway/ingress.yaml
```

---

## Step 8 — TLS Certificate Setup (Let's Encrypt)

> For self-signed or Hacettepe institutional certificates, follow
> [https-migration-guide.md](./https-migration-guide.md) Sections 6–7 instead of this section.

### 8a. Staging first (avoid rate limits)

```bash
# Update email in cluster-issuer-staging.yaml, then:
kubectl apply -f infrastructure/k8s/cert-manager/cluster-issuer-staging.yaml

kubectl annotate ingress tams-ingress -n tams \
  cert-manager.io/cluster-issuer=letsencrypt-staging --overwrite

# Watch for Ready: True (1–3 minutes)
kubectl get certificate tams-tls -n tams -w
```

### 8b. Switch to production

Once staging succeeds (`Ready: True`):

```bash
kubectl apply -f infrastructure/k8s/cert-manager/cluster-issuer-prod.yaml

kubectl annotate ingress tams-ingress -n tams \
  cert-manager.io/cluster-issuer=letsencrypt-prod --overwrite

kubectl delete secret tams-tls -n tams   # triggers fresh production cert issuance

kubectl get certificate tams-tls -n tams -w   # wait for Ready: True
```

---

## Step 9 — Verify Deployment

```bash
# All pods running
kubectl get pods -n tams

# Health checks
curl https://tams.hacettepe.edu.tr/actuator/health      # api-gateway
curl https://tams.hacettepe.edu.tr/api/v1/auth/...      # auth endpoints

# HTTP → HTTPS redirect
curl -I http://tams.hacettepe.edu.tr
# Expected: HTTP/1.1 301  Location: https://tams.hacettepe.edu.tr

# Run end-to-end test (requires API_URL override)
API_URL=https://tams.hacettepe.edu.tr ./docs/e2e-test.sh
```

---

## Step 10 — Post-Deployment Checklist

- [ ] All 6 pods show `Running` and `Ready 1/1` in `kubectl get pods -n tams`
- [ ] `https://tams.hacettepe.edu.tr` opens in browser with no TLS warning
- [ ] Certificate issuer is `Let's Encrypt` (check via browser lock icon)
- [ ] `curl -I http://tams.hacettepe.edu.tr` returns `301` redirect to HTTPS
- [ ] Admin can log in and create departments/courses/categories
- [ ] Teacher can upload a PDF transcript and receive analysis result
- [ ] Student can view their own result via `/student/results`
- [ ] Horizontal Pod Autoscalers are active: `kubectl get hpa -n tams`
- [ ] Monitoring/alerting configured on the cluster (Prometheus + Grafana recommended)

---

## Certificate Auto-Renewal

cert-manager automatically renews TLS certificates **30 days before expiry**. No manual
intervention is required. Each Let's Encrypt certificate is valid for 90 days; cert-manager
will renew it around day 60.

To verify the next renewal time:

```bash
kubectl describe certificate tams-tls -n tams | grep -A5 "Renewal Time"
```

---

## Updating the Application

To deploy a new version:

1. Build and push new images with a new tag (never reuse `:latest` in production).
2. Update the `image:` tag in the relevant `deployment.yaml` file.
3. Apply the updated deployment:

   ```bash
   kubectl apply -f infrastructure/k8s/<service>/deployment.yaml
   kubectl rollout status deployment/<service> -n tams
   ```

4. Roll back if needed:

   ```bash
   kubectl rollout undo deployment/<service> -n tams
   ```

---

## Estimated Resource Requirements (Minimum)

| Service | CPU Request | Memory Request | Replicas (initial) |
|---|---|---|---|
| api-gateway | 200m | 256Mi | 2 |
| auth-service | 250m | 512Mi | 1 |
| rule-service | 250m | 512Mi | 1 |
| analysis-service | 500m | 1Gi | 1 |
| parser-service | 500m | 512Mi | 1 |
| frontend | 50m | 64Mi | 1 |
| postgres × 3 | 250m each | 512Mi each | 1 (StatefulSet) |
| kafka | 500m | 1Gi | 1 (StatefulSet) |

parser-service and analysis-service have HPAs and will scale up automatically under load.

---

## Reference Files

| File | Purpose |
|------|---------|
| `infrastructure/k8s/NOTES.md` | Implementation notes, secret key reference, apply order |
| `infrastructure/k8s/cert-manager/install.sh` | cert-manager install + verification script |
| `infrastructure/k8s/cert-manager/cluster-issuer-staging.yaml` | Let's Encrypt staging ClusterIssuer |
| `infrastructure/k8s/cert-manager/cluster-issuer-prod.yaml` | Let's Encrypt production ClusterIssuer |
| `docs/e2e-test.sh` | End-to-end workflow test script |
| `docs/https-migration-guide.md` | HTTPS migration steps (self-signed, institutional, Let's Encrypt) |
| `CONTRIBUTING.md` | Local development setup guide |
