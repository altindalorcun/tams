| Use Case Number: | 004 | | |
| --- | --- | | | --- | --- |
| Use Case Code: | UC-AUTH-004 | | |
| --- | --- | | | --- | --- |
| Use Case Name: | Mandatory Password Change | | |
| --- | --- | | | --- | --- |

| Depicter:         | Agent | Last Modificatory:      | — |
| ----------------- | --- | ----------------------- | --- |
| Description Date: | 2026-06-30 | Last Modification Date: | — |
| ---               | --- | ---                     | --- |

| Actor(s):                | ADMIN, TEACHER, STUDENT (any authenticated user with `mustChangePassword=true`, or any user navigating to change password from AppShell) |
| ------------------------ | ---------------------------------------------------------------------------------------------------------------------------------------------- |
| Short Definition:        | An authenticated user changes their password; on first login the change is mandatory before accessing the main application shell. |
| ---                      | ---                                                                                                                                            |
| Pre-condition:           | The user holds a valid access token. For mandatory flow: `mustChangePassword` is true after UC-AUTH-001 or after Admin-created account (UC-ADMIN-005). Route `/change-password` is protected but rendered outside AppShell so navigation cannot be bypassed (FR-AUTH-003). |
| ---                      | ---                                                                                                                                            |
| Post-condition:          | The password is updated (BCrypt hash). All refresh tokens for the user are invalidated. `mustChangePassword` is cleared. The client clears local auth state and redirects to `/login` for re-authentication. |
| ---                      | ---                                                                                                                                            |
| Priority:                | Must |
| ---                      | ---                                                                                                                                            |
| Frequency of Occurrence: | Once per newly created user on first login; occasionally when a user voluntarily changes password from the AppShell menu. |
| ---                      | ---                                                                                                                                            |
| Main Path:               | 1. After login, if `mustChangePassword` is true, the system redirects to `/change-password` instead of the role home route.<br><br>2. The actor enters current password, new password (min 8 characters), and confirmation on the change-password form (`ChangePasswordPage.tsx`).<br><br>3. The frontend validates fields client-side (react-hook-form + zod).<br><br>4. The frontend sends `POST /api/v1/auth/change-password` with `{ currentPassword, newPassword }` and the Bearer access token.<br><br>5. auth-service verifies the current password, updates the hash, clears `mustChangePassword`, and deletes all refresh tokens for the user.<br><br>6. The frontend clears auth state (`clearAuth`), shows a success toast, and redirects to `/login`.<br><br>7. The actor logs in again with the new password (UC-AUTH-001). |
| ---                      | ---                                                                                                                                            |
| Alternative Path:        | A.01 (alternative to step 5): Current password incorrect — HTTP 401; toast error displayed; user remains on `/change-password`.<br><br>A.02 (alternative to step 3): Client validation failure (password too short, confirmation mismatch) — inline form errors; no API call.<br><br>A.03 (alternative to step 1): Unauthenticated access to `/change-password` — ProtectedRoute redirects to `/login`.<br><br>A.04 (alternative to step 4): Access token expired — HTTP 401; Axios interceptor redirects to `/login`. |
| ---                      | ---                                                                                                                                            |
| Containing Use-Case(s)   | Extended by UC-AUTH-001 when `mustChangePassword` is true. Triggered after UC-ADMIN-005 when Admin creates a user with default password. |
| ---                      | ---                                                                                                                                            |
| Special Requirements:    | FR-AUTH-003 (mandatory password change before AppShell access). Passwords hashed with BCrypt. All sessions invalidated on change. Protected route allows `mustChangePassword` users without AppShell (FR-AUTH-003). |
| ---                      | ---                                                                                                                                            |
| Assumptions:             | New Teacher and Student accounts receive a system default password and `mustChangePassword=true` from AdminUserService. Voluntary password change uses the same page and API when accessed from AppShell. |
| ---                      | ---                                                                                                                                            |
| Note:                    | Route: `/change-password`. Endpoint: `AuthController.changePassword`. Related: FR-AUTH-003, UC-AUTH-001, UC-ADMIN-005. |
| ---                      | ---                                                                                                                                            |

## APPENDIX

### Prompts

1. "Tamam şimdi vision.md, system-requirements.md ve architecture-notebook.md dokümanlarını tamamladık. Geriye use-case dokümanı kaldı. Bu dokümanı oluşturmanı istiyorum."

Conversation link: Current Cursor session.
