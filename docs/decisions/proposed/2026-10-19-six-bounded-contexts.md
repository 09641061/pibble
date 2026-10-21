# Decision: Six bounded contexts

Status: proposed

## Problem

The legacy backend had thirteen contexts drawn by noun (profiles, media, audit, retention), while real rules (who performs a service, opening hours, customer identity) fell between them and ended stubbed. The team concept alone was split across four contexts, so when one was deleted the system silently became owner-only.

## Proposal

Pibble has six contexts, cut around invariants and language: `iam` (who can sign in), `organization` (organizations, establishments with opening hours, members with roles, services with their performers), `customers` (a person identified by phone in an organization), `booking` (appointments, time blocks, availability), `conversations` (WhatsApp lines, conversations, messages, scheduled messages) and `assistant` (settings and turns). The context map and the allowed dependencies are in [architecture.md](../../architecture.md) and in `ArchitectureTest.ALLOWED_DEPENDENCIES`.

- **Members live in `organization`, not `iam`:** a user is who can sign in; a member is who works here and with which role.
- **Services live in `organization`:** they are establishment configuration edited by the same people as opening hours and staff; a separate catalog had no invariants of its own.
- **`customers` is separate from `conversations`:** staff create customers without WhatsApp, and `booking` needs customers without chats.
- **`assistant` is separate from `conversations`:** the inbox works with the assistant disabled, and turns, the LLM and tools have their own lifecycle.

## Alternatives considered

**The legacy thirteen contexts.** Several were tables with a four-layer structure each, which multiplied files and still left the important rules without an owner.

**Merging `assistant` into `conversations`.** Fewer facades, but the reliable message pipe would carry the LLM's failure modes, and disabling the assistant would touch the inbox code.

## Acceptance criteria

- Every class under `com.takodu.api` belongs to one of the six contexts or `shared`, and `contexts_are_on_the_context_map` passes.
- `contexts_depend_only_along_the_context_map` and `no_cycles_between_contexts` pass with all six contexts present.
- Each table in the schema is written by exactly one context.
