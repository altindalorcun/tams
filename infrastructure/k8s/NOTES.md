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

---

## Phase 8 ve Phase 9 — Yapılacaklar

Bu bölüm kalan tüm görevleri, hangi görevin hangi ortamda yapılması gerektiğiyle birlikte listeler.

---

### Phase 8 — Kalan Görev

#### P8-1 · Smoke Test (Yerel Kubernetes Cluster)

**Durum:** Henüz çalıştırılmadı.  
**Gereksinim:** Docker Desktop, `minikube` veya `kind`, `kubectl`.

```bash
# Minikube ile (önerilen — en az 4 CPU + 6 GB RAM gerekir)
./infrastructure/k8s/smoke-test.sh

# kind ile
./infrastructure/k8s/smoke-test.sh --driver=kind

# Image'lar önceden build edilmişse tekrar build etme
./infrastructure/k8s/smoke-test.sh --skip-build

# İşin bitince cluster'ı sil
./infrastructure/k8s/smoke-test.sh --teardown
```

Script otomatik olarak şunları yapar:
- Cluster'ı başlatır / oluşturur
- 6 servisin Docker image'larını `smoke` tag'iyle build eder
- Image'ları cluster'a yükler (registry gerekmez)
- Sahte secret değerleriyle Kubernetes Secret'larını oluşturur
- Tüm manifest'leri doğru sırayla apply eder
- Her servisin health endpoint'ini port-forward üzerinden kontrol eder

**Beklenen sonuç:** Tüm satırlar `[smoke-test] PASS` olarak kapanır.

---

### Phase 9 — Tamamlananlar

| # | Madde | Oluşturulan / Değiştirilen Dosyalar |
|---|---|---|
| 9-1 | Health endpoint'leri | `auth-service/pom.xml`, `rule-service/pom.xml`, `analysis-service/pom.xml` — `spring-boot-starter-actuator` eklendi |
| 9-2 | Yapılandırılmış JSON logging | Root `pom.xml` + 4 servis `pom.xml` — `logstash-logback-encoder 8.0`; 4× `logback-spring.xml`; `parser-service/requirements.txt` + `src/main.py` — `python-json-logger` |
| 9-3 | Swagger UI doğrulaması | Statik denetim — tüm servisler doğru; `parser-service/src/main.py` `description`/`version` eklendi |
| 9-4 | Uçtan uca test senaryosu | `docs/e2e-test.sh` — Admin → Kural → Teacher → PDF Yükleme → Poll → Sonuç → Student akışı |
| 9-5/6 | cert-manager kurulum scripti | `infrastructure/k8s/cert-manager/install.sh` |
| 9-7 | Staging ClusterIssuer manifest | `infrastructure/k8s/cert-manager/cluster-issuer-staging.yaml` |
| 9-9 | Production ClusterIssuer manifest | `infrastructure/k8s/cert-manager/cluster-issuer-prod.yaml` |
| 9-10 | Ingress SSL annotation doğrulaması | `api-gateway/ingress.yaml` — her iki annotation zaten mevcut |

---

### Phase 9 — Kalan Görevler

#### P9-8 · Staging ClusterIssuer'ı Dağıt ve Sertifikayı Doğrula

**Durum:** Gerçek cluster ve domain gerektirir.

**Ön koşul:** cert-manager kurulu (`./infrastructure/k8s/cert-manager/install.sh` çalıştırıldı).

```bash
# 1. cluster-issuer-staging.yaml içindeki email alanını kendi adresinle güncelle,
#    ardından uygula:
kubectl apply -f infrastructure/k8s/cert-manager/cluster-issuer-staging.yaml

# 2. ClusterIssuer'ın hazır olduğunu doğrula:
kubectl describe clusterissuer letsencrypt-staging
#    "Ready: True" ve "The ACME account was registered" görmek gerekir.

# 3. Ingress'i geçici olarak staging issuer'a çevir (production'da limit yok):
kubectl annotate ingress tams-ingress -n tams \
  cert-manager.io/cluster-issuer=letsencrypt-staging --overwrite

# 4. cert-manager'ın Certificate objesi oluşturmasını bekle (1-3 dakika):
kubectl get certificate tams-tls -n tams -w

# 5. Sertifikanın durumunu kontrol et:
kubectl describe certificate tams-tls -n tams
```

**Beklenen çıktı:**
```
NAME       READY   SECRET     AGE
tams-tls   True    tams-tls   3m
```

**Sorun giderme:**

| Belirti | Olası Neden | Çözüm |
|---|---|---|
| `CertificateRequest` sürekli `Pending` | HTTP-01 challenge erişilemiyor | `kubectl describe certificaterequest -n tams` ve ingress controller log'larına bak |
| `ACME account not registered` | ClusterIssuer e-postası yanlış | `cluster-issuer-staging.yaml` içindeki `email` alanını güncelle ve tekrar apply et |
| `ingressClassName: nginx` bulunamıyor | Nginx ingress controller kurulu değil | `kubectl get ingressclass` ile class adını doğrula |

---

#### P9-11 · Production ClusterIssuer'a Geç

**Durum:** P9-8 tamamlandıktan sonra yapılacak.

```bash
# 1. Production ClusterIssuer'ı uygula
#    (cluster-issuer-prod.yaml içindeki email alanını da güncelle):
kubectl apply -f infrastructure/k8s/cert-manager/cluster-issuer-prod.yaml

# 2. Ingress'i production issuer'a çevir:
kubectl annotate ingress tams-ingress -n tams \
  cert-manager.io/cluster-issuer=letsencrypt-prod --overwrite

# 3. Staging secret'ı sil — cert-manager yeni production sertifikasını otomatik oluşturur:
kubectl delete secret tams-tls -n tams

# 4. Production sertifikasını doğrula (3-5 dakika sürebilir):
kubectl get certificate tams-tls -n tams -w
```

---

#### P9-12 · HTTP → HTTPS Yönlendirmesini Doğrula

**Durum:** Production sertifikası alındıktan sonra yapılacak.

```bash
curl -I http://tams.example.com
```

**Beklenen çıktı:**
```
HTTP/1.1 301 Moved Permanently
Location: https://tams.example.com
```

---

#### P9-13 · Tarayıcıda TLS Doğrulaması

- `https://tams.example.com` adresini tarayıcıda aç
- Güvenlik uyarısı **çıkmaması** gerekir
- Adres çubuğundaki kilit simgesine tıklayarak sertifika detaylarını doğrula:
  - Verilen: `Let's Encrypt`
  - Alan adı: `tams.example.com`
  - Geçerlilik: 90 gün (cert-manager 30 gün kala otomatik yeniler)

---

#### P9-14 · README'ye Otomatik Yenileme Notu Ekle

`README.md`'e şu paragrafı ekle:

> **Sertifika Yönetimi:** cert-manager, Let's Encrypt TLS sertifikalarını sona ermeden 30 gün önce otomatik olarak yeniler. Manuel yenileme işlemi gerekmez.

---

#### P9-15 · PII Log Denetimi

**Durum:** Kod tabanında çalıştırılacak — cluster gerekmez.

```bash
# TC Kimlik No veya Öğrenci No içerebilecek log ifadelerini ara:
rg -n --type java \
  "log\.(info|debug|warn|error).*tc|log\.(info|debug|warn|error).*kimlik|log\.(info|debug|warn|error).*student" \
  services/ -i

rg -n --type py \
  "logger\.(info|debug|warning|error).*tc|logger\.(info|debug|warning|error).*pii" \
  services/ -i
```

**Kontrol edilecekler:**
- Ham TC Kimlik No veya öğrenci adı asla log'a yazılmamalı
- Öğrenci numarası (`student_number`) log'a yazılabilir
- `parser-service` testleri publish edilen payload'da TC kalıntısı olmadığını doğrular

---

#### P9-16 · CONTRIBUTING.md Oluştur

Repo kök dizininde `CONTRIBUTING.md` oluştur. İçermesi gerekenler:
- Geliştirme ortamı kurulumu (Java 21, Python 3.12, Node 22, Docker)
- `.env` dosyası kurulumu (`.env.example` üzerinden)
- Yerel çalıştırma adımları (`make infra-up`, `make up`)
- Branch convention: `feature/`, `fix/`, `chore/` prefixleri
- Commit convention: `type(scope): description` (örn: `feat(auth): add refresh token rotation`)
- PR açmadan önce testlerin geçmesi gerektiği notu

---

#### P9-17 · Final Güvenlik Kontrolü

```bash
# Git geçmişinde şifre/token içeren commit var mı?
git log --all -p | grep -i "password\s*=\s*['\"][^$]" | head -20

# .env dosyası commit'lenmiş mi?
git log --all --name-only | grep "^\.env$"

# Secret .yaml dosyaları (example dışında) commit'lenmiş mi?
git log --all --name-only | grep "secret\.yaml$" | grep -v "\.example$"
```

Sonuçlar boş olmalı.
