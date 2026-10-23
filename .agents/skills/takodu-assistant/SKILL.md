---
name: takodu-assistant
description: How the customer-facing AI booking assistant of the pibble backend is built with quarkus-langchain4j — AssistantTurn lifecycle and TurnWorker, the TurnScope that binds establishment and customer before the model runs, @Tool methods that never take ids, the AI service behind a port, chat memory from the last messages, reply rendering as buttons or lists, handoff and pause, guardrails, cost recording, and tests with a fake chat model. Use for any code in the assistant context, any change to a tool, prompt or model setting, or any assistant behavior bug.
---

# The assistant in pibble

The assistant answers customers on the establishment's WhatsApp line and books into the real calendar; it is not a general chatbot. The decision is [customer-facing assistant with in-process tools](../../../docs/decisions/implemented/2026-10-19-assistant-with-in-process-tools.md); the product design is `../docs/08-ai-assistant.md`; rules A1–A9 and B9, B14 are in `../docs/03-business-domain.md`. Package names are relative to `com.takodu.api.assistant`.

## Rules that do not bend

1. **The model never chooses the tenant.** `TurnScope` binds organization, establishment, conversation and customer from the turn before the model runs. **No `@Tool` method takes an organization, establishment, customer, member or appointment id**; `ArchitectureTest` rejects `UUID` and `*Id` types and parameters named `*Id`. Tools take names, local dates and times, and an appointment's position in the last `myAppointments` list.
2. **Same rules as staff.** Every write goes through `ExternalBookingService` → `BookingContextFacade`, which runs `AvailabilityPolicy` and the database constraints. Cut-offs (30 minutes lead, 60 days ahead, 2 hours before a change) live in `booking`, never only in the prompt.
3. **Tools, not knowledge.** Services, prices, hours and free slots come from a tool call or the owner's business info; the prompt forbids inventing them.
4. **One mechanism.** LangChain4j tool calling in process; no keyword router, no MCP.
5. **Humans win.** A human message pauses the assistant 12 hours (A3); staff pause it until resumed (`/assistant/pause`), and `/assistant/resume` clears every pause and a handoff. A paused conversation gets no turn.

## Where things live

| Class | Package | Job |
|---|---|---|
| `InboundMessageReceivedEventHandler` | `application.internal.eventhandlers` | In transaction: enqueue a turn if A2 holds; after commit: wake the worker |
| `TurnWorker` | `application.internal.jobs` | Up to 4 virtual threads call `RunNextTurnCommand` ([takodu-jobs](../takodu-jobs/SKILL.md)) |
| `AssistantTurnCommandServiceImpl` | `application.internal.commandservices` | Claims, runs and records a turn |
| `AnswerPolicy` | `domain.services` | A2, checked at enqueue and at claim |
| `TurnScope` | `application.internal.tools` | Request-scoped: the bound turn, establishment, numbered appointments, tool count, handoff request |
| `BookingTools` | `application.internal.tools` | The `@Tool` methods; call `External*Service` classes only |
| `AssistantModel` (port) | `application.internal.outboundservices.llm` | `ModelAnswer answer(TurnPrompt prompt)` |
| `LangChain4jAssistantModel`, `BookingAgent` | `infrastructure.llm` | Builds an AI service per turn with `AiServices` and the injected `ChatModel` |
| `prompts/booking-agent.txt` | `src/main/resources` | System prompt template |

`dev.langchain4j` imports are allowed in `application.internal.tools` (the `@Tool` and `@P` annotations) and `infrastructure.llm`, never in the domain. Booking checks the assistant like staff: the turn calls `TenantContext.bindAssistant`, and `AccessPolicy` allows it `BOOK_APPOINTMENTS` in its own establishment ([decision](../../../docs/decisions/implemented/2026-10-22-assistant-acts-as-a-bound-tenant.md)).

## A turn

1. **Claim** (one transaction): lock the next due turn (`FOR UPDATE SKIP LOCKED`); skip it if the customer wrote again (the newer turn answers both), if an earlier reply's `answered_through` covers it, if A2 fails, or at 30 answered turns a day (handoff). Otherwise `begin()`: `running`, with `started_at` as a 5-minute lease.
2. **Run** (no transaction, own request context): bind the assistant and `TurnScope`; load the last 20 messages; the trailing customer messages are the user message and the rest is memory. Each tool call commits or fails in booking's own transaction, so a refused booking never spoils the turn.
3. **Reply:** `ConversationsContextFacade.sendAssistantReply` with a key derived from the turn; conversations drops it if a person wrote meanwhile. Then the handoff, if a tool asked for one.
4. **Record** (one transaction): `complete(usage, toolCalls, answeredThrough)`, `drop(...)` when the reply was dropped, or `fail(error)`: back to `pending` after 30 s × attempts, `failed` and handed off at the third (A7).

Audio, images and documents skip the model: a fixed reply and a handoff.

## Writing a tool

```java
@Tool("Busca horarios libres para un servicio en una fecha. Fecha en formato YYYY-MM-DD.")
public String findSlots(String serviceName, String date,
                        @P(value = "Nombre del profesional", required = false) String staffName) {
    var service = organization.resolveService(scope.organizationId(), scope.establishmentId(), serviceName);
    if (service.isEmpty()) return "No existe ese servicio. Servicios: " + organization.serviceNames(scope.organizationId(), scope.establishmentId());
    var slots = booking.findSlots(scope, service.get(), LocalDate.parse(date), organization.resolveStaff(scope, staffName));
    return slots.isEmpty() ? "No hay horarios libres ese día." : "Horarios libres: " + formatTimes(slots, scope.zone());
}
```

- Descriptions and results are short Spanish text written for the model; times are local to the establishment.
- **Business failures are returned as text, never thrown**: map the context exception to the reason plus the next step ("Ese horario ya no está disponible. Horarios libres: 10:30, 11:00"). Only infrastructure errors fail the turn.
- A request past the B14 cut-off, a complaint, a request for a person, unreadable media or two failed attempts ends with `handOffToHuman(summary)` (A7).
- Every write tool passes an `IdempotencyKey` derived from `scope.turnId()`, the tool name and its arguments, so a retried turn never books, reschedules or cancels twice.
- Never put the customer's phone in a tool result or the prompt.

## Reply rendering

The model returns `AgentReply(text, options)` as JSON. `AssistantReply.format()` holds the 08 §5 rule: 1–3 options of ≤ 20 characters → buttons, ≤ 10 of ≤ 24 → list, otherwise numbered lines in `body()` (≤ 1,000 characters). `ExternalConversationService.sendReply` passes the choice as `ReplyChoicesView`; a tap returns as the next customer message.

## Guardrails

At most 8 tool calls per turn (`TurnScope`) and 30 answered turns per conversation per day, after which the conversation is handed off; `maxSequentialToolsInvocations(10)` is the hard stop. The model is configured only through `quarkus.langchain4j.openai.*` properties (any OpenAI-compatible provider), so changing it needs no code; compare candidates on recorded conversations by booking accuracy and cost per turn from `GET …/assistant/usage`.

## Tests

- **Agent with a fake chat model:** `ScriptedChatModel` (`@Alternative ChatModel`) answers each call with the next scripted step: `tool("bookAppointment", json)`, `reply(text, options...)` or `failure()`. `TurnWorkerTest` posts a webhook, calls `worker.runDue()` and `sender.sendDue()`, and asserts the appointment, the turn row (`AssistantRows`) and what `RecordingWhatsappGateway` sent. Call `AssistantRows.clearQueue()` first: turns of every test share one queue.
- **Facades:** `BookingContextFacadeTest` binds the assistant in its own request context and checks B9, B14 and that another customer's appointment is not found.
- **Tenancy:** a scripted call naming another establishment's service returns "No existe" and books nothing.
- No test calls a real LLM.

## Checklist

- [ ] No tool parameter is an id; every tool reads the tenant from `TurnScope`.
- [ ] Writes go through the `booking` facade; no rule is enforced only by the prompt.
- [ ] Business failures come back to the model as text; infrastructure failures fail the turn.
- [ ] The turn records model, tokens and tool calls, and re-checks A2 before running.
