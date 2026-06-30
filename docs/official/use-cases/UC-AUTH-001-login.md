| Use Case Number: | 001 | | |
| --- | --- | | | --- | --- |
| Use Case Code: | UC-AUTH-001 | | |
| --- | --- | | | --- | --- |
| Use Case Name: | User Login | | |
| --- | --- | | | --- | --- |

| Depicter:         | Agent | Last Modificatory:      | — |
| ----------------- | --- | ----------------------- | --- |
| Description Date: | 2026-06-30 | Last Modification Date: | — |
| ---               | --- | ---                     | --- |

| Actor(s):                | ADMIN, TEACHER, STUDENT |
| ------------------------ | ---------------------------------------------------------------------------------------------------------------------------------------------- |
| Short Definition:        | An authenticated user signs in with e-mail (or username) and password and receives JWT tokens to access role-specific application areas. |
| ---                      | ---                                                                                                                                            |
| Pre-condition:           | The user has an active account created by an Admin. The user is not already authenticated, or has cleared their session. The api-gateway and auth-service are reachable. |
| ---                      | ---                                                                                                                                            |
| Post-condition:          | The client holds a valid access token (and refresh token from the login response). The user is redirected to `/change-password` if `mustChangePassword` is true, otherwise to their role home route (`/admin`, `/teacher`, or `/student/results`). |
| ---                      | ---                                                                                                                                            |
| Priority:                | Must |
| ---                      | ---                                                                                                                                            |
| Frequency of Occurrence: | Several times per day per user; spikes during graduation periods. |
| ---                      | ---                                                                                                                                            |
| Main Path:               | 1. The actor navigates to `/login`.<br><br>2. The actor enters e-mail (or username) and password and submits the form.<br><br>3. The frontend sends `POST /api/v1/auth/login` with `{ email, password }`.<br><br>4. auth-service validates credentials via Spring Security `AuthenticationManager` and issues an access token and refresh token (FR-AUTH-001).<br><br>5. The frontend stores the access token in sessionStorage and updates the auth store with role, username, and `mustChangePassword`.<br><br>6. If `mustChangePassword` is true, the system redirects to `/change-password` (extends UC-AUTH-004).<br><br>7. Otherwise, the system redirects ADMIN to `/admin`, TEACHER to `/teacher`, or STUDENT to `/student/results` (FR-AUTH-002). |
| ---                      | ---                                                                                                                                            |
| Alternative Path:        | A.01 (alternative to step 4): Invalid credentials — auth-service returns HTTP 401; the frontend displays a Turkish error message and remains on `/login`.<br><br>A.02 (alternative to step 4): Inactive or missing user account — auth-service returns HTTP 401 or 404; login fails with a user-friendly error.<br><br>A.03 (alternative to step 3): Rate limit exceeded at api-gateway — HTTP 429 is returned (FR-SYS-002); the user must retry after a short delay.<br><br>A.04 (alternative to step 3): Network or server error — the frontend shows a generic failure message; no session is created. |
| ---                      | ---                                                                                                                                            |
| Containing Use-Case(s)   | Extends UC-AUTH-004 when `mustChangePassword` is true on first login. |
| ---                      | ---                                                                                                                                            |
| Special Requirements:    | FR-AUTH-001 (JWT access and refresh token issuance), FR-AUTH-002 (role embedded in token), FR-AUTH-004 (gateway JWT validation on subsequent requests), FR-SYS-002 (rate limiting). HTTPS/TLS required in production. |
| ---                      | ---                                                                                                                                            |
| Assumptions:             | MVP uses standalone JWT authentication; LDAP integration is deferred to Release 2 (FR-AUTH-006). Login accepts e-mail or username in the same field. |
| ---                      | ---                                                                                                                                            |
| Note:                    | Related requirements: FR-AUTH-001, FR-AUTH-002, FR-AUTH-003. Architectural mechanism: JWT Authentication (AD-005). |
| ---                      | ---                                                                                                                                            |

## APPENDIX

### Prompts

1. "Tamam şimdi vision.md, system-requirements.md ve architecture-notebook.md dokümanlarını tamamladık. Geriye use-case dokümanı kaldı. Bu dokümanı oluşturmanı istiyorum."

Conversation link: Current Cursor session.
