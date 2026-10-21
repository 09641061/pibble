# Decision: PostgreSQL is the only infrastructure

Status: proposed

## Problem

The legacy required Redis, a push server, a media server, Stripe, Sentry and Prometheus before the product worked, and refused to boot without Stripe secrets. About twenty businesses and a few thousand messages a day fit far inside one process and one database.

## Proposal

Pibble runs as one Quarkus process with one PostgreSQL database on one VPS. Login codes, sessions, work queues (`SKIP LOCKED`) and outboxes are tables. The live inbox is Server-Sent Events (`GET /api/v1/inbox/stream`) fed by an in-memory Mutiny broadcaster; the browser refetches on reconnect, so a missed event costs nothing. Inbound media is stored on a mounted volume. An external service is added only when a business rule needs it: Meta (WhatsApp), an LLM provider (assistant) and Resend (sign-in codes and invitations).

## Alternatives considered

**Redis for sessions, cache and queues.** One more service to pay for, secure and back up, for load PostgreSQL handles.

**WebSockets for the inbox.** Two-way channels are not needed; SSE is plain HTTP, reconnects by itself and passes proxies.

**Polling for the inbox.** Simplest, but either slow to show messages or wasteful.

## Acceptance criteria

- The application starts with only PostgreSQL and its environment variables; Meta, LLM and Resend keys missing disables those features instead of blocking startup.
- No dependency on Redis, a broker or a push service appears in `pom.xml`.
