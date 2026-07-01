| Use Case Number: | 004 | | |
| --- | --- | | | --- | --- |
| Use Case Code: | UC-AUTH-004 | | |
| --- | --- | | | --- | --- |
| Use Case Name: | User Logout | | |
| --- | --- | | | --- | --- |

| Depicter:         | Agent | Last Modificatory:      | — |
| ----------------- | --- | ----------------------- | --- |
| Description Date: | 2026-06-30 | Last Modification Date: | — |
| ---               | --- | ---                     | --- |

| Actor(s):                | ADMIN, TEACHER, STUDENT |
| ------------------------ | ---------------------------------------------------------------------------------------------------------------------------------------------- |
| Short Definition:        | An authenticated user ends their session so that subsequent requests require a new login. |
| ---                      | ---                                                                                                                                            |
| Pre-condition:           | The user is authenticated and inside the AppShell layout (or holds a valid refresh token for server-side invalidation). |
| ---                      | ---                                                                                                                                            |
| Post-condition:          | Client-side auth state is cleared (access token removed from sessionStorage). Optionally, the refresh token is invalidated on the server. The user is redirected to `/login`. |
| ---                      | ---                                                                                                                                            |
| Priority:                | Must |
| ---                      | ---                                                                                                                                            |
| Frequency of Occurrence: | Once or more per session per user. |
| ---                      | ---                                                                                                                                            |
| Main Path:               | 1. The actor selects "Çıkış Yap" (Logout) from the user menu in AppShell (`AppShell.tsx`).<br><br>2. The frontend clears local auth state via `clearAuth()` (removes accessToken, mustChangePassword, username from sessionStorage).<br><br>3. The frontend navigates to `/login`.<br><br>4. (Optional server step) If the client holds a refresh token, it may call `POST /api/v1/auth/logout` with `{ refreshToken }` to invalidate the token in auth-service (`AuthServiceImpl.logout`). |
| ---                      | ---                                                                                                                                            |
| Alternative Path:        | A.01 (alternative to step 4): Server logout fails (network error) — client session is still cleared locally; user reaches `/login` but refresh token may remain valid until expiry.<br><br>A.02 (alternative to step 4): Invalid or already-revoked refresh token — HTTP 401 or silent no-op; client logout still succeeds locally.<br><br>A.03 (alternative to step 1): Session expires (401 on API call) — Axios interceptor clears sessionStorage and redirects to `/login` without explicit logout action. |
| ---                      | ---                                                                                                                                            |
| Containing Use-Case(s)   | Complements UC-AUTH-001 (login creates the session this use case terminates). |
| ---                      | ---                                                                                                                                            |
| Special Requirements:    | FR-AUTH-001 (refresh token invalidation on logout when server endpoint is called). Client must not retain access token after logout. |
| ---                      | ---                                                                                                                                            |
| Assumptions:             | MVP AppShell logout clears client state only; `authApi.logout` exists but is not invoked from the UI because the refresh token is not persisted in sessionStorage after login. Server-side logout is available for clients that store refresh tokens. |
| ---                      | ---                                                                                                                                            |
| Note:                    | Endpoint: `POST /api/v1/auth/logout`. Related: FR-AUTH-001, UC-AUTH-001. |
| ---                      | ---                                                                                                                                            |

## APPENDIX

### Traceability Table

| Section | Source | Author | Date |
| --- | --- | --- | --- |
| Use Case Definition | `docs/official/vision.md` (Needs and Features — Secure Access); `docs/official/system-requirements.md` (FR-AUTH-001–005; System Interfaces § Login); `docs/official/architecture-notebook.md` (AD-005; JWT Authentication mechanism) | Agent | 2026-07-01 |

### Prompts

1. "Tamam şimdi vision.md, system-requirements.md ve architecture-notebook.md dokümanlarını tamamladık. Geriye use-case dokümanı kaldı. Bu dokümanı oluşturmanı istiyorum."

2. "Tamam şimdi aynı şekilde vision dokümanının altındaki Traceability Table'ı doldur. Dokümaları hazırlama sıram, Vision, SRS, architectural notebook, use-case ve graphical user interface. Bunların tracebility table'larını güncelle"

Conversation link: Current Cursor session.
