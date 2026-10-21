# Decision: Webhooks store first; outgoing work is polled from tables

Status: implemented

## Problem

WhatsApp is the core of the product and must never lose a message. Meta expects a fast 200 on webhooks and redelivers, while LLM calls and Meta sends are slow and fail. The legacy published inbound messages as in-memory events nobody listened to, so every message was lost.

## Decision

`WhatsappWebhookController` passes the raw bytes to `WhatsappWebhookCommandService`, which checks the signature and stores the customer, conversation and message in one transaction, skipping a `wamid` that already exists, before answering 200. The webhook calls nothing outside the database. Work that follows is a row: an outgoing message is a `pending` row of `messages`, and confirmations, reminders and assistant replies use `scheduled_messages` and `assistant_turns` the same way. After commit an event only wakes the job (`OutboundMessageSender`, later `ScheduledMessageDispatcher` and `TurnWorker`); the job also polls and claims rows with `SELECT … FOR UPDATE SKIP LOCKED`, one row per transaction. A send is retried up to 5 times with backoff, then marked `failed` and shown in the inbox.

## Alternatives considered

**Process the assistant inside the webhook.** Simple, but LLM latency delays the 200, and an LLM failure could lose the stored message.

**Asynchronous CDI events or a broker.** Work in flight disappears on a crash, or a broker joins the infrastructure; tables give durability with the database already there.

## Consequences

- Replaying the same webhook payload, even six times at once, stores one message (`WhatsappWebhookControllerTest`).
- A staff reply is a committed `pending` row before Meta is called, so a crash between commit and send leaves it for the next poll; parallel senders send each message once (`ConversationMessagesControllerTest`).
- A webhook answers without waiting for Meta or the LLM; a rare duplicate send, when the process dies after Meta accepted but before commit, is accepted.
