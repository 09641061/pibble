# Decision: Tenancy from the membership, with three fixed roles

Status: implemented

## Problem

Every request acts inside one organization. The legacy tenant filter checked `owner_id = current user`, so staff could never use the system, and it shipped 22 permission codes with custom roles for teams of two to ten people, half of them never wired.

## Decision

The client sends `X-Organization-Id`. `organization.interfaces.rest.filters.TenantFilter` loads the caller's active membership (role and assigned establishments) and binds the request-scoped `shared.application.tenancy.TenantContext`; a missing or malformed header is 400 and a missing membership is 403 `ACCESS_DENIED`. The filter skips sign-in (`/api/v1/auth/**`), listing and creating the caller's organizations (`/api/v1/organizations`) and accepting an invitation, which all come before the caller is a member. Command services call `AccessPolicy.require(Action)` and `requireEstablishment(id)`, which throw `AccessDeniedException`. Roles are `owner`, `manager` and `staff` (`shared.domain.model.valueobjects.Role`), with the fixed permission table of the product docs' roles table in `Action`; `staff` see only their assigned establishments.

Every repository port method takes the `OrganizationId` (or an establishment checked against it); there is no unscoped `findById`. The few lookups that cannot have one (a user's memberships before choosing an organization, an invitation token, job claims, webhooks) are marked `@CrossTenant` with the reason, and `ArchitectureTest` enforces both. Organization, role, owner and creator ids never come from a request body. Tests create tenants through the real endpoints with `TestTenants`.

## Alternatives considered

**Custom roles with permission codes.** Flexible, but a large model, UI and test surface for teams that need three roles; it can be added later behind `AccessPolicy` without changing callers.

**Tenant from a JWT claim.** Fewer queries, but a member removed or moved between establishments keeps access until the token expires, and a user in several organizations needs one token per organization.

**Row-level security in PostgreSQL.** Strong isolation, but session variables per transaction and policies per table add moving parts that scoped repository methods already cover.

## Consequences

Each request costs one indexed membership lookup, and a removed member loses access on their next request instead of when the token expires. An id from another organization is 404 and a forbidden role or establishment in the caller's own organization is 403, so ids never reveal other tenants. Every new endpoint must decide its `Action`, and every new repository method its scope; the build fails when the scope is missing.
