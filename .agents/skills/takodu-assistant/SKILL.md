---
name: takodu-assistant
description: How the customer-facing AI booking assistant of the pibble backend is built with quarkus-langchain4j — AssistantTurn lifecycle and TurnWorker, the TurnScope that binds establishment and customer before the model runs, @Tool methods that never take ids, the AI service behind a port, chat memory from the last messages, reply rendering as buttons or lists, handoff and pause, guardrails, cost recording, and tests with a fake chat model. Use for any code in the assistant context, any change to a tool, prompt or model setting, or any assistant behavior bug.
---

# The assistant in pibble

The assistant answers customers on the establishment's WhatsApp line and books into the real calendar. It is a booking assistant for one business, not a general chatbot. The decision is [customer-facing assistant with in-process tools](../../../docs/decisions/proposed/2026-10-19-assistant-with-in-process-tools.md); the product design is `../docs/08-ai-assistant.md`; rules A1–A9 and B9, B14 are in `../docs/03-business-domain.md`. Package names are relative to `com.takodu.api.assistant`.

## Rules that do not bend

1. **The model never chooses the tenant.** `TurnScope` binds organization, establishment, conversation and customer from the turn before the model runs. **No `@Tool` method takes an organization, establishment, customer, member or appointment id.** Tools take names, local dates and times, and the position of an appointment in the last `myAppointments` list.
2. **Same rules as staff.** Every write goes through `ExternalBookingService` → `BookingContextFacade`, which runs `AvailabilityPolicy` and the database constraints. Cut-offs (30 minutes lead, 60 days ahead, 2 hours before a change) live in `booking`, never only in the prompt.
3. **Tools, not knowledge.** Services, prices, hours and free slots come from a tool call or the owner's business info; the prompt forbids inventing them.
4. **One mechanism.** LangChain4j tool calling in process. No keyword router, no parsing tool calls out of text, no MCP.
5. **Humans win.** Any human message pauses the assistant on that conversation (A3); the assistant never answers a paused conversation.

## Where things live

| Class | Package | Job |
|---|---|---|
| `InboundMessageReceivedEventHandler` | `application.internal.eventhandlers` | In transaction: if A2 holds, `AssistantTurn.enqueue` (unique per trigger message) |
| `TurnWorker` | `application.internal.jobs` | Claims a pending turn, re-checks A2, runs it, records the outcome ([takodu-jobs](../takodu-jobs/SKILL.md)) |
| `TurnScope` | `application.internal.tools` | Holds the bound ids, `ZoneId` and the appointment list of the current turn |
| `BookingTools` | `application.internal.tools` | The `@Tool` methods; call `External*Service` classes only |
| `AssistantModel` (port) | `application.internal.outboundservices.llm` | `AssistantReplyResult reply(TurnPrompt prompt)` |
| `BookingAgent`, `TurnMemoryProvider`, `LangChain4jAssistantModel` | `infrastructure.llm` | `@RegisterAiService`, chat memory, port adapter |
| `prompts/booking-agent.txt` | `src/main/resources` | System prompt template |

`dev.langchain4j` imports are allowed in `application.internal.tools` (the `@Tool` and `@P` annotations) and `infrastructure.llm`, never in the domain.

## A turn

1. `TurnWorker` locks the next `pending` turn with `FOR UPDATE SKIP LOCKED`, skipping conversations that already have a `running` turn; when several turns of one conversation are pending, only the newest runs and the older ones become `skipped`.
2. It reloads the conversation context through `ExternalConversationService` and re-checks A2: assistant enabled, plan active, line connected, window open, not paused. Failing → `skip(reason)`.
3. It fills `TurnScope`, then calls `AssistantModel.reply` with the business name, establishment name, local date, business info and the latest customer message. `TurnMemoryProvider` pre-fills memory with the last 20 messages of the conversation; tool calls stay in that memory and are discarded after the turn.
4. It sends the reply through `ConversationsContextFacade.sendAssistantReply` and calls `turn.complete(model, tokens, toolCalls, now)` (A9).
5. A model or tool infrastructure error calls `turn.fail(error)`: back to `pending` up to 3 attempts, then `failed` and `handOff`.

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
- **Business failures are returned as text, never thrown**: catch the context exception (`SlotUnavailableException`, `AppointmentChangeTooLateException`) and return the reason plus the next step ("Ese horario ya no está disponible. Horarios libres: 10:30, 11:00"). Only infrastructure errors escape and fail the turn.
- A request past the B14 cut-off, a complaint, a request for a person, unreadable media or two failed attempts ends with `handOffToHuman(summary)` (A7).
- Every write tool passes an `IdempotencyKey` derived from `scope.turnId()`, the tool name and its arguments, so a retried turn never books, reschedules or cancels twice.
- Never put the customer's phone number in a tool result or the prompt.

## Reply rendering

The AI service returns `AssistantReply(String text, List<String> options)`. Empty options → text; 1–3 options of ≤ 20 characters → reply buttons; up to 10 of ≤ 24 → list; otherwise numbered lines in the text. Replies are capped at 1,000 characters and one message per turn.

## Guardrails

At most 8 tool calls per turn and 30 assistant turns per conversation per day, after which the conversation is handed off. The model is configured only through `quarkus.langchain4j.openai.*` properties (any OpenAI-compatible provider), so changing it needs no code; compare candidates on recorded conversations by booking accuracy and cost per turn from `assistant_turns`.

## Tests

- **Tools without a model:** call `BookingTools` methods directly with a filled `TurnScope` and fake `External*Service` classes; assert the returned text and the booking calls.
- **Agent with a fake chat model:** a `ChatModel` test double that issues scripted tool calls (`findSlots`, then `bookAppointment`) and a final reply; assert the appointment exists with `source=assistant`, the turn is `done` with token counts, and one message was queued.
- **Tenancy:** a scripted call naming another establishment's service returns "no existe" and books nothing.
- **Cut-offs:** a scripted booking 20 minutes ahead is refused by `booking` and the reply text says so.
- No test calls a real LLM.

## Checklist

- [ ] No tool parameter is an id; every tool reads the tenant from `TurnScope`.
- [ ] Writes go through the `booking` facade; no rule is enforced only by the prompt.
- [ ] Business failures come back to the model as text; infrastructure failures fail the turn.
- [ ] The turn records model, tokens and tool calls, and re-checks A2 before running.
