# TAMS — Mezuniyet Kuralları V2 Refactor

Mezuniyet motorunu Hacettepe BBM gerçek koşullarına uyarlamak için yapılacak değişikliklerin adım adım listesi.

Mimari kararlar ve tasarım gerekçeleri için → [`docs/graduation-rules-v2-architecture.md`](graduation-rules-v2-architecture.md)

---

## Faz 1 — Küresel Bölüm Kuralları

**Hedef:** 240 ECTS eşiği ve F notu engeli  
**Önkoşul:** Yok

### 1.1 rule-service — Veritabanı Migrasyonu

- [ ] **1.1.1** `services/rule-service/src/main/resources/db/migration/V7__add_global_rules_to_departments.sql` oluştur:

  ```sql
  ALTER TABLE departments
      ADD COLUMN min_total_ects       NUMERIC(6,2),
      ADD COLUMN block_on_any_f_grade BOOLEAN NOT NULL DEFAULT FALSE;
  ```

### 1.2 rule-service — Domain Katmanı

- [ ] **1.2.1** `Department.java` entity'sine alanlar ekle:
  - `@Column(name = "min_total_ects", precision = 6, scale = 2) private BigDecimal minTotalEcts;`
  - `@Column(name = "block_on_any_f_grade", nullable = false) private boolean blockOnAnyFGrade = false;`
  - Constructor'ı güncelle

- [ ] **1.2.2** `DepartmentResponse.java` record'una `BigDecimal minTotalEcts` ve `boolean blockOnAnyFGrade` ekle

- [ ] **1.2.3** `CreateDepartmentRequest.java` ve `UpdateDepartmentRequest.java` record'larına `BigDecimal minTotalEcts` (nullable) ve `Boolean blockOnAnyFGrade` (nullable, default false) ekle

- [ ] **1.2.4** `DepartmentService.java` — `create()` ve `update()` metodlarında yeni alanları entity'ye aktar

### 1.3 rule-service — Internal API

- [ ] **1.3.1** `RuleSetResponse.java` record'una `BigDecimal minTotalEcts` ve `boolean blockOnAnyFGrade` ekle:

  ```java
  public record RuleSetResponse(
      UUID departmentId,
      String departmentName,
      BigDecimal minTotalEcts,
      boolean blockOnAnyFGrade,
      List<RuleCategoryDto> categories
  ) {}
  ```

- [ ] **1.3.2** `CategoryService.getRuleSet()` metodunda yeni alanları `RuleSetResponse` yapıcısına geçir

### 1.4 analysis-service — Engine Güncellemesi

- [ ] **1.4.1** `service/dto/GlobalCheckResult.java` record'u oluştur:

  ```java
  public record GlobalCheckResult(
      GlobalCheckType checkType,
      boolean passed,
      String detail
  ) {
      public enum GlobalCheckType { TOTAL_ECTS, FAIL_GRADE }
  }
  ```

- [ ] **1.4.2** `service/dto/EngineResult.java` record'una `List<GlobalCheckResult> globalChecks` alanı ekle

- [ ] **1.4.3** `GraduationEngine.evaluate()` içine kategori döngüsünden **önce** iki global kontrol ekle:
  1. `ruleSet.minTotalEcts() != null && totalEcts < ruleSet.minTotalEcts()` → `allSatisfied = false`, `GlobalCheckResult(TOTAL_ECTS, false, ...)` listesine ekle
  2. `ruleSet.blockOnAnyFGrade() && allCourses.stream().anyMatch(c -> !c.isPassed())` → `allSatisfied = false`, `GlobalCheckResult(FAIL_GRADE, false, ...)` listesine ekle

- [ ] **1.4.4** `ResultService.java` — `GlobalCheckResult` listesini `CategoryResult` benzeri şekilde persist et ya da `AnalysisResult.errorMessage`'a özet yaz (tasarım tercihine göre)

---

## Faz 2 — Enrollment Year / Cohort Akışı

**Hedef:** Kayıt Tarihi bilgisini Kafka mesajından engine'e ulaştırmak  
**Önkoşul:** Yok (Faz 3'ten önce tamamlanmalı)

### 2.1 analysis-service — Veritabanı Migrasyonu

- [ ] **2.1.1** `services/analysis-service/src/main/resources/db/migration/V7__add_enrollment_fields_to_analysis_results.sql` oluştur:

  ```sql
  ALTER TABLE analysis_results
      ADD COLUMN enrollment_year  INTEGER,
      ADD COLUMN enrollment_term  VARCHAR(10);
  ```

### 2.2 analysis-service — Kafka DTO Güncellemesi

- [ ] **2.2.1** `dto/kafka/TranscriptMetadataDto.java` record'u oluştur:

  ```java
  public record TranscriptMetadataDto(
      @JsonProperty("registration_date") String registrationDate
  ) {}
  ```

- [ ] **2.2.2** `dto/kafka/ParsedTranscriptMessage.java` record'una `TranscriptMetadataDto metadata` alanı ekle:

  ```java
  public record ParsedTranscriptMessage(
      @JsonProperty("student_ref") String studentRef,
      @JsonProperty("job_id") String jobId,
      @JsonProperty("teacher_id") String teacherId,
      @JsonProperty("department_id") String departmentId,
      List<ParsedSemester> semesters,
      TranscriptMetadataDto metadata   // YENİ
  ) { ... }
  ```

### 2.3 analysis-service — EnrollmentYearParser Yardımcısı

- [ ] **2.3.1** `service/EnrollmentYearParser.java` `@Component`'i oluştur:
  - `parse(String registrationDate)` → `Optional<Integer>` döner (yıl)
  - `parseTerm(String registrationDate)` → `"GUZ"` veya `"BAHAR"` döner
  - Format: `"DD.MM.YYYY"` — ay 09–12 → GUZ, ay 01–08 → BAHAR
  - `registrationDate` null veya parse edilemezse `Optional.empty()` döner

### 2.4 analysis-service — AnalysisResult Entity Güncellemesi

- [ ] **2.4.1** `AnalysisResult.java` entity'sine `Integer enrollmentYear` ve `String enrollmentTerm` alanları ekle

- [ ] **2.4.2** `ResultService.java` — `completeResult()` içinde `EnrollmentYearParser` ile enrollment alanlarını doldur:
  - `transcript.metadata()` null kontrolü yaparak `registrationDate` çek
  - `enrollmentYear` ve `enrollmentTerm` hesapla ve entity'ye yaz

### 2.5 analysis-service — EngineResult Güncellemesi

- [ ] **2.5.1** `EngineResult.java` record'una `Integer enrollmentYear` alanı ekle

- [ ] **2.5.2** `GraduationEngine.evaluate()` başında `EnrollmentYearParser` çağrısı ile `enrollmentYear` hesapla; `EngineResult`'a aktar

---

## Faz 3 — Kohort Bazlı Kategori ve Zorunlu Ders Kuralları

**Hedef:** HAS/MUH (2015+), BBM105 (2017+), BBM384 mandatory (2017+)  
**Önkoşul:** Faz 2 tamamlanmış olmalı

### 3.1 rule-service — Veritabanı Migrasyonları

- [ ] **3.1.1** `V8__add_cohort_fields_to_categories.sql` oluştur:

  ```sql
  ALTER TABLE categories
      ADD COLUMN applies_from_year  INTEGER,
      ADD COLUMN applies_to_year    INTEGER;
  ```

- [ ] **3.1.2** `V9__add_cohort_fields_to_category_courses.sql` oluştur:

  ```sql
  ALTER TABLE category_courses
      ADD COLUMN mandatory_from_year  INTEGER,
      ADD COLUMN mandatory_to_year    INTEGER;
  ```

### 3.2 rule-service — Domain Katmanı

- [ ] **3.2.1** `Category.java` entity'sine `appliesFromYear` ve `appliesToYear` (`Integer`, nullable) ekle; constructor'ı güncelle

- [ ] **3.2.2** `CategoryCourse.java` entity'sine `mandatoryFromYear` ve `mandatoryToYear` (`Integer`, nullable) ekle

### 3.3 rule-service — DTO Katmanı

- [ ] **3.3.1** `RuleCategoryDto.java` record'una `Integer appliesFromYear`, `Integer appliesToYear` ekle; `from(Category)` factory metodunu güncelle

- [ ] **3.3.2** `RuleCourseDto.java` record'una `Integer mandatoryFromYear`, `Integer mandatoryToYear` ekle; `from(CategoryCourse)` factory metodunu güncelle

- [ ] **3.3.3** `CreateCategoryRequest.java` ve `UpdateCategoryRequest.java`'ya `Integer appliesFromYear`, `Integer appliesToYear` (nullable) ekle

- [ ] **3.3.4** `CategoryCourseRequest.java`'ya `Integer mandatoryFromYear`, `Integer mandatoryToYear` (nullable) ekle

### 3.4 rule-service — Servis Katmanı

- [ ] **3.4.1** `CategoryService.java` — `create()` ve `update()` metodlarında yeni kohort alanlarını entity'ye aktar

- [ ] **3.4.2** `CategoryController.java` — kategori ders atama endpoint'inde `mandatoryFromYear` / `mandatoryToYear` desteğini doğrula

### 3.5 analysis-service — Engine Güncellemesi

- [ ] **3.5.1** `GraduationEngine.evaluate()` içinde:
  - Her kategori için cohort kontrolü ekle: `appliesFromYear` ≤ `enrollmentYear` ≤ `appliesToYear` (null → sınır yok)
  - Kategori bu kohort için geçerli değilse `CategoryEvaluation` oluşturma; sonuca `COHORT_SKIPPED` etiketiyle ekle

- [ ] **3.5.2** `GraduationEngine.evaluateCategory()` imzasına `int enrollmentYear` parametresi ekle; her zorunlu ders için `mandatoryFromYear` / `mandatoryToYear` kontrolü yap — kohort dışıysa `isMandatory = false` olarak davran

- [ ] **3.5.3** `CategoryEvaluation.java` record'una `boolean cohortSkipped` alanı ekle

### 3.6 Frontend

- [ ] **3.6.1** `types/rules.ts` — `Category` interface'ine `appliesFromYear?: number`, `appliesToYear?: number` ekle

- [ ] **3.6.2** `CategoriesTab.tsx` — form'a opsiyonel "Geçerli Kohort Aralığı" alanları ekle (appliesFromYear / appliesToYear)

---

## Faz 4 — Koşullu Kategori Eşikleri

**Hedef:** BBM384 → 4 vs 5 TS lab; HAS/MUH → 20 vs 22 ECTS  
**Önkoşul:** Faz 3 tamamlanmış olmalı

### 4.1 rule-service — Veritabanı Migrasyonu

- [x] **4.1.1** `V10__add_conditional_thresholds_to_categories.sql` oluştur:

  ```sql
  ALTER TABLE categories
      ADD COLUMN condition_course_codes  TEXT[]       NOT NULL DEFAULT '{}',
      ADD COLUMN min_course_count_if_met INTEGER,
      ADD COLUMN min_ects_if_met         NUMERIC(5,2);
  ```

### 4.2 rule-service — Domain Katmanı

- [x] **4.2.1** `Category.java` entity'sine alanlar ekle:
  - `@Type(StringArrayType.class) @Column(name = "condition_course_codes", columnDefinition = "TEXT[]") private String[] conditionCourseCodes = new String[0];`
  - `@Column(name = "min_course_count_if_met") private Integer minCourseCountIfMet;`
  - `@Column(name = "min_ects_if_met", precision = 5, scale = 2) private BigDecimal minEctsIfMet;`

### 4.3 rule-service — DTO Katmanı

- [x] **4.3.1** `RuleCategoryDto.java` record'una `List<String> conditionCourseCodes`, `Integer minCourseCountIfMet`, `BigDecimal minEctsIfMet` ekle; `from()` factory metodunu güncelle

- [x] **4.3.2** `CreateCategoryRequest.java` ve `UpdateCategoryRequest.java`'ya aynı alanları ekle (nullable)

- [x] **4.3.3** `CategoryService.java` — `create()` ve `update()` metodlarında yeni alanları entity'ye aktar

### 4.4 analysis-service — Engine Güncellemesi

- [x] **4.4.1** `GraduationEngine.evaluateCategory()` başına koşullu eşik çözümü ekle:

  ```java
  boolean conditionMet = category.conditionCourseCodes() != null
      && category.conditionCourseCodes().stream()
             .anyMatch(code -> passedCourseCodes.contains(code.toUpperCase()));

  int effectiveCount = (conditionMet && category.minCourseCountIfMet() != null)
      ? category.minCourseCountIfMet() : category.minCourseCount();

  BigDecimal effectiveEcts = (conditionMet && category.minEctsIfMet() != null)
      ? category.minEctsIfMet() : category.minEcts();
  ```

### 4.5 Frontend

- [x] **4.5.1** `types/rules.ts` — `Category` interface'ine `conditionCourseCodes?: string[]`, `minCourseCountIfMet?: number`, `minEctsIfMet?: number` ekle

- [x] **4.5.2** `CategoriesTab.tsx` — form'a opsiyonel "Koşullu Eşikler" bölümü ekle

---

## Faz 5 — Muafiyet Kuralları (Exemption Rules)

**Hedef:** FIZ103 + FIZ104 → FIZ117 muafiyeti  
**Önkoşul:** Faz 1 tamamlanmış olmalı

### 5.1 rule-service — Veritabanı Migrasyonu

- [x] **5.1.1** `V11__create_exemption_rules_table.sql` oluştur:

  ```sql
  CREATE TABLE exemption_rules (
      id                    UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
      department_id         UUID         NOT NULL REFERENCES departments(id) ON DELETE CASCADE,
      required_course_codes TEXT[]       NOT NULL,
      exempted_course_code  VARCHAR(20)  NOT NULL,
      created_at            TIMESTAMPTZ  NOT NULL DEFAULT NOW()
  );

  CREATE INDEX idx_exemption_rules_department_id ON exemption_rules (department_id);
  ```

### 5.2 rule-service — Domain Katmanı

- [x] **5.2.1** `domain/ExemptionRule.java` entity'si oluştur:
  - `@ManyToOne` → `Department`
  - `@Type(StringArrayType.class) String[] requiredCourseCodes`
  - `String exemptedCourseCode`

- [x] **5.2.2** `Department.java` entity'sine `@OneToMany(mappedBy="department", cascade=ALL, orphanRemoval=true) List<ExemptionRule> exemptionRules` ekle

### 5.3 rule-service — Repository ve Servis

- [x] **5.3.1** `repository/ExemptionRuleRepository.java` oluştur:

  ```java
  public interface ExemptionRuleRepository extends JpaRepository<ExemptionRule, UUID> {
      List<ExemptionRule> findByDepartmentId(UUID departmentId);
  }
  ```

- [x] **5.3.2** `ExemptionRuleService.java` oluştur: `create(departmentId, request)`, `delete(id)`, `findByDepartment(departmentId)`

- [x] **5.3.3** `CategoryService.getRuleSet()` içine `exemptionRules` listesini `RuleSetResponse`'a ekle

### 5.4 rule-service — DTO ve Controller

- [x] **5.4.1** `dto/ExemptionRuleDto.java` record'u oluştur:

  ```java
  public record ExemptionRuleDto(UUID id, List<String> requiredCourseCodes, String exemptedCourseCode) {}
  ```

- [x] **5.4.2** `dto/CreateExemptionRuleRequest.java` record'u oluştur:

  ```java
  public record CreateExemptionRuleRequest(
      @NotEmpty List<String> requiredCourseCodes,
      @NotBlank @Size(max = 20) String exemptedCourseCode
  ) {}
  ```

- [x] **5.4.3** `RuleSetResponse.java` record'una `List<ExemptionRuleDto> exemptionRules` ekle

- [x] **5.4.4** `controller/ExemptionRuleController.java` oluştur:
  - `POST /api/v1/departments/{departmentId}/exemption-rules` (ADMIN)
  - `GET  /api/v1/departments/{departmentId}/exemption-rules` (ADMIN)
  - `DELETE /api/v1/exemption-rules/{id}` (ADMIN)

### 5.5 analysis-service — Engine Güncellemesi

- [x] **5.5.1** `client/dto/ExemptionRuleDto.java` record'u oluştur (analysis-service tarafı, rule-service DTO'sunun mirror'ı)

- [x] **5.5.2** `client/dto/RuleSetResponse.java` record'una `List<ExemptionRuleDto> exemptionRules` ekle

- [x] **5.5.3** `GraduationEngine.evaluate()` içinde kategori döngüsünden önce muafiyet uygulaması ekle:

  ```java
  Set<String> effectivePassed = new HashSet<>(passedCourseCodes);
  for (ExemptionRuleDto rule : ruleSet.exemptionRules()) {
      boolean allPresent = rule.requiredCourseCodes().stream()
          .allMatch(c -> effectivePassed.contains(c.toUpperCase()));
      if (allPresent) {
          effectivePassed.add(rule.exemptedCourseCode().toUpperCase());
      }
  }
  // Kategori döngüsünde effectivePassed kullan
  ```

### 5.6 Frontend

- [x] **5.6.1** `types/rules.ts` — `ExemptionRule` interface'i ekle; `Department` interface'ine `exemptionRules?: ExemptionRule[]` ekle

- [x] **5.6.2** Admin paneline yeni "Muafiyet Kuralları" sekmesi veya bölümü ekle

---

## Faz 6 — Ders Kodu Prefix Sub-limit

**Hedef:** Bölüm dışı seçmelilerde en fazla 3 SEC kodlu ders sayılabilir  
**Önkoşul:** Faz 1 tamamlanmış olmalı

### 6.1 rule-service — Veritabanı Migrasyonu

- [x] **6.1.1** `V12__create_category_prefix_limits_table.sql` oluştur:

  ```sql
  CREATE TABLE category_prefix_limits (
      id                  UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
      category_id         UUID        NOT NULL REFERENCES categories(id) ON DELETE CASCADE,
      course_code_prefix  VARCHAR(10) NOT NULL,
      max_count           INTEGER     NOT NULL,
      created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
  );

  CREATE INDEX idx_category_prefix_limits_category_id ON category_prefix_limits (category_id);
  ```

### 6.2 rule-service — Domain Katmanı

- [x] **6.2.1** `domain/CategoryPrefixLimit.java` entity'si oluştur:
  - `@ManyToOne` → `Category`
  - `String courseCodePrefix`
  - `int maxCount`

- [x] **6.2.2** `Category.java` entity'sine `@OneToMany(mappedBy="category", cascade=ALL, orphanRemoval=true) List<CategoryPrefixLimit> prefixLimits` ekle

### 6.3 rule-service — Repository ve Servis

- [x] **6.3.1** `repository/CategoryPrefixLimitRepository.java` oluştur

- [x] **6.3.2** `CategoryService.java` içine prefix limit CRUD metodları ekle: `addPrefixLimit(categoryId, prefix, maxCount)`, `removePrefixLimit(id)`

- [x] **6.3.3** `CategoryService.getRuleSet()` içindeki category mapping'ine `prefixLimits` listesini ekle

### 6.4 rule-service — DTO ve Controller

- [x] **6.4.1** `dto/PrefixLimitDto.java` record'u oluştur:

  ```java
  public record PrefixLimitDto(UUID id, String courseCodePrefix, int maxCount) {}
  ```

- [x] **6.4.2** `dto/CreatePrefixLimitRequest.java` record'u oluştur:

  ```java
  public record CreatePrefixLimitRequest(
      @NotBlank @Size(max = 10) String courseCodePrefix,
      @Positive int maxCount
  ) {}
  ```

- [x] **6.4.3** `RuleCategoryDto.java` record'una `List<PrefixLimitDto> prefixLimits` ekle

- [x] **6.4.4** `CategoryResponse.java` record'una `List<PrefixLimitDto> prefixLimits` ekle

- [x] **6.4.5** `CategoryController.java`'ya endpoint'ler ekle:
  - `POST /api/v1/categories/{categoryId}/prefix-limits` (ADMIN)
  - `DELETE /api/v1/categories/{categoryId}/prefix-limits/{limitId}` (ADMIN)

### 6.5 analysis-service — Engine Güncellemesi

- [x] **6.5.1** `client/dto/RuleCategoryDto.java` record'una `List<PrefixLimitDto> prefixLimits` ekle

- [x] **6.5.2** `GraduationEngine.evaluateCategory()` içinde prefix sayaçları mantığını ekle:

  ```java
  Map<String, Integer> prefixCounters = new HashMap<>();

  for (RuleCourseDto poolCourse : poolCourses) {
      String code = poolCourse.courseCode().toUpperCase();
      boolean passed = passedCourseCodes.contains(code);

      if (passed) {
          String matchedPrefix = findMatchingPrefix(code, category.prefixLimits());
          if (matchedPrefix != null) {
              int current = prefixCounters.getOrDefault(matchedPrefix, 0);
              PrefixLimitDto limit = getPrefixLimit(matchedPrefix, category.prefixLimits());
              if (current >= limit.maxCount()) {
                  // Bu ders limit dolduğu için sayılmaz
                  continue;
              }
              prefixCounters.put(matchedPrefix, current + 1);
          }
          earnedCredit = earnedCredit.add(...);
          earnedCount++;
      }
      // ... is_mandatory kontrolü devam eder
  }
  ```

### 6.6 Frontend

- [x] **6.6.1** `types/rules.ts` — `PrefixLimit` interface'i ekle; `Category` interface'ine `prefixLimits?: PrefixLimit[]` ekle

- [x] **6.6.2** `CategoriesTab.tsx` — kategori detay görünümüne "Prefix Limitleri" bölümü ekle

---

## Doğrulama Adımları

Her faz tamamlandıktan sonra:

- [ ] **D.1** rule-service Flyway migrasyonlarının hatasız çalıştığını doğrula:
  ```bash
  docker compose up rule-service --build
  ```

- [ ] **D.2** analysis-service Flyway migrasyonlarının hatasız çalıştığını doğrula:
  ```bash
  docker compose up analysis-service --build
  ```

- [ ] **D.3** (Faz 1 sonrası) Bölüme `minTotalEcts = 240`, `blockOnAnyFGrade = true` ayarla; 200 ECTS'li transkript yükle → `isEligible = false` ve global ECTS check sonucu doğrula

- [ ] **D.4** (Faz 1 sonrası) F notlu ders içeren transkript yükle → `isEligible = false` ve global fail_grade check sonucu doğrula

- [ ] **D.5** (Faz 2 sonrası) Kayıt tarihi `"15.09.2017"` olan transkript yükle → `analysis_results.enrollment_year = 2017`, `enrollment_term = 'GUZ'` doğrula

- [ ] **D.6** (Faz 3 sonrası) `applies_from_year = 2015` olan HAS/MUH kategorisini 2013 girişli öğrenci için çalıştır → kategori `COHORT_SKIPPED` olarak işaretlenmeli

- [ ] **D.7** (Faz 3 sonrası) BBM384 dersini `mandatory_from_year = 2017` ile tanımla; 2015 girişli öğrenci için BBM384 geçmeden analiz yap → zorunluluk uygulanmamalı

- [ ] **D.8** (Faz 4 sonrası) Teknik Seçmeli Lab kategorisini `conditionCourseCodes = ["BBM384"]`, `minCourseCountIfMet = 4`, `minCourseCount = 5` ile tanımla; BBM384 geçen öğrenci için 4 TS lab yeterli olmalı

- [ ] **D.9** (Faz 5 sonrası) FIZ103 ve FIZ104 geçen öğrenci için FIZ117 olmaksızın analiz yap → FIZ117 geçilmiş gibi değerlendirilmeli

- [ ] **D.10** (Faz 6 sonrası) Bölüm dışı seçmeli kategorisine `prefix = "SEC"`, `maxCount = 3` ekle; 4 adet SEC dersi geçen öğrencide yalnızca 3'ünün sayıldığını doğrula

---

> Mimari kararlar ve veri modeli detayları için → [`docs/graduation-rules-v2-architecture.md`](graduation-rules-v2-architecture.md)
