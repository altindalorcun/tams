# TAMS — Mezuniyet Kuralları: Mimari Kararlar

Bu dosya `docs/graduation-rules-refactor.md` adımlarının **neden** bu şekilde tasarlandığını açıklar.

---

## 1. Domain Modeli: Neden Değişmiyor?

Mevcut `Department → Category → CategoryCourse ← Course` yapısı kullanıcının tanımladığı tüm senaryoları zaten destekliyor:

```
departments          courses (global katalog)
     │                    │
     ├── department_courses (bölüm ders havuzu)
     │        │
     └── categories
              │
              └── category_courses (is_mandatory flag)
```

**Neden `course_code` global unique?**  
Bir ders (ör. MAT101) tüm üniversitede aynı içeriği temsil eder. Bilgisayar Mühendisliği'nde zorunlu, Elektrik Mühendisliği'nde seçmeli olabilir — bu fark `CategoryCourse.is_mandatory` ile ifade edilir, dersin kendisi değişmez.

**Neden önce bölüm havuzu, sonra kategori?**  
`CategoryService.addCourse()` bir dersin kategoriye eklenebilmesi için önce o bölümün havuzunda olması gerektiğini zorunlu kılar. Bu, "bir bölümdeki kategoriler yalnızca o bölümün verdiği derslerden oluşur" kuralının uygulama katmanında garantisidir.

---

## 2. Kategori Eşik Kuralları

Bir kategori tanımlarken üç bağımsız eşik girilir:

| Alan | Anlamı | Örnek |
|------|--------|-------|
| `min_course_count` | Havuzdan en az kaç ders geçilmeli | `5` (Zorunlu kategorisinde tüm 5 ders) |
| `min_credit` | Havuzdan geçilen derslerin toplam kredisi | `15.0` (Seçmeli kategorisi) |
| `min_ects` | Havuzdan geçilen derslerin toplam ECTS'i | `30.0` |

**Üç eşik de bağımsız değerlendirilir.** Öğrencinin kategoriden mezun sayılması için **üçü de** sağlanmalıdır (0 set edilenler görmezden gelinir).

**`is_mandatory` bayrağı ayrı çalışır:** `CategoryCourse.is_mandatory = true` olan dersler, eşiklerden bağımsız olarak mutlaka geçilmiş olmalıdır. "Zorunlu Dersler" kategorisindeki tüm dersler için bu bayrak `true` set edilirse, minCourseCount eşiği yerine her birinin tek tek geçilmesi garantilenir.

---

## 3. `Deficiency` → `CategoryResult` Dönüşümü

### Mevcut Sorun

`ResultService.completeResult()` şu anda yalnızca **başarısız** kategorileri `Deficiency` satırı olarak kaydediyor. Başarılı kategoriler veritabanına yazılmıyor.

Bu yüzden:
- Frontend'in istediği per-kategori progress barları gösterilemez
- "Seçmeli Derslerden 12/15 kredi tamamlandı" gibi bilgiler kaybolur
- Öğrenciye tam durum raporu verilemez

### Yeni Yaklaşım: `CategoryResult`

Her kategori değerlendirmesi (başarılı veya başarısız) `category_results` tablosuna yazılır:

```
analysis_results (1) ──── (N) category_results
```

`satisfied = false` olan satırlar eski `Deficiency` rolünü üstlenir, ancak artık `earned_credit`, `required_credit` gibi detaylar da saklanır.

### Neden `Deficiency` tablosu silinip `CategoryResult` ayrı tablo?

`CategoryResult` `Deficiency`'nin *strict superset*'idir — tüm deficiency alanlarını içerdiği gibi `satisfied`, `earned_course_count` gibi ekstra alanlar da taşır. İki tabloyu paralel tutmak veri tutarsızlığı riski yaratır.

---

## 4. GPA Hesaplama

**Hacettepe Üniversitesi not skalası (4.00 sistemi):**

| Harf Notu | Katsayı | Harf Notu | Katsayı |
|-----------|---------|-----------|---------|
| A1        | 4.00    | C1        | 2.50    |
| A2        | 3.75    | C2        | 2.25    |
| A3        | 3.50    | C3        | 2.00    |
| B1        | 3.25    | D         | 1.75    |
| B2        | 3.00    | F         | 0.00    |
| B3        | 2.75    |           |         |

**Hesaplama formülü:**

```
GPA = Σ(kredi_i × katsayı_i) / Σ(kredi_i)
```

- Tablodaki not kodlarını tanınan tüm dersler hesaplamaya girer (F dahil, 0.00 ile)
- Tanınmayan not kodları (ör. "G", "EX", "W") hesaplama dışı bırakılır
- `GpaCalculator` `@Component` olarak `GraduationEngine`'e inject edilir

**Neden `GraduationEngine`'de, `ResultService`'de değil?**  
GPA hesaplama, transkript verisi üzerinde saf bir hesaplama işlemidir — veritabanı yoktur, side effect yoktur. `GraduationEngine` stateless bir hesap motoru; GPA da bu katmana uygun.

---

## 5. Öğrenci Self-Lookup: JWT `studentNumber` Claim

### Akış

1. Admin öğrenci oluştururken `studentNumber` girer.
2. auth-service bu değeri `users.student_number` kolonuna yazar.
3. Token üretiminde `JwtUtil.generateAccessToken()`, `STUDENT` rolü için JWT'ye `"studentNumber"` claim'i ekler.
4. parser-service transkript PDF'inden Öğrenci No'yu çıkarır ve `transcript.parsed` Kafka mesajına düz metin olarak yazar (`student_number`). TC Kimlik No yalnızca bellek içinde kalır.
5. analysis-service `ResultService.completeResult()` ile `analysis_results.student_number` kolonuna düz metin yazar.
6. `GET /api/v1/results/me` JWT'deki `studentNumber` ile birebir eşleşme yapar.

```
JWT["studentNumber"] = "21627208"
    ↓ WHERE student_number = '21627208'
en güncel AnalysisResult
```

Öğrenci numarası transkriptten çıkarılamazsa analiz tamamlanır ancak `student_number` null kalır; öğrenci eşleşmesi yapılamaz.

---

## 6. Servisler Arası İletişim

```
rule-service  ←── GET /internal/rules/{departmentId} ───  analysis-service
    (kural deposu)                                          (değerlendirme motoru)
```

- Bu endpoint auth gerektirmez (`SecurityConfig`'de `permitAll()`)
- analysis-service `RuleServiceClient` (WebClient) ile çağırır
- Dönen `RuleSetResponse` içindeki `departmentName` artık `AnalysisResult.departmentName` olarak saklanır — her analiz sonucunda rule-service'e tekrar sorgu atmaya gerek kalmaz

---

## 7. Frontend Alan Adı Uyumsuzlukları

Mevcut `types/rules.ts` dosyası backend'deki DTO alan adlarıyla uyuşmuyor:

| Frontend (yanlış) | Backend (doğru) | Etki |
|-------------------|-----------------|------|
| `Course.name`     | `courseName`    | Ders listeleme ve formlar çalışmıyor |
| `Course.credits`  | `credit`        | Kredi değerleri gösterilemiyor |
| `DepartmentCourse.name` | `courseName` | Bölüm havuzu görünümü bozuk |
| `CategoryCourse.name`   | `courseName` | Kategori ders listesi bozuk |
| `Department.code` | (yok — ekleniyor) | Department form'unda boş alan |
| `CreateDepartmentRequest.code` | (yok — ekleniyor) | Create isteği eksik alan |

Düzeltme stratejisi: Frontend'i backend'e uydurmak (backend naming convention'ları doğru — `courseName`, `credit` Java standard camelCase).

---

## 8. `GET /api/v1/results/by-job/{jobId}` Neden Gerekli?

Öğretmen PDF yüklediğinde `UploadResponse` içinde `jobId` döner. Analiz tamamlanınca öğretmen bu `jobId` ile sonuca ulaşmak ister. Şu an:

- `GET /api/v1/results/{id}` → UUID bekliyor (öğretmenin bilmediği)
- Öğretmen ya sonuç listesinden seçmek zorunda kalıyor

Yeni endpoint `GET /api/v1/results/by-job/{jobId}` bu boşluğu kapatır:
```
uploadTranscript() → {jobId: "abc-123"}
    ↓ poll until COMPLETED
getResultByJobId("abc-123") → AnalysisResultDetailResponse
```
