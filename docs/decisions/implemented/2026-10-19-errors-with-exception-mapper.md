# Decision: Errors with domain exceptions and one exception mapper

Status: implemented

## Problem

An expected failure (unknown customer, phone already registered, slot taken, closed customer service window, inactive plan) must reach the client with the right HTTP status and a stable code. If each controller picks the status, responses diverge; if the code throws generic exceptions, Quarkus answers 500 to client errors.

## Decision

Expected failures are domain exceptions. `shared.domain.exceptions` defines `DomainException` and its bases: `ResourceNotFoundException` (404), `ConflictException` (409), `BusinessRuleViolationException` (422), `AccessDeniedException` (403), `PlanInactiveException` (402, rule O6), `AuthenticationFailedException` (401, a wrong or expired sign-in code or refresh token) and `TooManyRequestsException` (429, a rate limit from a business rule such as I3). Each context declares its own exceptions in `<context>.domain.exceptions` extending a base. Value objects and commands throw `IllegalArgumentException` (400).

`shared.interfaces.rest.errors.GlobalExceptionMapper` is the only class that translates these types to HTTP, with the body `ErrorResource(code, message)`. Unexpected exceptions are not mapped: Quarkus logs them and answers 500. Database constraint violations are not mapped either; the repository adapter that triggers them translates them into a context exception. `GlobalExceptionMapperTest` pins the mapping.

## Alternatives considered

**`Result<T, ApplicationError>` as in learning-center-platform.** It makes failures explicit in the signature, but every controller needs a response assembler and every composition a `flatMap`, and Quarkus transactions roll back on exceptions, not on return values. The error code is kept in `DomainException.code()`.

**JAX-RS exceptions (`NotFoundException`, `WebApplicationException`) in services and domain.** It couples the domain to HTTP, and the purity rule of `ArchitectureTest` rejects it.

**`IllegalArgumentException` for everything, as in fudi.** Without a mapper, a duplicate phone arrives as 500, and the client cannot tell a conflict from invalid data.

**Mapping SQL states (`23505`, `23P01`) globally.** It would turn every unique violation into the same generic 409 and hide which rule failed; the adapter knows the constraint and can raise `SlotUnavailableException` or `CustomerAlreadyRegisteredException`.

## Consequences

Each new failure needs a small exception class, and choosing the wrong base changes the HTTP status. In exchange, services return the aggregate directly, controllers have no error branches and the error contract lives in one file.
