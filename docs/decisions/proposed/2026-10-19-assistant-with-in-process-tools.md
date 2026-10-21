# Decision: Customer-facing assistant with in-process tools bound to the conversation

Status: proposed

## Problem

The business pain is answering customers on WhatsApp all day. The legacy assistant faced staff inside the app, routed by keywords and ran two tool-calling mechanisms side by side. A customer-facing assistant must never reach another customer's or business's data, whatever the customer writes.

## Proposal

The assistant answers customers on the establishment's WhatsApp line. It uses one mechanism: LangChain4j tool calling in the same process, with `@Tool` methods in `assistant.application.internal.tools` that call `External<Context>Service` classes, and the AI service in `assistant.infrastructure.llm` behind a port. Before the model runs, the establishment and customer are bound from the conversation; **no tool takes an organization, establishment or customer id**, only names and local times. Every write goes through the `booking` facade, so the assistant obeys the same `AvailabilityPolicy` and constraints as staff. Cut-offs (30 minutes lead, 60 days ahead, 2 hours before changing) are enforced in `booking`, never in the prompt. Each `AssistantTurn` records model, tokens and tool calls.

## Alternatives considered

**A staff-facing assistant, as in the legacy.** It saves clicks for people already in the app, not the hours spent answering customers.

**An MCP server for the tools.** MCP exposes tools across process boundaries; here the tools are methods in the same application, so it adds a protocol hop and a deployment for nothing.

**Tools that take ids, with the prompt naming the tenant.** A manipulated prompt could then address another customer or business.

## Acceptance criteria

- No `@Tool` method has an organization, establishment or customer id parameter.
- An agent test with a fake chat model that issues scripted tool calls books through the `booking` facade and records the turn.
- A booking 20 minutes ahead by the assistant is refused by `booking` with a domain exception.
