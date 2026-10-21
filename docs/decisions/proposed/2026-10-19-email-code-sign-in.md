# Decision: Email-code sign-in with rotating sessions

Status: proposed

## Problem

Staff sign in from a phone and a computer. Passwords need reset flows and leak; the legacy kept codes and sessions in Redis and allowed one session per user, signing staff out of one device when they used the other.

## Proposal

Sign-in is passwordless: `iam` emails a 6-digit code through Resend, valid 10 minutes, single use, with 5 wrong attempts and at most 5 codes per email per hour. The first successful sign-in creates the `User`. A `Session` is a refresh token (30 days, rotated on every use, stored only as a hash) plus 15-minute JWT access tokens verified with SmallRye JWT. A user may hold several sessions, each revocable. Codes and sessions are PostgreSQL rows; a daily job deletes expired ones.

## Alternatives considered

**Passwords.** They need hashing, reset by email anyway and breach handling, for no gain over a code sent to the same inbox.

**Google sign-in or magic links.** Useful later; both can be added as other ways to create a `Session` without changing the model.

**Redis for codes and sessions.** Expiry for free, but one more service to run, secure and back up at a scale PostgreSQL handles.

## Acceptance criteria

- A code is refused after 10 minutes, after one use and after 5 wrong attempts.
- Refreshing rotates the token, and the old token is refused.
- Signing in on a second device keeps the first session valid.
