# Decision: Webhooks store first; outgoing work is polled from tables

Status: proposed

## Problem

WhatsApp is the core of the product and must never lose a message. Meta expects a fast 200 on webhooks and redelivers, while LLM calls and Meta sends are slow and fail. The legacy published inbound messages as in-memory events nobody listened to, so every message was lost.

## Proposal

`WebhookController` verifies the signature on the raw bytes, and `conversations` stores the customer, conversation and message in one transaction, skipping a `wamid` that already exists, before answering 200. Work that follows is a row: `scheduled_messages` for confirmations, reminders and cancellations, `assistant_turns` for replies. In-transaction integration events insert those rows; after-commit events only wake the jobs (`ScheduledMessageDispatcher`, `TurnWorker`, `MediaDownloader`), which also poll and claim rows with `SELECT … FOR UPDATE SKIP LOCKED`. A send is retried up to 5 times, then marked `failed` and shown in the inbox.

## Alternatives considered

**Process the assistant inside the webhook.** Simple, but LLM latency delays the 200, and an LLM failure could lose the stored message.

**Asynchronous CDI events or a broker.** Work in flight disappears on a crash, or a broker joins the infrastructure; tables give durability with the database already there.

## Acceptance criteria

- Replaying the same webhook payload twice stores one message.
- Killing the process between commit and send leaves a pending row that the job sends after restart.
- A webhook answers 200 without waiting for any LLM or Meta call.
