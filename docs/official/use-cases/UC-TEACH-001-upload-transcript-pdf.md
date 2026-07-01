| Use Case Number: | 001 | | |
| --- | --- | | | --- | --- |
| Use Case Code: | UC-TEACH-001 | | |
| --- | --- | | | --- | --- |
| Use Case Name: | Upload Transcript PDF | | |
| --- | --- | | | --- | --- |

| Depicter:         | Agent | Last Modificatory:      | — |
| ----------------- | --- | ----------------------- | --- |
| Description Date: | 2026-06-30 | Last Modification Date: | — |
| ---               | --- | ---                     | --- |

| Actor(s):                | TEACHER |
| ------------------------ | ---------------------------------------------------------------------------------------------------------------------------------------------- |
| Short Definition:        | A Teacher uploads a student's PDF transcript, selects the target department, and triggers asynchronous parsing and graduation evaluation. |
| ---                      | ---                                                                                                                                            |
| Pre-condition:           | The Teacher is authenticated (UC-AUTH-001) with role TEACHER. Graduation rules exist for the selected department (UC-ADMIN-003). The PDF is a valid Hacettepe transcript format, ≤ 10 MB. |
| ---                      | ---                                                                                                                                            |
| Post-condition:          | An analysis job is created with status PENDING then COMPLETED (or FAILED). Raw PDF is not stored on disk. Masked result is persisted and can be viewed via UC-TEACH-002. |
| ---                      | ---                                                                                                                                            |
| Priority:                | Must |
| ---                      | ---                                                                                                                                            |
| Frequency of Occurrence: | Several times per day per teacher during graduation verification periods; highest during graduation weeks. |
| ---                      | ---                                                                                                                                            |
| Main Path:               | 1. The Teacher navigates to `/teacher` (`TeacherPage.tsx`).<br><br>2. The Teacher selects a department from the dropdown (loaded via `GET /api/v1/departments`).<br><br>3. The Teacher drops or selects a PDF file in `UploadSection` (react-dropzone, max 10 MB client check).<br><br>4. The Teacher clicks upload; the frontend sends `POST /api/v1/transcripts` as multipart form data with `file` and `departmentId` (FR-ANAL-001).<br><br>5. api-gateway enforces 10 MB limit and forwards to analysis-service; `TranscriptService` creates a PENDING `AnalysisResult`, Base64-encodes PDF bytes, and publishes to `transcript.raw` (FR-ANAL-002).<br><br>6. analysis-service returns HTTP 202 with `{ jobId, status: PENDING }`.<br><br>7. The frontend polls `GET /api/v1/transcripts/{jobId}/status` every 3 seconds until status is COMPLETED or FAILED.<br><br>8. The system includes UC-SYS-001 (Kafka PDF parsing) and UC-SYS-002 (graduation evaluation) asynchronously.<br><br>9. On COMPLETED, the frontend invokes UC-TEACH-002 by fetching the result via jobId and opening the result dialog. |
| ---                      | ---                                                                                                                                            |
| Alternative Path:        | A.01 (alternative to step 4): File exceeds 10 MB — client toast error or HTTP 413 from gateway.<br><br>A.02 (alternative to step 4): STUDENT or ADMIN role attempts upload — HTTP 403 (`@PreAuthorize("hasRole('TEACHER')")`).<br><br>A.03 (alternative to step 7): Status becomes FAILED — toast "Analiz başarısız oldu"; no result dialog.<br><br>A.04 (alternative to step 4): Missing departmentId — HTTP 400.<br><br>A.05 (alternative to step 3): Non-PDF file selected — client rejects or server returns parse failure downstream. |
| ---                      | ---                                                                                                                                            |
| Containing Use-Case(s)   | **Includes** UC-SYS-001 (async PDF parsing) and UC-SYS-002 (graduation evaluation). Leads to UC-TEACH-002 (view result). |
| ---                      | ---                                                                                                                                            |
| Special Requirements:    | FR-ANAL-001 (10 MB upload limit), FR-ANAL-002 (async Kafka pipeline), FR-PII-001–003 (in-memory PDF, PII masking, short Kafka retention). AD-002, AD-005. End-to-end completion typically within seconds. |
| ---                      | ---                                                                                                                                            |
| Assumptions:             | Teacher may upload any student transcript; access control on results is by `teacherId` on the analysis record, not by pre-linked teacher-student map. Student number is extracted from PDF during UC-SYS-001. |
| ---                      | ---                                                                                                                                            |
| Note:                    | Endpoints: `TranscriptUploadController`. Poll interval: 3000 ms. Related: FR-ANAL-001, FR-ANAL-002, AD-002. |
| ---                      | ---                                                                                                                                            |

## APPENDIX

### Traceability Table

| Section | Source | Author | Date |
| --- | --- | --- | --- |
| Use Case Definition | `docs/official/vision.md` (Needs and Features — Transcript Analysis, Result Display, History); `docs/official/system-requirements.md` (FR-ANAL-001–005); `docs/official/architecture-notebook.md` (AD-002, AD-003; Kafka pipeline) | Agent | 2026-07-01 |

### Prompts

1. "Tamam şimdi vision.md, system-requirements.md ve architecture-notebook.md dokümanlarını tamamladık. Geriye use-case dokümanı kaldı. Bu dokümanı oluşturmanı istiyorum."

2. "Tamam şimdi aynı şekilde vision dokümanının altındaki Traceability Table'ı doldur. Dokümaları hazırlama sıram, Vision, SRS, architectural notebook, use-case ve graphical user interface. Bunların tracebility table'larını güncelle"

Conversation link: Current Cursor session.
