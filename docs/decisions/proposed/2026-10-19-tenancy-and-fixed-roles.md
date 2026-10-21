# Decision: Tenancy from the membership, with three fixed roles

Status: proposed

## Problem

Every request acts inside one organization. The legacy tenant filter checked `owner_id = current user`, so staff could never use the system, and it shipped 22 permission codes with custom roles for teams of two to ten people, half of them never wired.

## Proposal

The client sends `X-Organization-Id`. `organization.interfaces.rest.filters.TenantFilter` loads the caller's active membership (role and assigned establishments) and fills the request-scoped `shared.application.tenancy.TenantContext`, or answers 403. Command services call `AccessPolicy` with the action, which throws `AccessDeniedException`. Roles are `owner`, `manager` and `staff`, with a fixed permission table in code that follows the product docs' roles table; `staff` sees only assigned establishments.

Every repository port method takes the `OrganizationId` (or an establishment checked against it); there is no unscoped `findById`. Webhooks and jobs have no `TenantContext` and derive the organization from the WhatsApp line or the appointment. Organization, role, owner and creator ids never come from a request body.

## Alternatives considered

**Custom roles with permission codes.** Flexible, but a large model, UI and test surface for teams that need three roles; it can be added later behind `AccessPolicy` without changing callers.

**Tenant from a JWT claim.** Fewer queries, but a member removed or moved between establishments keeps access until the token expires, and a user in several organizations needs one token per organization.

**Row-level security in PostgreSQL.** Strong isolation, but session variables per transaction and policies per table add moving parts that scoped repository methods already cover.

## Acceptance criteria

- A request without a membership for the header's organization gets 403 with code `ACCESS_DENIED`.
- A `staff` member gets 403 or 404 for an establishment they are not assigned to.
- No repository port has a lookup method without an `OrganizationId` or establishment parameter.
