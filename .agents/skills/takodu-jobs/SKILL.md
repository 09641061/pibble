---
name: takodu-jobs
description: How background work runs in the pibble backend — work stored as rows (scheduled_messages, assistant_turns, pending media), @Scheduled jobs that claim rows with SELECT FOR UPDATE SKIP LOCKED, after-commit wake-ups, retries with attempt limits, idempotency keys, the injected Clock, and how to test jobs. Use when adding or changing ScheduledMessageDispatcher, TurnWorker, MediaDownloader, CleanupJob or any other scheduled or deferred work, when a call to Meta, the LLM or Resend must happen after a user action, or when work is lost, duplicated or stuck.
---

# Background jobs in pibble

Slow or failing work never runs inside a request or webhook transaction. The transaction inserts a row; a job claims the row and does the work. The decision is [webhooks store first; outgoing work is polled from tables](../../../docs/decisions/proposed/2026-10-19-webhooks-store-first-and-polled-outbox.md). Package names are relative to `com.takodu.api`.

## The jobs

| Job | Context | Row | Trigger | Work |
|---|---|---|---|---|
| `ScheduledMessageDispatcher` | `conversations` | `scheduled_messages` (`pending`, `send_at <= now`) | every 30 s | Send confirmations, reminders and cancellations (W11–W15) |
| `OutboundMessageSender` | `conversations` | `messages` (`pending`, outbound) | after-commit wake-up + every 10 s | Send staff and assistant messages to Meta |
| `MediaDownloader` | `conversations` | `messages` with `media_id` and no `media_path` | after-commit wake-up + every 60 s | Download inbound media to the media volume |
| `TurnWorker` | `assistant` | `assistant_turns` (`pending`) | after-commit wake-up + every 10 s | Run the assistant, at most 4 turns in parallel |
| `CleanupJob` | `identity` | expired `login_codes`, `sessions` | daily | Delete them |

Jobs live in `<context>.application.internal.jobs`, use `quarkus-scheduler` (`@Scheduled(every = "30s", concurrentExecution = SKIP)`), and call ports for external systems, never infrastructure classes.

## Shape of a job

```java
package com.takodu.api.conversations.application.internal.jobs;

@ApplicationScoped
public class ScheduledMessageDispatcher {
    private static final int BATCH = 20;

    private final ScheduledMessageCommandService scheduledMessageCommandService;
    private final Clock clock;

    // constructor injection

    /** Each call handles at most one due row in its own transaction; stops when none is left. */
    @Scheduled(every = "30s", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void dispatchDue() {
        for (int i = 0; i < BATCH; i++) {
            try {
                if (!scheduledMessageCommandService.handle(new DispatchNextDueMessageCommand(clock.instant()))) return;
            } catch (RuntimeException e) {
                Log.warnf(e, "Scheduled message dispatch failed");
            }
        }
    }
}
```

- **One row per transaction.** The `@Transactional` handler locks the next due row with `SELECT … WHERE status = 'pending' AND send_at <= :now ORDER BY send_at LIMIT 1 FOR UPDATE SKIP LOCKED`, so two instances never take the same row and no lease column is needed. The repository port method that runs it is annotated `@CrossTenant`, since the job has no organization until it holds the row.
- **Re-check before acting.** The command handler reloads the row and re-checks the rule that allowed it: appointment still `confirmed`, plan active (O6), line connected (W15), window open for free-form text (W6), conversation not paused (A2). If the rule no longer holds, it marks the row `cancelled` or `skipped` and stops.
- **Call the port inside that transaction, with a short client timeout**, and record the outcome before commit: `sent` with the `wamid`, or `recordFailure()`. Holding one row lock for one HTTP call is fine at this scale. If the process dies after Meta accepted but before commit, the retry sends again; that rare duplicate is accepted. Webhooks never call an external system. A request calls one only when the user waits for that result (connecting a WhatsApp line, emailing a sign-in code or an invitation), and never while a database transaction is open; everything else is a row that a job processes.
- **Retries are counted on the row**: `scheduled_messages` and outbound messages stop at 5 attempts (`failed`, shown in the inbox, W14); turns stop at 3 and hand the conversation off (A7). A failure never escapes the job loop; it is logged and the next row runs. A crash rolls the row back to `pending`, so nothing depends on an in-memory flag.

## Wake-ups

A request that creates work fires an integration event or a context-internal CDI event; an `@Observes(during = AFTER_SUCCESS)` handler calls the job's `wake()` method, which runs one claim cycle on a virtual thread. The wake-up is an optimization: if it is lost, the schedule picks the row up. **Never put the work itself in the after-commit handler.**

## Idempotency

| Work | Key that makes repeats harmless |
|---|---|
| Staff action from the app | `Idempotency-Key` header stored on the row or as `last_idempotency_key` |
| Assistant write | key derived from turn id, tool and arguments |
| Inbound message | unique `wamid` |
| Notification per appointment | unique (`appointment_id`, `kind`) |
| Assistant turn | unique `trigger_message_id` |
| Status webhook | `DeliveryStatus.advanceTo` only moves forward (W10) |

Insert with the domain check first; the unique constraint catches the race ([takodu-migrations](../takodu-migrations/SKILL.md)).

## Time

Inject `java.time.Clock` (UTC, from `shared.infrastructure.time.ClockProducer`), pass `clock.instant()` into domain methods, and never call `Instant.now()` in a job or domain class. In a `@QuarkusTest` the clock is `TestClock`: `set` or `advance` it past `send_at` or a 12-hour pause, then `reset()` it.

## Tests

- Domain: the row aggregate's transitions (`reminderFor`, `reschedule`, `recordFailure` reaching `FAILED`) with plain JUnit.
- Job: a `@QuarkusTest` that inserts a due row, calls `dispatchDue()` directly (do not wait for the schedule), and asserts the row state and the fake port's calls. Disable schedules in tests with `quarkus.scheduler.enabled=false` in the `%test` profile.
- Failure: make the fake port throw and assert `attempts` grows and stops at the limit.

## Checklist

- [ ] The request or webhook only inserts a row; no external call happens inside its transaction.
- [ ] Each row is locked with `FOR UPDATE SKIP LOCKED` in its own transaction, with a batch limit per run.
- [ ] The handler re-checks the business rule before acting.
- [ ] Attempts and the final failed state are on the row and visible to staff.
- [ ] The work has an idempotency key backed by a unique constraint.
