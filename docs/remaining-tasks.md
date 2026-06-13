# TAMS — Kalan Görevler

Bu dosya, bir önceki conversation'da tamamlanamayan ve gerçek bir Kubernetes cluster'ı gerektiren görevleri içerir.
Tüm adımların komutları ve sorun giderme bilgileri `infrastructure/k8s/NOTES.md` dosyasında detaylıca belgelenmiştir.

---

## Phase 8 — Kalan

### P8-1 · Smoke Test (Yerel Kubernetes Cluster)

**Gereksinim:** Docker Desktop + `minikube` veya `kind` + `kubectl`

```bash
# Minikube ile (en az 4 CPU + 6 GB RAM)
./infrastructure/k8s/smoke-test.sh

# kind ile
./infrastructure/k8s/smoke-test.sh --driver=kind
```

Detaylar → `infrastructure/k8s/NOTES.md` — "Phase 8 · P8-1" bölümü

---

## Phase 9 — Kalanlar

### P9-8 · Staging ClusterIssuer'ı Dağıt ve Sertifikayı Doğrula

**Ön koşul:** cert-manager kurulu (`./infrastructure/k8s/cert-manager/install.sh` çalıştırıldı)

```bash
# 1. cluster-issuer-staging.yaml içindeki email alanını güncelle
# 2. ClusterIssuer'ı uygula
kubectl apply -f infrastructure/k8s/cert-manager/cluster-issuer-staging.yaml

# 3. Ingress'i staging issuer'a çevir
kubectl annotate ingress tams-ingress -n tams \
  cert-manager.io/cluster-issuer=letsencrypt-staging --overwrite

# 4. Sertifikayı izle (1-3 dakika sürer)
kubectl get certificate tams-tls -n tams -w
# Ready: True görülünce devam et
```

Detaylar → `infrastructure/k8s/NOTES.md` — "P9-8" bölümü (sorun giderme tablosu dahil)

---

### P9-9 · Production ClusterIssuer'a Geç

**Ön koşul:** P9-8 tamamlandı (staging sertifikası alındı)

```bash
# cluster-issuer-prod.yaml içindeki email alanını güncelle
kubectl apply -f infrastructure/k8s/cert-manager/cluster-issuer-prod.yaml

kubectl annotate ingress tams-ingress -n tams \
  cert-manager.io/cluster-issuer=letsencrypt-prod --overwrite

kubectl delete secret tams-tls -n tams
kubectl get certificate tams-tls -n tams -w   # Ready: True bekle
```

Detaylar → `infrastructure/k8s/NOTES.md` — "P9-11" bölümü

---

### P9-10 · HTTP → HTTPS Yönlendirmesini Doğrula

**Ön koşul:** Production sertifikası alındı

```bash
curl -I http://tams.example.com
# Beklenen: HTTP/1.1 301  +  Location: https://tams.example.com
```

---

### P9-11 · Tarayıcıda TLS Doğrulaması (Manuel)

- `https://tams.example.com` adresini aç
- Güvenlik uyarısı çıkmamalı
- Sertifika: Let's Encrypt, doğru domain, geçerli

---

## Referans Dosyalar

| Dosya | İçerik |
|-------|--------|
| `infrastructure/k8s/NOTES.md` | Tüm adımların detaylı komutları + sorun giderme |
| `infrastructure/k8s/cert-manager/install.sh` | cert-manager kurulum + doğrulama script'i |
| `infrastructure/k8s/cert-manager/cluster-issuer-staging.yaml` | Staging ClusterIssuer manifest'i |
| `infrastructure/k8s/cert-manager/cluster-issuer-prod.yaml` | Production ClusterIssuer manifest'i |
| `infrastructure/k8s/smoke-test.sh` | Yerel K8s smoke test script'i |
| `docs/e2e-test.sh` | Uçtan uca iş akışı test script'i |
