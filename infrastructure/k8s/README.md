# TAMS — Kubernetes Deployment Guide

This guide explains how to deploy the full TAMS stack onto a Kubernetes cluster.
Apply manifests in the exact order shown below; each step depends on the previous one being healthy.

---

## Prerequisites

- `kubectl` configured and pointing at your target cluster
- A running Kubernetes cluster (Minikube, kind, or a managed cluster)
- Metrics Server installed (required by HPA):
  ```bash
  kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml
  ```
- `cert-manager` installed (required by Ingress TLS — see Phase 9 in `docs/todo.md`):
  ```bash
  kubectl apply -f https://github.com/cert-manager/cert-manager/releases/latest/download/cert-manager.yaml
  ```
- An nginx Ingress controller installed in the cluster.

---

## Step 1 — Namespace

```bash
kubectl apply -f namespace.yaml
```

Verify:
```bash
kubectl get namespace tams
```

---

## Step 2 — Secrets

Real secrets are **never committed to the repository**. Copy each `.yaml.example` file,
fill in the actual values, and apply. Delete the filled-in copies immediately after applying.

```bash
# 1. JWT secret (shared by api-gateway, auth-service, rule-service, analysis-service)
cp tams-jwt-secret.yaml.example tams-jwt-secret.yaml
# Edit tams-jwt-secret.yaml — set jwt-secret
kubectl apply -f tams-jwt-secret.yaml
rm tams-jwt-secret.yaml

# 2. PII hash salt (parser-service only — never change after first use)
cp tams-pii-salt.yaml.example tams-pii-salt.yaml
# Edit tams-pii-salt.yaml — set PII_HASH_SALT
kubectl apply -f tams-pii-salt.yaml
rm tams-pii-salt.yaml

# 3. Admin seed credentials (auth-service first-run only)
cp tams-admin-creds.yaml.example tams-admin-creds.yaml
# Edit tams-admin-creds.yaml — set email, username, password
kubectl apply -f tams-admin-creds.yaml
rm tams-admin-creds.yaml

# 4. PostgreSQL credentials (one per database)
cp postgres/postgres-auth-secret.yaml.example postgres/postgres-auth-secret.yaml
cp postgres/postgres-rules-secret.yaml.example postgres/postgres-rules-secret.yaml
cp postgres/postgres-analysis-secret.yaml.example postgres/postgres-analysis-secret.yaml
# Edit each file — set host, user, password
kubectl apply -f postgres/postgres-auth-secret.yaml
kubectl apply -f postgres/postgres-rules-secret.yaml
kubectl apply -f postgres/postgres-analysis-secret.yaml
rm postgres/postgres-auth-secret.yaml postgres/postgres-rules-secret.yaml postgres/postgres-analysis-secret.yaml
```

Verify:
```bash
kubectl get secrets -n tams
```

---

## Step 3 — ConfigMap

```bash
kubectl apply -f tams-config.yaml
```

> Before applying, update `CORS_ALLOWED_ORIGINS` with your actual domain and confirm
> `KAFKA_BROKER` matches the service name in `kafka/service.yaml`.

---

## Step 4 — PostgreSQL

Apply PVCs first so storage is provisioned before the StatefulSets start.

```bash
kubectl apply -f postgres/postgres-auth-pvc.yaml
kubectl apply -f postgres/postgres-rules-pvc.yaml
kubectl apply -f postgres/postgres-analysis-pvc.yaml

kubectl apply -f postgres/postgres-auth-statefulset.yaml
kubectl apply -f postgres/postgres-rules-statefulset.yaml
kubectl apply -f postgres/postgres-analysis-statefulset.yaml

kubectl apply -f postgres/postgres-auth-service.yaml
kubectl apply -f postgres/postgres-rules-service.yaml
kubectl apply -f postgres/postgres-analysis-service.yaml
```

Wait for all pods to be ready:
```bash
kubectl rollout status statefulset/postgres-auth -n tams
kubectl rollout status statefulset/postgres-rules -n tams
kubectl rollout status statefulset/postgres-analysis -n tams
```

---

## Step 5 — Kafka

```bash
# 1. Generate a CLUSTER_ID and update kafka/configmap.yaml before applying:
#    docker run --rm confluentinc/cp-kafka:7.8.0 kafka-storage random-uuid
kubectl apply -f kafka/configmap.yaml
kubectl apply -f kafka/service.yaml
kubectl apply -f kafka/statefulset.yaml
```

Wait for Kafka to be ready, then create topics:
```bash
kubectl rollout status statefulset/kafka -n tams
kubectl apply -f kafka/init-job.yaml
kubectl wait --for=condition=complete job/kafka-init -n tams --timeout=120s
```

---

## Step 6 — Backend Services

auth-service and rule-service have no dependency on Kafka and can be applied together.
analysis-service and parser-service require Kafka to be ready.

```bash
# Independent services
kubectl apply -f auth-service/deployment.yaml
kubectl apply -f auth-service/service.yaml
kubectl apply -f rule-service/deployment.yaml
kubectl apply -f rule-service/service.yaml

# Kafka-dependent services
kubectl apply -f parser-service/deployment.yaml
kubectl apply -f parser-service/service.yaml
kubectl apply -f parser-service/hpa.yaml
kubectl apply -f analysis-service/deployment.yaml
kubectl apply -f analysis-service/service.yaml
kubectl apply -f analysis-service/hpa.yaml
```

Wait for rollout:
```bash
kubectl rollout status deployment/auth-service -n tams
kubectl rollout status deployment/rule-service -n tams
kubectl rollout status deployment/parser-service -n tams
kubectl rollout status deployment/analysis-service -n tams
```

---

## Step 7 — api-gateway and frontend

```bash
kubectl apply -f api-gateway/deployment.yaml
kubectl apply -f api-gateway/service.yaml
kubectl apply -f api-gateway/hpa.yaml
kubectl apply -f frontend/deployment.yaml
kubectl apply -f frontend/service.yaml
```

---

## Step 8 — Ingress

Apply the Ingress last, after all backend services are healthy.
Update `tams.example.com` in `api-gateway/ingress.yaml` to your actual domain first.

```bash
kubectl apply -f api-gateway/ingress.yaml
```

---

## Verifying the Stack

```bash
# All pods running
kubectl get pods -n tams

# All services present
kubectl get services -n tams

# HPA status
kubectl get hpa -n tams

# Ingress address
kubectl get ingress -n tams
```

---

## Tearing Down

```bash
# Remove all TAMS resources (preserves PVCs to protect data)
kubectl delete namespace tams

# To also delete persistent data (irreversible)
kubectl delete pvc --all -n tams
```

---

For implementation notes, caveats, and design decisions see [`NOTES.md`](./NOTES.md).
