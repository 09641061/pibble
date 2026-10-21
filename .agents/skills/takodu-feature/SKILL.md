---
name: takodu-feature
description: Step-by-step workflow for adding a capability to the pibble Quarkus backend — a new endpoint, aggregate, command, query, field, or state transition — so the change lands in the right bounded context and layers with tests and docs. Use for any feat or new behavior; use takodu-fix for defects instead.
---

# Adding a feature to pibble

Build from the inside out: domain first, HTTP last. Each step names the files it creates; [takodu-ddd](../takodu-ddd/SKILL.md) owns the package and naming rules and [its templates](../takodu-ddd/references/templates.md) own the class shapes. Do not skip a layer because the feature looks small: a one-field change still flows value object → aggregate → persistence entity → assembler → resource.

## 1. Place the feature

1. Name the business fact in one sentence ("staff cancel a confirmed appointment") and find its rule ids in the product docs (`../docs/03-business-domain.md`, for example B10 and B12). If no rule covers the behavior, stop and ask the user instead of inventing one.
2. Pick the bounded context that owns the aggregate whose state changes, using the table in [docs/architecture.md](../../../docs/architecture.md). If no context owns it, stop and ask the user; a new context needs a decision record ([takodu-docs](../takodu-docs/SKILL.md)).
3. List what you need from other contexts. Each item becomes a facade call or an integration event, and must be allowed by the context map: follow [takodu-context-integration](../takodu-context-integration/SKILL.md) before writing code that touches another context.
4. Decide who may do it (the roles table in the product docs) and whether an inactive plan blocks it (O6), following [takodu-tenancy](../takodu-tenancy/SKILL.md). WhatsApp work also follows [takodu-whatsapp](../takodu-whatsapp/SKILL.md); assistant work, [takodu-assistant](../takodu-assistant/SKILL.md).
5. Read the existing classes of the target aggregate and copy their style before adding new ones. When the context does not exist yet, read its section of `../docs/05-domain-model.md` and its tables in `../docs/schema.sql`, and follow this repository's names where the two differ.

## 2. Domain

1. Add or extend value objects for every new field with a rule.
2. Add the aggregate method or factory that expresses the change and enforces its invariants, with the rule id in its Javadoc. Throw a context exception that extends the right shared base. Pass the current instant in; never read the clock in the domain.
3. Add the command or query record with compact-constructor checks.
4. Add repository port methods only if an existing one cannot answer the question; each takes the `OrganizationId`.
5. Write plain JUnit tests for the new invariants: one test for the accepted case and one per rejected case.

## 3. Application

1. Add `handle(<New>Command)` or `handle(<New>Query)` to the service interface with Javadoc naming every exception it throws.
2. Implement it in the `*ServiceImpl`: check `AccessPolicy` and the plan, load, check, call the domain, save, fire the integration event if other contexts care, return the aggregate. Mark command handlers `@Transactional`.
3. Put any call to an external system (Meta, the LLM, Resend) behind a port in `application.internal.outboundservices.<capability>`. Work that calls one after a user action is a row that a job sends ([takodu-jobs](../takodu-jobs/SKILL.md)), not a call inside the transaction.

## 4. Infrastructure

1. Add columns to the persistence entity, a `*PersistenceConverter` for each new value object, and the mapping in both directions of the persistence assembler. Schema changes follow [takodu-migrations](../takodu-migrations/SKILL.md), including the constraint that guards any invariant concurrency can break.
2. Add Panache queries and implement new port methods in the `*RepositoryImpl`; translate the SQL state of a guarding constraint into the context's exception.
3. Implement new outbound ports under `infrastructure.<technology>`.

## 5. Interfaces

1. Add request and response resources; request fields get Bean Validation annotations. Organization, role and author come from `TenantContext`, never from the body.
2. Add `*CommandFromResourceAssembler` / `*ResourceFromEntityAssembler` methods.
3. Add the controller method with `@Operation` and one `@APIResponse` per possible status. State transitions are `POST /{id}/<noun-plural>`.
4. Add a `@QuarkusTest` that checks the success status and at least one error status and its `code`, including 403 for a role that may not act when the rule restricts roles.

## 6. Finish

1. Run `./mvnw test`. `ArchitectureTest` must pass; fix the placement instead of weakening a rule.
2. Update the docs the change affects in the same commit ([takodu-docs](../takodu-docs/SKILL.md)). Write a decision record only when you chose between real alternatives.
3. Commit with `feat(<context>): <imperative summary>`, where the context is `identity`, `organization`, `customers`, `booking`, `conversations` or `assistant`; split unrelated changes into separate commits.

## Done means

- [ ] The rule lives in the domain, cites its rule id, and a plain unit test proves the rule.
- [ ] Every query is scoped by `OrganizationId`, and the endpoint checks who may act.
- [ ] Every new failure has a typed exception and a documented HTTP status.
- [ ] No class outside `interfaces` knows about HTTP, no class outside `infrastructure` knows about JPA.
- [ ] `./mvnw test` passes locally and you report the command you ran.
