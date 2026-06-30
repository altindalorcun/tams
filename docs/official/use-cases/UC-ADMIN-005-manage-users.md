| Use Case Number: | 005 | | |
| --- | --- | | | --- | --- |
| Use Case Code: | UC-ADMIN-005 | | |
| --- | --- | | | --- | --- |
| Use Case Name: | Manage Users | | |
| --- | --- | | | --- | --- |

| Depicter:         | Agent | Last Modificatory:      | — |
| ----------------- | --- | ----------------------- | --- |
| Description Date: | 2026-06-30 | Last Modification Date: | — |
| ---               | --- | ---                     | --- |

| Actor(s):                | ADMIN |
| ------------------------ | ---------------------------------------------------------------------------------------------------------------------------------------------- |
| Short Definition:        | An Admin creates, updates, deletes, and resets passwords for Teacher and Student platform accounts. |
| ---                      | ---                                                                                                                                            |
| Pre-condition:           | The actor is authenticated with role ADMIN. No public self-registration endpoint exists in MVP. |
| ---                      | ---                                                                                                                                            |
| Post-condition:          | User records in postgres-auth reflect Admin changes. New users receive a default password and `mustChangePassword=true`. Deleted users cannot authenticate. |
| ---                      | ---                                                                                                                                            |
| Priority:                | Must |
| ---                      | ---                                                                                                                                            |
| Frequency of Occurrence: | Low at steady state; bursts at semester start when onboarding teachers and students. |
| ---                      | ---                                                                                                                                            |
| Main Path:               | 1. The Admin navigates to `/admin/users` (`UsersPage.tsx`).<br><br>2. The frontend loads users via `GET /api/v1/auth/admin/users`.<br><br>3. The Admin creates a Teacher or Student account with username, e-mail, role, and student number (required for STUDENT) (`POST /api/v1/auth/admin/users`).<br><br>4. auth-service assigns default password, sets `mustChangePassword=true`, and persists the user.<br><br>5. The Admin updates username, e-mail, or active status via `PUT /api/v1/auth/admin/users/{id}`.<br><br>6. The Admin resets a user's password to the system default via `POST /api/v1/auth/admin/users/{id}/reset-password`.<br><br>7. The Admin deletes a user via `DELETE /api/v1/auth/admin/users/{id}`, invalidating all sessions. |
| ---                      | ---                                                                                                                                            |
| Alternative Path:        | A.01 (alternative to step 3): Duplicate username, e-mail, or student number — HTTP 409.<br><br>A.02 (alternative to step 3): STUDENT role without student number — client zod validation blocks submit; server also rejects if bypassed.<br><br>A.03 (alternative to step 7): Delete non-existent user — HTTP 404.<br><br>A.04 (alternative to step 2): Non-ADMIN role — HTTP 403 from `@PreAuthorize("hasRole('ADMIN')")`. |
| ---                      | ---                                                                                                                                            |
| Containing Use-Case(s)   | Created users trigger UC-AUTH-001 on first login, then UC-AUTH-004 (mandatory password change). Student accounts enable UC-STUD-001 when transcript is analyzed. |
| ---                      | ---                                                                                                                                            |
| Special Requirements:    | FR-AUTH-002 (roles TEACHER, STUDENT only via Admin UI; ADMIN seeded at startup). Passwords BCrypt-hashed. No hardcoded secrets — default password from environment (FR-AUTH-001). |
| ---                      | ---                                                                                                                                            |
| Assumptions:             | Public `POST /api/v1/auth/register` is not exposed in AuthController; all end-user provisioning is Admin-driven. Student JWT includes `studentNumber` claim for result matching (FR-ANAL-006). |
| ---                      | ---                                                                                                                                            |
| Note:                    | Controller: `AdminUserController`. Replaces planned UC-AUTH-002 (public registration). Related: FR-AUTH-002, FR-AUTH-003, UC-AUTH-004. |
| ---                      | ---                                                                                                                                            |

## APPENDIX

### Prompts

1. "Tamam şimdi vision.md, system-requirements.md ve architecture-notebook.md dokümanlarını tamamladık. Geriye use-case dokümanı kaldı. Bu dokümanı oluşturmanı istiyorum."

Conversation link: Current Cursor session.
