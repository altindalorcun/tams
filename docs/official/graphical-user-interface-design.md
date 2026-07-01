**TAMS — Transcript Analysis and Graduation Control System**

**Graphical User Interface Design**

# Purpose

This document inventories every user-facing screen in TAMS Release 1 (MVP) and maps each screen to its related use cases. It follows the structure of the DEL2 Graphical User Interface Design template: each entry provides a **User Interface/Report Number**, **Related Use-Case** references, and a screenshot placeholder for manual capture.

Screens are grouped by functional area for readability. When transferring content to the Word template, each entry below corresponds to one template block; the H2 grouping headings are optional and may be omitted in Word.

Backend-only flows (UC-AUTH-002 token refresh, UC-SYS-001 async PDF parsing, UC-SYS-002 graduation evaluation) have no dedicated UI and are not listed as separate entries.

# Word Template Transfer Notes

- Copy each entry block into the Word template as one **User Interface/Report Number** section followed by **Related Use-Case** and the screenshot image.
- Use TAMS use case codes (`UC-AUTH-001`, etc.) in the Related Use-Case field; do not reuse the numeric placeholders from the sample template.
- H2 section headings in this document are organizational only and are not required by the Word template.

---

## Authentication and Access Control

### Login Page

**User Interface/Report Number:** Login Page

**Route / Component:** `/login` — `LoginPage.tsx`

**Related Use-Case:** UC-AUTH-001

**Screenshot:** [Insert screenshot here]

**Description:** Public login form with TAMS and Hacettepe branding. Capture the default empty form state before submission. Optional second capture: invalid-credentials error state (A.01).

---

### Mandatory Password Change Page

**User Interface/Report Number:** Mandatory Password Change Page

**Route / Component:** `/change-password` — `ChangePasswordPage.tsx`

**Related Use-Case:** UC-AUTH-003

**Screenshot:** [Insert screenshot here]

**Description:** Standalone page rendered outside AppShell. Used on first login when `mustChangePassword` is true and when a user voluntarily opens "Şifre Değiştir" from the AppShell user menu. Capture the form with current password, new password, and confirmation fields visible.

---

### Unauthorized Access Page

**User Interface/Report Number:** Unauthorized Access Page

**Route / Component:** `/forbidden` — `ForbiddenPage` in `AppRouter.tsx`

**Related Use-Case:** FR-AUTH-002

**Screenshot:** [Insert screenshot here]

**Description:** Displayed when an authenticated user navigates to a route that requires a role they do not hold. Capture after redirect from a ProtectedRoute role check.

---

## Shared Application Shell

UC-AUTH-004 (logout) and voluntary password change (UC-AUTH-003) are triggered from the AppShell user dropdown menu and do not have separate full-page entries.

### Authenticated Application Shell (Admin Navigation)

**User Interface/Report Number:** Authenticated Application Shell (Admin Navigation)

**Route / Component:** `AppShell.tsx` with `/admin/*` content area

**Related Use-Case:** UC-AUTH-001, UC-AUTH-004

**Screenshot:** [Insert screenshot here]

**Description:** Desktop view with expanded sidebar showing Admin navigation items: Genel Bakış, Bölümler, Dersler, Mezuniyet Şartları (expanded group), Kullanıcılar. Header must show username, role label "Yönetici", ThemeToggle, and user dropdown menu.

---

### Authenticated Application Shell (Teacher Navigation)

**User Interface/Report Number:** Authenticated Application Shell (Teacher Navigation)

**Route / Component:** `AppShell.tsx` with `/teacher/*` content area

**Related Use-Case:** UC-AUTH-001, UC-AUTH-004

**Screenshot:** [Insert screenshot here]

**Description:** Desktop view with expanded sidebar showing Teacher navigation: Transkript Yükle, Analiz Geçmişi. Header must show role label "Öğretmen" and user dropdown with "Şifre Değiştir" and "Çıkış Yap" items visible or expanded.

---

### Authenticated Application Shell (Student Navigation)

**User Interface/Report Number:** Authenticated Application Shell (Student Navigation)

**Route / Component:** `AppShell.tsx` with `/student/*` content area

**Related Use-Case:** UC-AUTH-001, UC-AUTH-004

**Screenshot:** [Insert screenshot here]

**Description:** Desktop or mobile view with sidebar (or mobile drawer) showing the single Student item: Mezuniyet Durumu. Header must show role label "Öğrenci".

---

## Admin Area

Sidebar routes (`/admin/departments`, `/admin/courses`, etc.) are the primary navigation targets. The Admin Overview Dashboard at `/admin` is an optional consolidated tabbed view using the same underlying components.

### Admin Overview Dashboard

**User Interface/Report Number:** Admin Overview Dashboard

**Route / Component:** `/admin` — `AdminPage.tsx` (tabbed: Bölümler, Ders Kataloğu, Mezuniyet Kategorileri, Müfredat Değişikliği Kuralları)

**Related Use-Case:** UC-ADMIN-001, UC-ADMIN-002, UC-ADMIN-003, UC-ADMIN-004

**Screenshot:** [Insert screenshot here]

**Description:** Optional overview capture showing the tabbed Admin Paneli with at least one tab content visible. Use this entry if documenting the consolidated dashboard rather than individual sidebar pages.

---

### Department Management

**User Interface/Report Number:** Department Management

**Route / Component:** `/admin/departments` — `DepartmentsPage.tsx`, `DepartmentsTab.tsx`

**Related Use-Case:** UC-ADMIN-001

**Screenshot:** [Insert screenshot here]

**Description:** Department list table with global threshold fields (`min_total_ects`, `block_on_any_f_grade`). Capture the list view with filters and action columns visible.

---

### Department Create / Edit Dialog

**User Interface/Report Number:** Department Create / Edit Dialog

**Route / Component:** Modal dialog inside `DepartmentsTab.tsx` — `DeptDialog` (titles: "Yeni Bölüm" / "Bölümü Düzenle")

**Related Use-Case:** UC-ADMIN-001

**Screenshot:** [Insert screenshot here]

**Navigation:** `/admin/departments` → click **Yeni Bölüm**, or click the pencil icon on a department row.

**Description:** Capture the create dialog with department name, `min_total_ects`, and `block_on_any_f_grade` fields visible. The edit dialog may be used as an alternative capture with an existing department name pre-filled.

---

### Department Course Pool Dialog

**User Interface/Report Number:** Department Course Pool Dialog

**Route / Component:** Modal dialog inside `DepartmentsTab.tsx` — `DeptCoursesPoolDialog` (title: `{departmentName} — Ders Havuzu`)

**Related Use-Case:** UC-ADMIN-001, UC-ADMIN-002

**Screenshot:** [Insert screenshot here]

**Navigation:** `/admin/departments` → click the book icon (**Ders Havuzu**) on a department row.

**Description:** Dialog for linking global catalog courses to a department's course pool. Not the same as the category course pool (no enrollment cohort bounds). Capture with assigned and available course lists visible.

---

### Global Course Catalog Management

**User Interface/Report Number:** Global Course Catalog Management

**Route / Component:** `/admin/courses` — `CoursesPage.tsx`, `CoursesTab.tsx`

**Related Use-Case:** UC-ADMIN-002

**Screenshot:** [Insert screenshot here]

**Description:** Global course catalog table with course code, credit, ECTS, and department associations. Capture the list view with filters and action columns visible.

---

### Course Create / Edit Dialog

**User Interface/Report Number:** Course Create / Edit Dialog

**Route / Component:** Modal dialog inside `CoursesTab.tsx` — `CourseDialog` (titles: "Yeni Ders" / "Dersi Düzenle")

**Related Use-Case:** UC-ADMIN-002

**Screenshot:** [Insert screenshot here]

**Navigation:** `/admin/courses` → click **Yeni Ders**, or click the pencil icon on a course row.

**Description:** Capture the create dialog with course code, name, credit, ECTS, and department multi-select visible. The edit dialog may be used as an alternative capture with existing values pre-filled.

---

### Graduation Categories Management

**User Interface/Report Number:** Graduation Categories Management

**Route / Component:** `/admin/graduation-categories` — `CategoriesPage.tsx`, `CategoriesTab.tsx`

**Related Use-Case:** UC-ADMIN-003

**Screenshot:** [Insert screenshot here]

**Description:** Department-scoped graduation category list with threshold columns (`min_course_count`, credit, ECTS, category-level cohort range). Expand a department card, then capture the category table with action buttons visible.

---

### Graduation Category Create / Edit Dialog

**User Interface/Report Number:** Graduation Category Create / Edit Dialog

**Route / Component:** Modal dialog inside `CategoriesTab.tsx` — `CatDialog` (titles: "Yeni Kategori" / "Kategoriyi Düzenle")

**Related Use-Case:** UC-ADMIN-003

**Screenshot:** [Insert screenshot here]

**Navigation:** `/admin/graduation-categories` → expand a department card → click **Kategori Ekle**, or click the pencil icon on a category row.

**Description:** Capture the category definition form: name, minimum course count, credit/ECTS thresholds, and category-level year-only cohort bounds (`applies_from_year` / `applies_to_year`). This dialog is distinct from the course pool and cohort assignment dialogs below.

---

### Category Course Pool Dialog

**User Interface/Report Number:** Category Course Pool Dialog

**Route / Component:** Modal dialog inside `CategoriesTab.tsx` — `CoursesPoolDialog` (title: `{categoryName} — Ders Havuzu`)

**Related Use-Case:** UC-ADMIN-003

**Screenshot:** [Insert screenshot here]

**Navigation:** `/admin/graduation-categories` → expand a department card → on a category row, click the book icon (**Ders havuzu**).

**Description:** Two-panel dialog: **Kategorideki Dersler** (assigned courses with Zorunlu/Seçmeli badges and cohort range labels) and **Eklenebilir Dersler** (courses available to add). Capture with at least one assigned course visible. Do not confuse with the department course pool on `/admin/departments`.

---

### Category Course Assignment and Enrollment Cohort Bounds Dialog

**User Interface/Report Number:** Category Course Assignment and Enrollment Cohort Bounds Dialog

**Route / Component:** Nested modal inside `CategoriesTab.tsx` — `CategoryCourseAssignmentDialog`, `CohortBoundaryFields.tsx` (titles: "Dersi Kategoriye Ekle" / "Ders Atamasını Düzenle")

**Related Use-Case:** UC-ADMIN-003, FR-RULE-005

**Screenshot:** [Insert screenshot here]

**Navigation:** Open the category course pool dialog first, then either:
- click **Seçmeli** or **Zorunlu** on a course under **Eklenebilir Dersler**, or
- click the pencil icon on an assigned course under **Kategorideki Dersler**.

**Description:** Capture the assignment form with Zorunlu/Seçmeli toggle, **Başlangıç Kohortu**, and **Bitiş Kohortu** fields (year + GUZ/BAHAR term selectors). Architecturally significant per AD-007.

---

### Curriculum Equivalence Rules Management

**User Interface/Report Number:** Curriculum Equivalence Rules Management

**Route / Component:** `/admin/curriculum-equivalence-rules` — `CurriculumEquivalenceRulesPage.tsx`, `CurriculumEquivalenceRulesTab.tsx`

**Related Use-Case:** UC-ADMIN-004

**Screenshot:** [Insert screenshot here]

**Description:** Department-scoped table of legacy-to-current course code mappings. Expand a department card and capture the rule list with action columns visible.

---

### Curriculum Equivalence Rule Create Dialog

**User Interface/Report Number:** Curriculum Equivalence Rule Create Dialog

**Route / Component:** Modal dialog inside `CurriculumEquivalenceRulesTab.tsx` — `AddEquivalenceRuleDialog` (title: "Yeni Müfredat Değişikliği Kuralı")

**Related Use-Case:** UC-ADMIN-004

**Screenshot:** [Insert screenshot here]

**Navigation:** `/admin/curriculum-equivalence-rules` → expand a department card → click **Kural Ekle**.

**Description:** Capture the create form with legacy course picker, target course picker, and save action visible. MVP supports create and delete only; there is no edit dialog.

---

### User Management

**User Interface/Report Number:** User Management

**Route / Component:** `/admin/users` — `UsersPage.tsx`

**Related Use-Case:** UC-ADMIN-005

**Screenshot:** [Insert screenshot here]

**Description:** Paginated user list with role, student number, and active status columns. Filters for username, role, student number, and status should be visible.

---

### User Create / Edit Dialog

**User Interface/Report Number:** User Create / Edit Dialog

**Route / Component:** Modal dialogs inside `UsersPage.tsx` (create and edit forms)

**Related Use-Case:** UC-ADMIN-005

**Screenshot:** [Insert screenshot here]

**Navigation:** `/admin/users` → click **Yeni Kullanıcı**, or click the pencil icon on a user row.

**Description:** Capture the create-user dialog with role selector (TEACHER or STUDENT) and, when STUDENT is selected, the student number field visible. Edit dialog may be used as an alternative capture showing active/inactive status toggle.

---

## Teacher Area

### Transcript Upload Page

**User Interface/Report Number:** Transcript Upload Page

**Route / Component:** `/teacher` — `TeacherPage.tsx`, `UploadSection.tsx`

**Related Use-Case:** UC-TEACH-001

**Screenshot:** [Insert screenshot here]

**Description:** Upload card with department dropdown, PDF drop zone, and selected file name visible. Capture the state before upload with department selected and PDF file chosen, showing the "Analizi Başlat" button enabled. Optional second capture: PENDING status indicator during async processing.

---

### Analysis Result Dialog (Post-Upload)

**User Interface/Report Number:** Analysis Result Dialog (Post-Upload)

**Route / Component:** Modal on `TeacherPage.tsx` — `ResultCard.tsx`

**Related Use-Case:** UC-TEACH-002

**Screenshot:** [Insert screenshot here]

**Description:** Dialog titled "Analiz Tamamlandı" shown automatically after a successful upload. Capture with eligibility badge, GPA, category breakdown, global checks, and deficiency details visible for a COMPLETED analysis.

---

### Analysis History and Detail View

**User Interface/Report Number:** Analysis History and Detail View

**Route / Component:** `/teacher/history` — `StudentHistoryPage.tsx`, `HistoryTable`, inline `ResultCard`

**Related Use-Case:** UC-TEACH-003, UC-TEACH-002

**Screenshot:** [Insert screenshot here]

**Description:** History table with filter controls (student number, department, eligibility status, date) and pagination. Capture with one row selected and the inline detail panel showing full analysis result below or beside the table.

---

## Student Area

### Student Graduation Status Page

**User Interface/Report Number:** Student Graduation Status Page

**Route / Component:** `/student/results` — `StudentResultPage.tsx`

**Related Use-Case:** UC-STUD-001

**Screenshot:** [Insert screenshot here]

**Description:** Read-only graduation status view with overall eligibility, GPA, per-category progress cards, global checks, and deficiency alerts. Capture at mobile viewport width (375 px or similar) to demonstrate mobile-first responsive layout per SRS usability requirements.

---

# Excluded Flows

| Item | Reason |
| --- | --- |
| UC-AUTH-002 (Token refresh) | Handled transparently by Axios interceptor; no user-visible screen |
| UC-SYS-001 (Async PDF parsing) | Backend Kafka consumer pipeline; no user-visible screen |
| UC-SYS-002 (Graduation eligibility evaluation) | Backend engine invoked after parsing; results appear in UC-TEACH-002 and UC-STUD-001 screens |

# Traceability Table

| Section | Source | Author | Date |
| --- | --- | --- | --- |
| Purpose | GUI Design template; `docs/official/vision.md` (User Environment) | Agent | 2026-07-01 |
| Word Template Transfer Notes | DEL2 Graphical User Interface Design template | Agent | 2026-07-01 |
| Authentication and Access Control | `docs/official/system-requirements.md` § User Interfaces; `docs/official/use-cases/UC-AUTH-001-login.md`, `UC-AUTH-003-mandatory-password-change.md`, `UC-AUTH-004-logout.md` | Agent | 2026-07-01 |
| Shared Application Shell | `docs/official/system-requirements.md` § Layout and Navigation; `docs/official/vision.md` (role-specific areas) | Agent | 2026-07-01 |
| Admin Area | `docs/official/system-requirements.md` § User Interfaces; `docs/official/use-cases/UC-ADMIN-001` through `UC-ADMIN-005` | Agent | 2026-07-01 |
| Teacher Area | `docs/official/system-requirements.md` § User Interfaces; `docs/official/use-cases/UC-TEACH-001` through `UC-TEACH-003` | Agent | 2026-07-01 |
| Student Area | `docs/official/system-requirements.md` § User Interfaces; `docs/official/use-cases/UC-STUD-001-view-own-analysis-result.md`; `docs/official/vision.md` (mobile-first student UI) | Agent | 2026-07-01 |
| Excluded Flows | `docs/official/architecture-notebook.md` § Use Case View; `docs/official/use-cases/UC-AUTH-002-token-refresh.md`, `UC-SYS-001-async-pdf-parsing.md`, `UC-SYS-002-graduation-eligibility-evaluation.md` | Agent | 2026-07-01 |

# Prompts

1. "Şimdi sana bir doküman template'i vereceğim. DEL2.Graphical_User_Interface_Design_Template (2).docx bu dokümanın içerisindeki fotoğrafları ben dolduracağım ancak senden isteğim, docs/official/architecture-notebook.md, docs/official/system-requirements.md ve docs/official/use-cases içerisindeki dosyalara göre hangi ekranları hangi başlıkların altına yazacağımı ve başlıkların ne olduğunu belirle. Bunun için /docs/official klasörünün altına yeni bir .md dosyası oluştur ve bu dosyaya gerekli template'e göre içeriğini doldur."

2. "Eksik olanları ekle ve dosyayı buna göre güncelle."

3. "Tamam şimdi aynı şekilde vision dokümanının altındaki Traceability Table'ı doldur. Dokümaları hazırlama sıram, Vision, SRS, architectural notebook, use-case ve graphical user interface. Bunların tracebility table'larını güncelle"

Conversation link: Current Cursor session.
