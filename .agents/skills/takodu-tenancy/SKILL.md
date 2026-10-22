---
name: takodu-tenancy
description: How a request in the pibble backend learns who is calling and in which organization — JWT user, X-Organization-Id, TenantFilter, the request-scoped TenantContext, AccessPolicy with the three fixed roles, establishment visibility for staff, organization-scoped repositories, and the TestTenants fixture. Use when building or touching any endpoint, command service, repository port, permission check, webhook or job that must find its organization, or when fixing a cross-tenant leak or a wrong 403/404.
---

# Tenancy and access in pibble

Every row belongs to one organization, and a caller may act only inside the organizations they are a member of. The decision is [tenancy and fixed roles](../../../docs/decisions/implemented/2026-10-19-tenancy-and-fixed-roles.md); the role table is in `../docs/03-business-domain.md` §5. Package names are relative to `com.takodu.api`.

## The pieces and where they live

| Piece | Package | Job |
|---|---|---|
| `TenantContext` | `shared.application.tenancy` | `@RequestScoped`: `userId()`, `organizationId()`, `memberId()`, `role()`, `establishmentIds()`, `canSee(EstablishmentId)` |
| `Role` | `shared.domain.model.valueobjects` | `owner`, `manager`, `staff`; the `Member` aggregate and `TenantContext` share it |
| `Action`, `AccessPolicy` | `shared.application.tenancy` | Fixed permission table; `require(Action)` and `requireEstablishment(EstablishmentId)` throw `AccessDeniedException` |
| `TenantFilter` | `organization.interfaces.rest.filters` | Reads the JWT subject and `X-Organization-Id`, loads the active membership, fills `TenantContext` or answers 403 |
| `TestTenants` | `src/test/java/com/takodu/api/shared/testing` | Creates user, organization and membership per role through the endpoints; returns a `TestTenant` with `spec()` |

`TenantContext` sits in `shared` because every context reads it; only `organization` fills it, because `organization` owns memberships. `shared` never imports `organization`: the filter writes into the holder through its setter method `bind(...)`.

## Request path

1. SmallRye JWT verifies the access token; the subject is the `UserId`.
2. `TenantFilter` skips `/webhooks/**`, `/api/v1/auth/**` and `/q/**`. For the rest, a missing or malformed `X-Organization-Id` gives 400, and no active membership gives 403 `ACCESS_DENIED`.
3. The controller copies `tenantContext.organizationId()` (and `memberId()`/`userId()` when the command records an author) into the command. **It never reads them from the body or a path parameter.**
4. The command service calls `accessPolicy.require(Action.X)` first, then `requireEstablishment(id)` when the target belongs to one establishment, then loads the aggregate scoped by the organization.

```java
package com.takodu.api.shared.application.tenancy;

/** Fixed permissions per role (product docs §5). Changing a row is a product decision. */
public enum Action {
    MANAGE_ORGANIZATION(Role.OWNER),
    MANAGE_ESTABLISHMENTS(Role.OWNER),
    CONNECT_WHATSAPP(Role.OWNER),
    MANAGE_SERVICES_AND_HOURS(Role.OWNER, Role.MANAGER),
    MANAGE_MEMBERS(Role.OWNER, Role.MANAGER),
    MANAGE_ANY_CALENDAR(Role.OWNER, Role.MANAGER),
    MANAGE_OWN_TIME_BLOCKS(Role.OWNER, Role.MANAGER, Role.STAFF),
    BOOK_APPOINTMENTS(Role.OWNER, Role.MANAGER, Role.STAFF),
    REPLY_IN_INBOX(Role.OWNER, Role.MANAGER, Role.STAFF),
    EDIT_CUSTOMERS(Role.OWNER, Role.MANAGER, Role.STAFF),
    DELETE_CUSTOMERS(Role.OWNER, Role.MANAGER);

    private final Set<Role> allowed;

    Action(Role... allowed) { this.allowed = Set.of(allowed); }

    public boolean allows(Role role) { return allowed.contains(role); }
}
```

`AccessPolicy` (same package) checks `tenantContext.role()` against the table and `canSee(id)` for the establishment.

## Scoping rules

- **Every repository port method takes the `OrganizationId`**, or an `EstablishmentId` the service already checked belongs to the organization. `findById(id)` without a tenant does not exist; an id from another organization returns empty, which the controller turns into 404. Only job claims, webhook lookups and a signed-in user's lookups before choosing an organization (their memberships, an invitation token) go without it, annotated `shared.domain.repositories.CrossTenant`; `ArchitectureTest` enforces both. `iam` is exempt: a user belongs to no organization, so its repositories are scoped by `UserId` or by the code or token presented.
- List endpoints for establishment data (calendar, inbox) filter by `tenantContext.establishmentIds()` for `staff`.
- 404 versus 403: a row in another organization is 404 (do not reveal it exists); a row in the caller's organization that their role or establishment forbids is 403.
- Facades take `organizationId` as their first parameter, so the supplier keeps scoping its own queries.

## No request, no TenantContext

Webhooks, scheduled jobs and integration event handlers run without a `TenantContext`; injecting it there fails with `ContextNotActiveException`. They derive the organization from the data they process:

| Entry point | Organization comes from |
|---|---|
| WhatsApp webhook | The `WhatsappLine` found by `phone_number_id` → its establishment → organization |
| `ScheduledMessageDispatcher` | The appointment of the scheduled row |
| `TurnWorker` | The conversation of the turn; it then binds the assistant (below) |
| Event handler | The `organizationId` carried by the integration event |

Services called from both a request and a job take the `OrganizationId` as a command field and do not call `AccessPolicy`; the controller-facing service checks access before delegating.

**The assistant is the exception.** A turn activates a request context and calls `TenantContext.bindAssistant(organization, establishment)`. The assistant has no user, member or role (reading them throws); `AccessPolicy` allows it only `Action.allowsAssistant()` actions, in its own establishment, so booking checks it like staff.

## Testing

- Endpoint tests use `TestTenants.owner()`, `TestTenants.managerOf(tenant)` and `TestTenants.staffOf(tenant, establishmentId)`, never hand-built JWTs.
- For every endpoint with a role restriction, test one forbidden role (403 `ACCESS_DENIED`) and one id from a second organization (404).
- Domain tests need none of this: tenancy is application and interfaces only.

## Checklist

- [ ] No organization, role, member or author id is read from a request body.
- [ ] The command service checks `AccessPolicy` before loading or changing state.
- [ ] Every new repository method is scoped by organization or by a checked establishment.
- [ ] Jobs, webhooks and handlers derive the organization from data and never inject `TenantContext`.
- [ ] Tests cover a forbidden role and a foreign organization.
