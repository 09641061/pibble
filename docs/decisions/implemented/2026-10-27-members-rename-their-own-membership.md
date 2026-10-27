# Decision: Members rename their own membership

Status: implemented

## Problem

The name the team sees is `members.display_name`, set by whoever invited the person and changed only through `PUT /api/v1/members/{id}`, which needs `MANAGE_MEMBERS` (owner or manager). Staff could not fix their own name, and the app shows that name in the profile menu, the calendar and the inbox. `iam` also keeps an optional `users.name` that no screen edits.

## Decision

`PUT /api/v1/members/me` with `{ displayName }` renames the caller's membership in the organization of `X-Organization-Id`, for any role and while the plan is inactive. The member comes from `TenantContext.memberId()`, never from the body; bookability, role and establishments stay with owners and managers. `PUT` replaces one value, so a repeat is harmless and no `Idempotency-Key` is needed. A person in several organizations renames each membership separately.

## Alternatives considered

**One name per user in `iam`** (`PUT /api/v1/me` on `users.name`), copied to every membership. One edit for everyone, but `iam` may not call `organization` on the context map, each business may know the person by a different name, and the copy would overwrite names owners chose.

**Letting staff call `PUT /api/v1/members/{id}` on themselves.** No new endpoint, but that resource also carries role and establishments, so the service would need field-by-field rules about who may change what.

## Consequences

The profile menu edits the name of the current organization only, and says so. `users.name` stays unused by the app.
