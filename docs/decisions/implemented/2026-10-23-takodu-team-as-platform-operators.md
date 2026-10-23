# Decision: Takodu team as platform operators

Status: implemented

## Problem

The Takodu team sets plans after a bank transfer, Yape or Plin payment and answers questions about usage (03 §2). It acts on every organization but belongs to none, so `TenantFilter` and `AccessPolicy` cannot admit it. A payment dispute also needs to know which person changed a plan, not only that someone did.

## Decision

The team uses the same passwordless sign-in as everyone else. A user with `users.platform_operator = true` may call `/internal/v1/**`; only the database grants the flag (`UPDATE users SET platform_operator = true WHERE email = …`). `iam`'s `OperatorFilter` reads the flag and `disabled_at` from the database on every internal request, answers 401 without a token and 403 for anyone else, and binds the user in `OperatorContext` (`shared.application.tenancy`), which internal endpoints use to record who acted. `TenantFilter` covers only `/api/v1`, so no `TenantContext` is bound and `X-Organization-Id` is ignored there.

## Alternatives considered

**A shared admin token from configuration** (`TAKODU_ADMIN_TOKEN`, compared in constant time). No column and no sign-in, but every operator shares one secret: the plan history could not say who acted, rotating it locks everyone out at once, and a leaked token never expires.

**A role claim in the access token.** Saves one query per internal request, but a removed operator would keep access until the token expires, and iam would need to know about platform roles when it issues tokens.

**Editing the database by hand only**, as the product docs first allowed. Nothing to build, but no idempotency, no history, and every plan change is a hand-written `UPDATE` on production.

## Consequences

Operators sign in with their email like any user; an operator who is also a member of a business uses one token for both APIs. Granting and removing the flag is a database operation, which suits a team of a few people. Disabling the user (I6) cuts internal access on the next request. Internal endpoints add one primary-key lookup on `users` per request.
