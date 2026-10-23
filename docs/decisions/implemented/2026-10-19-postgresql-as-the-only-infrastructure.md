# Decision: PostgreSQL is the only infrastructure

Status: implemented

## Problem

The legacy required Redis, a push server, a media server, Stripe, Sentry and Prometheus before the product worked, and refused to boot without Stripe secrets. About twenty businesses and a few thousand messages a day fit far inside one process and one database.

## Decision

Pibble runs as one Quarkus process with one PostgreSQL database on one VPS. Login codes, sessions, work queues (`SKIP LOCKED`) and outboxes are tables. The live inbox is Server-Sent Events (`GET /api/v1/inbox/stream`) fed by an in-memory Mutiny broadcaster; the browser refetches on reconnect, so a missed event costs nothing. Inbound media is stored on a mounted volume. An external service exists only where a business rule needs it: Meta (WhatsApp), an LLM provider (assistant) and Resend (sign-in codes and invitations).

## Alternatives considered

**Redis for sessions, cache and queues.** One more service to pay for, secure and back up, for load PostgreSQL handles.

**WebSockets for the inbox.** Two-way channels are not needed; SSE is plain HTTP, reconnects by itself and passes proxies.

**Polling for the inbox.** Simplest, but either slow to show messages or wasteful.

**Starting without the Meta, LLM or Resend keys and disabling those features.** A production deploy with a forgotten key would boot and silently stop answering customers or emailing codes. The prod profile instead refuses to start when any of them is missing (`ProductionConfigTest`); dev and test use fakes.

## Consequences

Running Pibble needs PostgreSQL, a media volume and the environment variables in `.env.example`; `pom.xml` has no Redis, broker or push service. Backups are a database dump plus a copy of the media volume. Jobs are safe on several instances, because every claim locks its row with `SKIP LOCKED` (`OutboundMessageSenderTest`). **The application itself is not:** an inbox stream only receives changes made on its own instance, and media files live on one instance's volume. Scaling past one instance needs a shared volume or object storage and PostgreSQL `LISTEN/NOTIFY` behind the broadcaster.
