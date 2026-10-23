# Decision: Health checks cover only the process and what it owns

Status: implemented

## Problem

The container runtime restarts an instance whose liveness fails and stops routing to one whose readiness fails. Pibble depends on PostgreSQL and the media volume, which it owns, and on Meta, the LLM provider and Resend, which it does not. A check that is too broad takes the whole API down for an outage it cannot fix.

## Decision

`quarkus-smallrye-health` serves `/q/health/live` and `/q/health/ready` without a token; `TenantFilter` and `OperatorFilter` only see `/api/v1` and `/internal`. Liveness has no checks of its own: it answers while the process serves HTTP. Readiness checks the database connection (the datasource check of the extension) and `MediaVolumeHealthCheck`, which creates and deletes a file in `takodu.media.dir`. **No health check calls Meta, the LLM or Resend.** Their failures are already handled where they happen: outbound messages, media downloads and assistant turns retry from their rows and end as `failed` or handed off (takodu-jobs), and a sign-in email failure is an error the user sees.

## Alternatives considered

**Readiness includes Meta, the LLM and Resend.** It would show an outage on the health page, but every instance would turn unready at once during a Meta or provider incident, so staff could not even read the inbox or the calendar, which need neither.

**Database check in liveness too.** A database restart would make the runtime restart every app instance, which does not fix the database and adds a cold start when it returns.

## Consequences

An outage of Meta, the LLM or Resend shows in logs and in rows marked `failed`, not on the health endpoints. A missing or read-only media volume makes the instance unready before a download would write into the container's own disk.
