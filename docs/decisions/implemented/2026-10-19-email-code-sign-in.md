# Decision: Email-code sign-in with rotating sessions

Status: implemented

## Problem

Staff sign in from a phone and a computer, often on weak wifi. Passwords need reset flows and leak; the legacy kept codes and sessions in Redis and allowed one session per user, signing staff out of one device when they used the other.

## Decision

Sign-in is passwordless. `POST /api/v1/auth/login-codes` stores a 6-digit code as a SHA-256 hash and emails it after the commit: through Resend in prod (`ResendEmailSender`, with the code's id as Resend's idempotency key), to the console in dev mode (`LoggingEmailSender`, absent from the prod build). A code is valid 10 minutes, single use and refused after 5 wrong attempts, and an email gets at most 5 codes per hour (429 `LOGIN_CODE_LIMIT`). `POST /api/v1/auth/sessions` accepts **any usable code of the email**, so a code that arrived late still works, and consumes all of them; a wrong code counts against every usable code and is committed although the request answers 401 `LOGIN_CODE_INVALID`. The first sign-in creates the `User`; a disabled user gets 403.

A `Session` is one device: a refresh token (32 random bytes, 30 days, stored only as a hash) plus 15-minute RS256 access tokens signed with SmallRye JWT, whose subject is the `UserId` and whose `sid` claim is the session. `POST /api/v1/auth/session-refreshes` rotates the token. **The previous token is accepted again within 2 minutes of its rotation**, so a refresh whose response was lost can be retried; presenting it later means two parties hold the session, so the session is revoked (401 `REFRESH_TOKEN_INVALID`). `GET /api/v1/auth/sessions` lists the caller's devices and `DELETE /api/v1/auth/sessions/{id}` signs one out. `CleanupJob` deletes codes and sessions one day after they end.

## Alternatives considered

**Passwords.** They need hashing, reset by email anyway and breach handling, for no gain over a code sent to the same inbox.

**Google sign-in or magic links.** Useful later; both can be added as other ways to create a `Session` without changing the model.

**Redis for codes and sessions.** Expiry for free, but one more service to run, secure and back up at a scale PostgreSQL handles.

**Only the newest code is valid.** Simpler, but on slow email the person types the first code after asking for a second one and is refused.

**Strict rotation without a retry window.** Any lost refresh response would sign the device out; with 15-minute access tokens on weak wifi that happens daily.

## Consequences

Prod needs `RESEND_API_KEY`, `EMAIL_FROM` and the two JWT key files (`.env.example`); dev and test generate a key pair at startup. A revoked session's access tokens keep working for up to 15 minutes, since they are not checked against the database. Codes are hashed without a pepper: a leaked database exposes codes for at most 10 minutes. The tests (`SessionsControllerTest`, `SessionRefreshesControllerTest`, `SessionTest`) pin every rule above.
