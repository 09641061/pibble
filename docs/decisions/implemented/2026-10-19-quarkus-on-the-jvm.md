# Decision: Rebuild in Quarkus on the JVM

Status: implemented

## Problem

The legacy Spring Boot backend has the wrong boundaries at the model level (customers identified by document, owner-only tenancy, stubbed booking rules), so fixing it costs more than rebuilding. The new backend runs on one small VPS and needs scheduled jobs, in-process events and LLM tool calling.

## Decision

Pibble is a new Quarkus application on Java 25, run in JVM mode as one container. Quarkus brings CDI events, `quarkus-scheduler`, Dev Services for PostgreSQL and the LangChain4j extension. The legacy backend is read only as a reference for business rules and the Meta integration; no code is copied without adapting it to these layers.

## Alternatives considered

**Fix the Spring Boot legacy.** Every fallacy changes the shape of the domain, so most of the code would be rewritten anyway, with Redis and Stripe still required to boot.

**Spring Boot rewrite.** Familiar, but slower startup and higher memory on a 4 GB VPS, and scheduling, events and LLM integration need more assembly.

**GraalVM native image.** Lower memory, but slower builds, harder debugging and reflection configuration for Hibernate and LangChain4j; the JVM fits in the available memory.

## Consequences

The team learns Quarkus idioms (Panache, `@ServerExceptionMapper`, Dev Services). The build has no native profile, and `src/main/docker/Dockerfile.jvm` is the only container image.
