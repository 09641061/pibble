# Decision: Customer-facing assistant with in-process tools bound to the conversation

Status: implemented

## Problem

The business pain is answering customers on WhatsApp all day. The legacy assistant faced staff inside the app, routed by keywords and ran two tool-calling mechanisms side by side. A customer-facing assistant must never reach another customer's or business's data, whatever the customer writes.

## Decision

The assistant answers customers on the establishment's WhatsApp line. It uses one mechanism: LangChain4j tool calling in the same process, with `@Tool` methods in `assistant.application.internal.tools` that call `External<Context>Service` classes, and the AI service in `assistant.infrastructure.llm` behind the `AssistantModel` port. Before the model runs, `TurnScope` binds the turn's establishment, customer and conversation, and `TenantContext` binds the assistant to that establishment ([bound tenant](2026-10-22-assistant-acts-as-a-bound-tenant.md)). **No tool takes an organization, establishment or customer id**, only names, local times and positions in a list. Every write goes through the `booking` facade, so the assistant obeys the same `AvailabilityPolicy` and constraints as staff. Cut-offs (30 minutes lead, 60 days ahead, 2 hours before changing) are enforced in `booking`, never only in the prompt. Each `AssistantTurn` records model, tokens and tool calls. The model is any OpenAI-compatible provider, set by `quarkus.langchain4j.openai.*` properties.

## Alternatives considered

**A staff-facing assistant, as in the legacy.** It saves clicks for people already in the app, not the hours spent answering customers.

**An MCP server for the tools.** MCP exposes tools across process boundaries; here the tools are methods in the same application, so it adds a protocol hop and a deployment for nothing.

**Tools that take ids, with the prompt naming the tenant.** A manipulated prompt could then address another customer or business.

## Consequences

`ArchitectureTest.assistant_tools_take_no_ids` rejects any tool parameter that is a UUID, an `*Id` type or named `*Id`. Because tools write in booking's own transactions, a turn cannot hold its row lock while the model thinks: it is claimed in one transaction, runs outside any, and is recorded in another, with `started_at` as a 5-minute lease for a crashed worker. Tests replace the model with `ScriptedChatModel`, whose scripted tool calls book through the real facade (`TurnWorkerTest`), and a booking 20 minutes ahead comes back from `booking` as `OUTSIDE_BOOKING_WINDOW`, which the tool turns into text for the model. Changing the model needs no code; comparing candidates uses the token counts in `assistant_turns`.
