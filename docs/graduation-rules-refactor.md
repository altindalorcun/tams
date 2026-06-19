# TAMS — Mezuniyet Kuralları Refactor

Mezuniyet kural yönetimi, analiz sonuç yapısı ve frontend tip uyumsuzluklarını gidermek için yapılacak değişikliklerin adım adım listesi.

Mimari kararlar ve sistem tasarımı için → [`docs/graduation-rules-architecture.md`](graduation-rules-architecture.md)

---

## Part 1 — rule-service: `Department.code` Alanı Ekleme

Ön koşul: Yok

- [x] **1.1** `services/rule-service/src/main/resources/db/migration/V6__add_code_to_departments.sql` dosyasını oluştur:
  ```sql
  ALTER TABLE departments
      ADD COLUMN code VARCHAR(20) NOT NULL DEFAULT '',
      ADD CONSTRAINT uq_departments_code UNIQUE (code);
  ALTER TABLE departments ALTER COLUMN code DROP DEFAULT;
  CREATE INDEX idx_departments_code ON departments (code);
  ```

- [x] **1.2** `Department.java` entity'sine `code` alanı ekle:
  - `@Column(nullable = false, unique = true, length = 20) private String code;`
  - Mevcut `constructor`'ı `code` parametresini alacak şekilde güncelle

- [x] **1.3** `DepartmentResponse.java` record'una `String code` alanı ekle

- [x] **1.4** `CreateDepartmentRequest.java` record'una `@NotBlank @Size(max = 20) String code` ekle

- [x] **1.5** `UpdateDepartmentRequest.java` record'una `@NotBlank @Size(max = 20) String code` ekle

- [x] **1.6** `DepartmentService.java` içinde `create()` ve `update()` metodlarına `code` için unique kontrol ekle (mevcut `name` kontrolüne paralel olarak)

- [x] **1.7** `DepartmentController.java` — `create()` ve `update()` metodlarında service'e `code` iletildiğini doğrula (gerekirse güncelle)

---

## Part 2 — auth-service: Öğrenci Numarası Desteği

Ön koşul: Yok

- [x] **2.1** `auth-service` Flyway migration numarasını kontrol et (`db/migration/` klasöründeki son V numarası neyse bir sonraki). `V?__add_student_number_to_users.sql` oluştur:
  ```sql
  ALTER TABLE users ADD COLUMN student_number VARCHAR(20) UNIQUE;
  ```

- [x] **2.2** `User.java` entity'sine nullable `studentNumber` alanı ekle:
  - `@Column(name = "student_number", unique = true, length = 20) private String studentNumber;`

- [x] **2.3** `RegisterRequest.java` record'una `String studentNumber` (opsiyonel, `@Nullable`) ekle

- [x] **2.4** `AuthServiceImpl.register()` metoduna validasyon ekle:
  - Rol `STUDENT` ise `studentNumber` boş olamaz — `IllegalArgumentException` veya `ValidationException` fırlat
  - `studentNumber` unique kontrolü yap

- [x] **2.5** `JwtUtil.java` → `generateAccessToken()` metoduna:
  - `User` parametresi eklenmiş değilse ekle (ya da mevcut parametre yapısını kullan)
  - Rol `STUDENT` ise JWT claims'e `"studentNumber"` claim'i ekle

- [x] **2.6** `AuthResponse.java` record'una `String studentNumber` ekle (student için JWT response'a da dönmek gerekebilir)

---

## Part 3 — analysis-service: Tam Kategori Sonuçları + GPA

### 3.1 Veritabanı Migrasyonları

Ön koşul: Yok (mevcut V1–V3 mevcut)

- [x] **3.1.1** `V4__add_department_name_gpa_to_analysis_results.sql` oluştur:
  ```sql
  ALTER TABLE analysis_results
      ADD COLUMN department_name VARCHAR(255),
      ADD COLUMN gpa             NUMERIC(4,2);
  ```

- [x] **3.1.2** `V5__create_category_results_table.sql` oluştur:
  ```sql
  CREATE TABLE category_results (
      id                        UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
      result_id                 UUID         NOT NULL REFERENCES analysis_results(id) ON DELETE CASCADE,
      category_id               UUID         NOT NULL,
      category_name             VARCHAR(255) NOT NULL,
      satisfied                 BOOLEAN      NOT NULL,
      required_credit           NUMERIC(5,2) NOT NULL DEFAULT 0,
      earned_credit             NUMERIC(5,2) NOT NULL DEFAULT 0,
      required_ects             NUMERIC(5,2) NOT NULL DEFAULT 0,
      earned_ects               NUMERIC(5,2) NOT NULL DEFAULT 0,
      required_course_count     INTEGER      NOT NULL DEFAULT 0,
      earned_course_count       INTEGER      NOT NULL DEFAULT 0,
      missing_mandatory_courses TEXT[]       NOT NULL DEFAULT '{}',
      created_at                TIMESTAMPTZ  NOT NULL DEFAULT NOW()
  );
  CREATE INDEX idx_category_results_result_id ON category_results (result_id);
  ```

- [x] **3.1.3** `V6__drop_deficiencies_table.sql` oluştur:
  ```sql
  DROP TABLE deficiencies;
  ```

### 3.2 Domain Katmanı

Ön koşul: 3.1 tamamlandı

- [x] **3.2.1** `domain/CategoryResult.java` entity'si oluştur:
  - `result_id` FK ile `AnalysisResult`'a `@ManyToOne` bağlantı
  - Alanlar: `categoryId (UUID)`, `categoryName`, `satisfied (boolean)`, `requiredCredit`, `earnedCredit`, `requiredEcts`, `earnedEcts`, `requiredCourseCount`, `earnedCourseCount`, `missingMandatoryCourses (String[])`
  - `@Type(value = io.hypersistence.utils.hibernate.type.array.StringArrayType.class)` ile PostgreSQL `TEXT[]` desteği veya `@Convert` kullan

- [x] **3.2.2** `AnalysisResult.java` entity'sini güncelle:
  - `String departmentName` alanı ekle
  - `BigDecimal gpa` alanı ekle
  - `List<Deficiency> deficiencies` → `List<CategoryResult> categoryResults` olarak değiştir (`@OneToMany mappedBy="result"`)

- [x] **3.2.3** `domain/Deficiency.java` dosyasını sil

### 3.3 Repository

Ön koşul: 3.2 tamamlandı

- [x] **3.3.1** `repository/CategoryResultRepository.java` interface'i oluştur:
  ```java
  public interface CategoryResultRepository extends JpaRepository<CategoryResult, UUID> {
      List<CategoryResult> findByResultId(UUID resultId);
  }
  ```

### 3.4 GPA Hesaplama

Ön koşul: Yok (bağımsız bileşen)

- [x] **3.4.1** `service/GpaCalculator.java` `@Component`'i oluştur. Hacettepe not skalası:

  | Not | Puan | Not | Puan |
  |-----|------|-----|------|
  | A1  | 4.00 | C1  | 2.50 |
  | A2  | 3.75 | C2  | 2.25 |
  | A3  | 3.50 | C3  | 2.00 |
  | B1  | 3.25 | D   | 1.75 |
  | B2  | 3.00 | F   | 0.00 |
  | B3  | 2.75 |     |      |

  Formül: `GPA = Σ(kredi × not_puanı) / Σ(kredi)` — tanımlı not koduna sahip tüm dersler.

- [x] **3.4.2** `service/dto/EngineResult.java` record'una `BigDecimal gpa` alanı ekle

- [x] **3.4.3** `service/GraduationEngine.java` → `evaluate()` metodunda `GpaCalculator` çağır ve `EngineResult`'a `gpa` ekle

### 3.5 Servis Katmanı

Ön koşul: 3.2, 3.3, 3.4 tamamlandı

- [x] **3.5.1** `service/ResultService.java` → `completeResult()` metodunu güncelle:
  - `departmentName` değerini `ruleSet.departmentName()` kaynağından `AnalysisResult`'a yaz
  - `gpa` değerini `engineResult.gpa()` kaynağından `AnalysisResult`'a yaz
  - `Deficiency` persist eden kodu sil
  - **Tüm** `CategoryEvaluation` sonuçlarını (satisfied ve unsatisfied) `CategoryResult` olarak persist et

### 3.6 DTO Katmanı

Ön koşul: 3.2 tamamlandı

- [x] **3.6.1** `dto/CategoryResultResponse.java` record'u oluştur:
  ```java
  public record CategoryResultResponse(
      UUID categoryId, String categoryName, boolean satisfied,
      BigDecimal requiredCredit, BigDecimal earnedCredit,
      BigDecimal requiredEcts, BigDecimal earnedEcts,
      int requiredCourseCount, int earnedCourseCount,
      List<String> missingMandatoryCourses
  ) { /* static from(CategoryResult) factory */ }
  ```

- [x] **3.6.2** `dto/AnalysisResultDetailResponse.java` güncelle:
  - `List<DeficiencyResponse> deficiencies` → `List<CategoryResultResponse> categoryResults`
  - `String departmentName` ekle
  - `BigDecimal gpa` ekle

- [x] **3.6.3** `dto/AnalysisResultSummaryResponse.java` güncelle:
  - `String departmentName` ekle
  - `BigDecimal gpa` ekle

- [x] **3.6.4** `dto/DeficiencyResponse.java` dosyasını sil

### 3.7 Controller

Ön koşul: 3.6 tamamlandı

- [x] **3.7.1** `controller/ResultController.java` → yeni endpoint ekle:
  ```
  GET /api/v1/results/by-job/{jobId}  (TEACHER rolü)
  ```
  Öğretmen PDF yükledikten sonra `jobId` ile direkt sonuca erişebilir.

- [x] **3.7.2** `controller/ResultController.java` → `getMyResult()` metodunu güncelle:
  - `?studentRef=` query param'ı kaldır
  - JWT'den `studentNumber` claim'ini çıkar
  - Aynı masking mantığı uygulanarak `maskedStudentRef` ile sorgulama yap

### 3.8 ResultQueryService

Ön koşul: 3.6 tamamlandı

- [x] **3.8.1** `service/ResultQueryService.java` güncelle:
  - `deficiencies` mapping'i → `categoryResults` mapping'i ile değiştir
  - `departmentName` ve `gpa` alanlarını response'a ekle

---

## Part 4 — Frontend: Tip ve Bileşen Uyumsuzluklarının Düzeltilmesi

### 4.1 `types/rules.ts`

Ön koşul: Part 1 tamamlandı

- [x] **4.1.1** `Department` interface'ini güncelle:
  - `code: string` ekle
  - `description?: string` ekle
  - `createdAt?: string`, `updatedAt?: string` ekle

- [x] **4.1.2** `Course` interface'ini güncelle:
  - `name` → `courseName`
  - `credits` → `credit`

- [x] **4.1.3** `DepartmentCourse` interface'ini güncelle:
  - `name` → `courseName`
  - `credits` → `credit`

- [x] **4.1.4** `CategoryCourse` interface'ini güncelle:
  - `name` → `courseName`
  - `credits` → `credit`

- [x] **4.1.5** `Category` interface'ini güncelle:
  - `description?: string` ekle

- [x] **4.1.6** Request tiplerini güncelle:
  - `CreateDepartmentRequest`: `code: string` ve `description?: string` ekle
  - `CreateCourseRequest`: `name` → `courseName`, `credits` → `credit`
  - `CreateCategoryRequest`: `description?: string` ekle

### 4.2 `api/ruleApi.ts`

Ön koşul: 4.1 tamamlandı

- [x] **4.2.1** Ders oluşturma/güncelleme request body'lerinde `name` → `courseName`, `credits` → `credit` rename et

- [x] **4.2.2** Bölüm oluşturma/güncelleme request body'lerine `code` alanını ekle

### 4.3 Admin Bileşenleri

Ön koşul: 4.1, 4.2 tamamlandı

- [x] **4.3.1** `DepartmentsTab.tsx` güncelle:
  - Oluşturma/düzenleme formuna `code` (zorunlu) ve `description` (opsiyonel) alanları ekle
  - Tablo kolonuna `code` ekle

- [x] **4.3.2** `CoursesTab.tsx` güncelle:
  - `name` → `courseName`, `credits` → `credit` field referanslarını düzelt

- [x] **4.3.3** `CategoriesTab.tsx` güncelle:
  - `name` → `courseName`, `credits` → `credit` field referanslarını düzelt

### 4.4 `types/analysis.ts`

Ön koşul: Part 3.6 tamamlandı

- [x] **4.4.1** `Deficiency` interface'ini sil

- [x] **4.4.2** `CategoryResult` interface'ini backend `CategoryResultResponse` ile hizala:
  ```typescript
  export interface CategoryResult {
    categoryId: string;
    categoryName: string;
    satisfied: boolean;
    requiredCredit: number;
    earnedCredit: number;
    requiredEcts: number;
    earnedEcts: number;
    requiredCourseCount: number;
    earnedCourseCount: number;
    missingMandatoryCourses: string[];
  }
  ```

- [x] **4.4.3** `AnalysisResult` interface'ini güncelle:
  - `studentRef` → `maskedStudentRef`
  - `departmentName: string` ekle (eskiden yoktu)
  - `gpa: number` (zaten var, backend artık doldurduğuna göre aktif hale gelir)
  - `categoryResults: CategoryResult[]` (yeni şema)

- [x] **4.4.4** `AnalysisResultSummary` interface'ini güncelle:
  - `departmentName: string` ekle

### 4.5 `api/analysisApi.ts`

Ön koşul: Part 2 (auth-service), 4.4 tamamlandı

- [x] **4.5.1** `getMyResult()` fonksiyonundan `?studentRef=` query param'ını kaldır (backend artık JWT'den alıyor)

- [x] **4.5.2** `getResultByJobId(jobId: string)` fonksiyonu ekle:
  ```typescript
  export async function getResultByJobId(jobId: string): Promise<AnalysisResult>
  // GET /api/v1/results/by-job/{jobId}
  ```

### 4.6 Analiz Görünüm Bileşenleri

Ön koşul: 4.4, 4.5 tamamlandı

- [x] **4.6.1** `StudentResultPage.tsx` güncelle:
  - `studentRef` → `maskedStudentRef`
  - `Deficiency` referanslarını kaldır
  - `CategoryResult` yeni şemasına göre per-kategori progress görünümünü güncelle

- [x] **4.6.2** `ResultCard.tsx` güncelle:
  - `Deficiency` referanslarını kaldır
  - `CategoryResult` yeni şemasına göre kategori kartlarını güncelle (`satisfied`, `earnedCredit`, `missingMandatoryCourses` alanlarını kullan)

- [x] **4.6.3** `TeacherPage.tsx` güncelle:
  - Yükleme tamamlandıktan sonra `getResult(id)` yerine `getResultByJobId(jobId)` kullan

---

## Doğrulama Adımları

Her part tamamlandıktan sonra:

- [ ] **D.1** rule-service servisini yeniden başlat, Flyway migrasyonlarının hatasız çalıştığını doğrula (Docker daemon çalışırken yapılacak):
  ```bash
  docker compose up rule-service --build
  ```

- [ ] **D.2** auth-service servisini yeniden başlat, `student_number` kolonu eklendiğini doğrula:
  ```bash
  docker compose up auth-service --build
  ```

- [ ] **D.3** analysis-service servisini yeniden başlat, V4–V6 migrasyonlarının çalıştığını doğrula:
  ```bash
  docker compose up analysis-service --build
  ```

- [ ] **D.4** Yeni öğrenci kaydı (studentNumber ile), JWT decode edilerek `studentNumber` claim'i doğrula

- [ ] **D.5** Admin panelinden bölüm/ders/kategori oluşturma formlarının doğru çalıştığını doğrula (code alanı dahil)

- [ ] **D.6** Transkript yükle → analiz tamamlansın → `GET /api/v1/results/by-job/{jobId}` endpoint'ini test et → `categoryResults` array'inin tüm kategorileri içerdiğini kontrol et

- [ ] **D.7** Öğrenci kendi sonucuna `GET /api/v1/results/me` ile erişebiliyor mu kontrol et (query param göndermeden)

---

> Mimari kararlar, tasarım gerekçeleri ve servisler arası iletişim detayları için → [`docs/graduation-rules-architecture.md`](graduation-rules-architecture.md)
