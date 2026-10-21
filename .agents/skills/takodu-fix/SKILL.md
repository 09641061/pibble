---
name: takodu-fix
description: Workflow for fixing a defect in the pibble Quarkus backend without breaking DDD layering — reproduce with a failing test, locate the layer that owns the broken rule, fix it there, and prove it. Use for any bug, wrong HTTP status, data mapping error, or regression; use takodu-feature when the change adds behavior.
---

# Fixing a defect in pibble

A fix changes the class that owns the broken rule, not the class where the symptom appeared. Read [takodu-ddd](../takodu-ddd/SKILL.md) first if you have not touched this codebase in this session.

## 1. Reproduce

1. Write the smallest test that fails for the reported behavior, at the lowest layer that can show it: a plain JUnit test for a domain rule, a `@QuarkusTest` only when the defect involves HTTP, persistence, or CDI wiring.
2. Run it and confirm it fails for the reported reason, not for a setup error.

## 2. Locate the owner

| Symptom | Owning layer and class |
|---|---|
| A value is accepted that should be refused, or the reverse | The value object or the aggregate method |
| An invariant breaks across several aggregates | A domain service, called by the application service |
| Wrong HTTP status or error body | The exception's shared base class, or a missing context exception |
| HTTP 500 for an expected failure | A missing domain exception: the code threw a generic one or none |
| A field is lost or wrong after saving or loading | The persistence assembler or converter |
| Duplicate rows, update fails on existing data | The repository adapter's `save` (persist vs merge) |
| Lazy-loading error or N+1 queries | The Panache query (add `left join fetch`) |
| Wrong data from another context | The `External<Context>Service` translation, or the supplier's facade |
| Data of another organization or establishment visible | The repository port method missing the `OrganizationId`, or `AccessPolicy` not checked |
| HTTP 500 on a duplicate or overlapping row | The repository adapter not translating the constraint's SQL state |
| Duplicate WhatsApp message, reminder or assistant turn | The idempotency key (`wamid`, one row per appointment and kind, one turn per message) or its unique constraint |
| Message, reminder or turn never sent | The job's claim query or the after-commit wake-up; the row must still be `pending` |
| Wrong day or hour in a booking rule | The conversion to the establishment's `ZoneId`; storage stays UTC |
| Request rejected before reaching the service | The resource's Bean Validation annotations |

## 3. Fix in place

- Change the owner. Do not add an `if` in a controller, a `try/catch` that hides the exception, or a second validation in another layer to mask the defect.
- Keep the public method names and the package layout. A fix that needs a new class or layer follows [takodu-feature](../takodu-feature/SKILL.md) for that part.
- If the fix changes an HTTP status, an error `code`, or a response field, treat it as a contract change: update the controller's `@APIResponse` annotations and say so in the pull request.
- If the same mistake exists in sibling classes, fix them in the same change or list them in the pull request.

## 4. Prove

1. The new test passes and the rest of `./mvnw test` passes, including `ArchitectureTest`.
2. Undo the fix once and confirm the new test fails again, then restore the fix.
3. Commit with `fix(<context>): <what now works>`. The cause, the rule id and the evidence go in the pull request's Proof block, for example: `CustomerTest and CustomersControllerTest pass; reverting the constraint translation makes CustomersControllerTest fail with 500.`
