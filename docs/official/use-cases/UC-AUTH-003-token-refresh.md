| Use Case Number: | 003 | | |
| --- | --- | | | --- | --- |
| Use Case Code: | UC-AUTH-003 | | |
| --- | --- | | | --- | --- |
| Use Case Name: | Token Refresh | | |
| --- | --- | | | --- | --- |

| Depicter:         | Agent | Last Modificatory:      | — |
| ----------------- | --- | ----------------------- | --- |
| Description Date: | 2026-06-30 | Last Modification Date: | — |
| ---               | --- | ---                     | --- |

| Actor(s):                | ADMIN, TEACHER, STUDENT (any authenticated user holding a valid refresh token) |
| ------------------------ | ---------------------------------------------------------------------------------------------------------------------------------------------- |
| Short Definition:        | A client exchanges a valid refresh token for a new access token and a rotated refresh token without requiring the user to re-enter credentials. |
| ---                      | ---                                                                                                                                            |
| Pre-condition:           | The user previously logged in successfully (UC-AUTH-001) and holds a non-expired refresh token stored by the client. auth-service is reachable. |
| ---                      | ---                                                                                                                                            |
| Post-condition:          | The presented refresh token is invalidated (rotation). A new access token and refresh token pair is issued. The client can continue authenticated API calls. |
| ---                      | ---                                                                                                                                            |
| Priority:                | Must |
| ---                      | ---                                                                                                                                            |
| Frequency of Occurrence: | As needed when the access token expires during an active session; potentially several times per long session. |
| ---                      | ---                                                                                                                                            |
| Main Path:               | 1. The client detects that the access token is expired or about to expire.<br><br>2. The client sends `POST /api/v1/auth/refresh` with `{ refreshToken }`.<br><br>3. auth-service looks up the refresh token in the database.<br><br>4. auth-service verifies the token is not expired.<br><br>5. auth-service deletes the old refresh token (rotation per FR-AUTH-001).<br><br>6. auth-service issues a new access token and refresh token pair and returns them in the response.<br><br>7. The client replaces stored tokens and resumes API calls with the new access token. |
| ---                      | ---                                                                                                                                            |
| Alternative Path:        | A.01 (alternative to step 3): Refresh token not found — HTTP 401; the client clears session and redirects to `/login`.<br><br>A.02 (alternative to step 4): Refresh token expired — auth-service deletes the token and returns HTTP 401; the user must log in again (UC-AUTH-001).<br><br>A.03 (alternative to step 2): Malformed request body — HTTP 400 validation error.<br><br>A.04 (alternative to step 2): In MVP frontend, if the access token expires and no refresh is attempted, the Axios response interceptor on HTTP 401 clears sessionStorage and redirects to `/login` without calling refresh. |
| ---                      | ---                                                                                                                                            |
| Containing Use-Case(s)   | Included by UC-AUTH-001 (login issues the initial refresh token). May be invoked before any protected use case when the access token expires. |
| ---                      | ---                                                                                                                                            |
| Special Requirements:    | FR-AUTH-001 (refresh token rotation — old token deleted on each successful refresh). Tokens must not be logged. Stateless JWT access tokens validated at api-gateway (FR-AUTH-004). |
| ---                      | ---                                                                                                                                            |
| Assumptions:             | The refresh token API is fully implemented in auth-service. MVP frontend persists the access token in sessionStorage; automatic silent refresh via the refresh token is a client capability (`authApi.refreshToken`) not yet wired into the Axios interceptor. |
| ---                      | ---                                                                                                                                            |
| Note:                    | Backend endpoint: `AuthController.refresh`, `AuthServiceImpl.refresh`. Related: FR-AUTH-001, AD-005. |
| ---                      | ---                                                                                                                                            |

## APPENDIX

### Prompts

1. "Tamam şimdi vision.md, system-requirements.md ve architecture-notebook.md dokümanlarını tamamladık. Geriye use-case dokümanı kaldı. Bu dokümanı oluşturmanı istiyorum."

Conversation link: Current Cursor session.
