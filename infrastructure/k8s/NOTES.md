# Kubernetes Manifests — Implementation Notes

Bu dosya, manifest dosyaları oluşturulurken karşılaşılan önemli noktaları ve dikkat edilmesi gereken hususları içerir.

---

## 1. Image Registry Placeholder

Tüm `deployment.yaml` dosyalarındaki image adresleri **placeholder** olarak tanımlanmıştır:

```
ghcr.io/tams/<service-name>:latest
```

Gerçek bir registry kullanılmadan önce bu değerlerin güncellenmesi gerekir. Üretim ortamında `latest` tag yerine sabit bir commit SHA veya semantik versiyon kullanılmalıdır (örn: `ghcr.io/tams/api-gateway:1.2.3`).

---

## 2. Secret Referansları — Gerçek Değerler Yok

Deployment'lardaki `secretKeyRef` blokları yalnızca referans tanımlar; Secret objelerinin kendisi ayrıca oluşturulmalıdır. Phase 8'in 8. maddesinde bunlar için `.yaml.example` şablonları oluşturulacaktır.

Referans verilen Secret'lar ve beklenen key'leri:

| Secret Adı          | Key'ler                          | Tüketen Servis(ler)                        |
|---------------------|----------------------------------|--------------------------------------------|
| `tams-jwt-secret`   | `jwt-secret`                     | api-gateway, auth-service, rule-service, analysis-service |
| `tams-db-auth`      | `host`, `user`, `password`       | auth-service                               |
| `tams-db-rules`     | `host`, `user`, `password`       | rule-service                               |
| `tams-db-analysis`  | `host`, `user`, `password`       | analysis-service                           |
| `tams-pii-salt`     | `PII_HASH_SALT`                  | parser-service                             |
| `tams-admin-creds`  | `email`, `username`, `password`  | auth-service                               |

> **Not:** `tams-admin-creds`, `architecture.md`'deki resmi Secret listesinde yer almaz ancak `auth-service` başlangıçta admin kullanıcısını seed etmek için `ADMIN_SEED_EMAIL`, `ADMIN_SEED_USERNAME`, `ADMIN_SEED_PASSWORD` env var'larına ihtiyaç duyar. Bu Secret eksik olduğunda auth-service pod'u `CrashLoopBackOff` ile başarısız olur.

---

## 3. ConfigMap — `tams-config`

Tüm servisler `tams-config` ConfigMap'ine referans verir. Bu ConfigMap Phase 8'in 7. maddesinde (`tams-config.yaml`) oluşturulacaktır. O dosya oluşturulmadan servislerin pod'ları başlamaz.

Beklenen key'ler:

| Key                   | Tüketen Servis(ler)                       | Örnek Değer                              |
|-----------------------|-------------------------------------------|------------------------------------------|
| `KAFKA_BROKER`        | analysis-service, parser-service          | `kafka.tams.svc.cluster.local:9092`      |
| `AUTH_SERVICE_URL`    | api-gateway                               | `http://auth-service.tams.svc.cluster.local:8081` |
| `RULE_SERVICE_URL`    | api-gateway, analysis-service             | `http://rule-service.tams.svc.cluster.local:8082` |
| `ANALYSIS_SERVICE_URL`| api-gateway                               | `http://analysis-service.tams.svc.cluster.local:8083` |
| `CORS_ALLOWED_ORIGINS`| api-gateway                               | `https://tams.example.com`               |
| `LOG_LEVEL`           | parser-service                            | `INFO`                                   |

---

## 4. PostgreSQL Host İsimlendirmesi

`application.yml` dosyaları DB bağlantısını ayrı parçalar halinde okur (`POSTGRES_AUTH_HOST`, `POSTGRES_AUTH_PORT`, `POSTGRES_AUTH_DB`, `POSTGRES_AUTH_USER`, `POSTGRES_AUTH_PASSWORD`). `architecture.md` bu değerleri `POSTGRES_URL` olarak tek bir connection string şeklinde tanımlamıştır — ancak gerçek uygulama koduna göre parçalı yöntem kullanıldığından Deployment'lar bu şekilde yapılandırılmıştır.

PostgreSQL StatefulSet'leri oluşturulduğunda (`infrastructure/k8s/postgres/`) Secret'lardaki `host` değerleri şu formatta olmalıdır:

```
postgres-auth.tams.svc.cluster.local
postgres-rules.tams.svc.cluster.local
postgres-analysis.tams.svc.cluster.local
```

---

## 5. Frontend — Build-Time Env Var

`frontend` Deployment'ında runtime env var yoktur çünkü `VITE_API_URL` bir **build-time** değişkenidir; Vite tarafından derleme esnasında statik bundle'a gömülür. Image build edilirken bu değerin Docker build arg olarak geçirilmesi gerekir:

```bash
docker build --build-arg VITE_API_URL=https://tams.example.com/api .
```

Farklı ortamlar için (staging, prod) farklı image tag'leri oluşturulması önerilir.

---

## 6. Liveness vs Readiness Probe Farkı

Spring Boot servislerinde probe gecikmeler `initialDelaySeconds` ile ayrıştırılmıştır:

| Probe      | initialDelaySeconds | Amacı                                                    |
|------------|---------------------|----------------------------------------------------------|
| readiness  | 30s                 | Servis JVM + Flyway migration tamamlandıktan sonra traffic alır |
| liveness   | 40s                 | Servis tamamen ayağa kalktıktan sonra ölü döngü kontrolü başlar |

parser-service (Python/uvicorn) çok daha hızlı başladığından bu değerler daha düşük tutulmuştur (readiness: 10s, liveness: 15s).

---

## 7. Uygulama Sırası

Manifest'ler belirli bir sırayla apply edilmelidir (bağımlılık sırası):

1. `namespace.yaml`
2. `postgres/` (StatefulSet'ler + PVC'ler)
3. `kafka/` (StatefulSet)
4. ConfigMap ve Secret'lar (`tams-config.yaml`, Secret `.yaml.example` dosyalarından türetilen gerçek Secret'lar)
5. `auth-service/`, `rule-service/` (birbirinden bağımsız)
6. `parser-service/`, `analysis-service/` (Kafka hazır olmalı)
7. `api-gateway/`, `frontend/`
8. `api-gateway/ingress.yaml` (en son — tüm backend'ler hazır olmalı)

---

## 8. Smoke Test — Yapılacaklar Listesi

`smoke-test.sh` scripti tüm adımları otomatikleştirir. Çalıştırmadan önce aşağıdakilerin hazır olduğunu kontrol et.

### Ön Koşullar

- [ ] Docker Desktop çalışıyor
- [ ] `minikube` veya `kind` kurulu (`brew install minikube` / `brew install kind`)
- [ ] `kubectl` kurulu ve çalışıyor
- [ ] Yeterli kaynak: en az **4 CPU**, **6 GB RAM** serbest (Minikube için)

### Çalıştırma (repo root'undan)

```bash
# Minikube ile (önerilen)
./infrastructure/k8s/smoke-test.sh

# kind ile
./infrastructure/k8s/smoke-test.sh --driver=kind

# Image'lar zaten build edilmişse tekrar build etme
./infrastructure/k8s/smoke-test.sh --skip-build
```

### Script Ne Yapar?

1. `tams-smoke` adında yerel bir cluster başlatır (zaten varsa atlar)
2. 6 servisin Docker image'larını `smoke` tag'iyle build eder
3. Image'ları cluster içine yükler (registry gerekmez)
4. Tüm manifest'leri doğru sırayla apply eder
5. Dummy secret değerleriyle Kubernetes Secret'larını otomatik oluşturur — gerçek değer girmen gerekmez
6. Her servis için health endpoint'i `port-forward` üzerinden curl eder ve `PASS` / `FAIL` yazar

### Beklenen Çıktı

Başarılı bir smoke test şu satırlarla biter:

```
[smoke-test] PASS auth-service     — /actuator/health
[smoke-test] PASS rule-service     — /actuator/health
[smoke-test] PASS analysis-service — /actuator/health
[smoke-test] PASS parser-service   — /health
[smoke-test] PASS api-gateway      — /actuator/health
[smoke-test] PASS frontend         — /

[smoke-test] === Smoke test complete ===
```

### Cluster'ı Sil (İşin Bitince)

```bash
./infrastructure/k8s/smoke-test.sh --teardown
# veya
./infrastructure/k8s/smoke-test.sh --teardown --driver=kind
```

### Sorun Giderme

| Belirti | Olası Neden | Çözüm |
|---|---|---|
| Pod `CrashLoopBackOff` | Secret key eksik | `kubectl describe pod <pod> -n tams` çıktısına bak |
| Pod `ImagePullBackOff` | Image yüklenmedi | `smoke-test.sh --skip-build` değil, tam çalıştır |
| Kafka `init-job` timeout | Kafka henüz hazır değil | `kubectl logs job/kafka-init -n tams` ile kontrol et |
| `pg_isready` probe başarısız | PostgreSQL başlamadı | `kubectl logs statefulset/postgres-auth -n tams` |
| Port-forward başarısız | Pod henüz ready değil | `kubectl get pods -n tams` ile tüm pod'ların `Running` olduğunu doğrula |
