| Use Case Number: | 001 | | |
| --- | --- | | | --- | --- |
| Use Case Code: | UC-SYS-001 | | |
| --- | --- | | | --- | --- |
| Use Case Name: | Async PDF Parsing via Kafka | | |
| --- | --- | | | --- | --- |

| Depicter:         | Agent | Last Modificatory:      | — |
| ----------------- | --- | ----------------------- | --- |
| Description Date: | 2026-06-30 | Last Modification Date: | — |
| ---               | --- | ---                     | --- |

| Actor(s):                | System (parser-service Kafka consumer) |
| ------------------------ | ---------------------------------------------------------------------------------------------------------------------------------------------- |
| Short Definition:        | The system consumes a raw transcript PDF from Kafka, parses course data entirely in memory, masks PII, and publishes a structured PII-free message for graduation evaluation. |
| ---                      | ---                                                                                                                                            |
| Pre-condition:           | UC-TEACH-001 has published a message to `transcript.raw` with jobId, teacherId, departmentId, and Base64-encoded PDF bytes. Kafka broker and parser-service consumer are running. |
| ---                      | ---                                                                                                                                            |
| Post-condition:          | A PII-free `ParsedTranscriptMessage` is published to `transcript.parsed` with student number (masked form), course list, and metadata including `registration_date`. Raw PDF bytes are discarded from memory. National identity number never appears in the output. |
| ---                      | ---                                                                                                                                            |
| Priority:                | Must |
| ---                      | ---                                                                                                                                            |
| Frequency of Occurrence: | Once per transcript upload; high volume during graduation periods. |
| ---                      | ---                                                                                                                                            |
| Main Path:               | 1. parser-service `TranscriptConsumer` polls `transcript.raw` (KRaft Kafka, group id from config).<br><br>2. The consumer deserializes the JSON payload into `RawTranscriptMessage`.<br><br>3. The consumer Base64-decodes PDF bytes into memory (FR-PII-001).<br><br>4. `parse_transcript()` extracts semesters, courses, grades, student number, and metadata (`registration_date`) using pdfplumber/PyPDF2 tuned to Hacettepe transcript layout.<br><br>5. `pii_masker.py` validates the parsed payload and ensures national identity number is excluded (FR-PII-002).<br><br>6. `TranscriptProducer` publishes the PII-free result to `transcript.parsed` with jobId as message key.<br><br>7. Kafka retention on `transcript.raw` expires the raw message within 5 minutes (FR-PII-003). |
| ---                      | ---                                                                                                                                            |
| Alternative Path:        | A.01 (alternative to step 4): PDF parse failure (unrecognized format, corrupt file) — error logged without message body; no valid publish to `transcript.parsed`; downstream job marked FAILED by UC-SYS-002 or timeout.<br><br>A.02 (alternative to step 2): Invalid JSON or schema validation failure — consumer logs error and skips message without crashing the loop.<br><br>A.03 (alternative to step 5): PII validation failure — publish blocked; error logged.<br><br>A.04 (alternative to step 1): Kafka consumer error — logged; consumer continues polling (at-least-once delivery with retry). |
| ---                      | ---                                                                                                                                            |
| Containing Use-Case(s)   | Included by UC-TEACH-001. Output consumed by UC-SYS-002. |
| ---                      | ---                                                                                                                                            |
| Special Requirements:    | FR-PII-001 (in-memory only, no disk write), FR-PII-002 (TC Kimlik No excluded), FR-PII-003 (`transcript.raw` 5 min retention, `transcript.parsed` 1 hr). AD-002 (Kafka pipeline), AD-003 (Python parser-service). Parser failures must not log raw PDF or PII (FR-SYS-004). |
| ---                      | ---                                                                                                                                            |
| Assumptions:             | Transcript PDF follows standard Hacettepe University layout. Parser is stateless with no database. CPU-bound parsing runs in the consumer thread/process. |
| ---                      | ---                                                                                                                                            |
| Note:                    | Components: `parser-service/src/consumer.py`, `pdf_parser.py`, `pii_masker.py`, `producer.py`. Related: AD-002, AD-003, FR-ANAL-002. |
| ---                      | ---                                                                                                                                            |

## APPENDIX

### Traceability Table

| Section | Source | Author | Date |
| --- | --- | --- | --- |
| Use Case Definition | `docs/official/vision.md` (Other Product Requirements — PII, async pipeline); `docs/official/system-requirements.md` (FR-PII-001–003, FR-ANAL-002); `docs/official/architecture-notebook.md` (AD-002, AD-003; PII Masking Pipeline) | Agent | 2026-07-01 |

### Prompts

1. "Tamam şimdi vision.md, system-requirements.md ve architecture-notebook.md dokümanlarını tamamladık. Geriye use-case dokümanı kaldı. Bu dokümanı oluşturmanı istiyorum."

2. "Tamam şimdi aynı şekilde vision dokümanının altındaki Traceability Table'ı doldur. Dokümaları hazırlama sıram, Vision, SRS, architectural notebook, use-case ve graphical user interface. Bunların tracebility table'larını güncelle"

Conversation link: Current Cursor session.
