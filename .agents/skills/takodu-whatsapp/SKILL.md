---
name: takodu-whatsapp
description: How the pibble conversations context integrates the Meta WhatsApp Cloud API — Embedded Signup line connection in coexistence mode, the /webhooks/whatsapp endpoint (signature on raw bytes, store-first, wamid idempotency, echoes, history, statuses), sending through WhatsappGateway, the 24-hour customer service window, templates, encrypted business tokens, media download, and replaying recorded Meta payloads in tests. Use for any code that receives or sends WhatsApp messages, touches WhatsappLine, Conversation, Message or ScheduledMessage, or parses or builds a Meta payload.
---

# WhatsApp in pibble

WhatsApp is the core of the product: **a customer message is never lost and never stored twice.** All Meta code lives in `conversations`. The decisions are [official Cloud API with coexistence](../../../docs/decisions/implemented/2026-10-19-official-whatsapp-cloud-api-with-coexistence.md), [webhooks store first](../../../docs/decisions/implemented/2026-10-19-webhooks-store-first-and-polled-outbox.md) and [message as its own aggregate](../../../docs/decisions/implemented/2026-10-19-message-as-its-own-aggregate.md). The product design is `../docs/07-whatsapp-integration.md`; rules W1–W15 are in `../docs/03-business-domain.md` §4.5.

Meta changes field names, limits and error codes often. **Check every Meta detail against Meta's current docs when you implement it**, pin the Graph API version (`takodu.meta.graph-version`), and keep each detail in the class that owns it.

## Where Meta code lives

| Class | Package (under `conversations`) | Owns |
|---|---|---|
| `WhatsappWebhookController` | `interfaces.rest` | `GET` verification handshake, `POST` events; returns 200 fast |
| `MetaSignatureVerifier` | `infrastructure.meta` | HMAC-SHA256 of the raw body with the app secret, constant-time compare |
| `MetaWebhookParser` | `infrastructure.meta` | The only class that knows Meta's JSON; turns it into domain-shaped records (`InboundEvent`, `StatusEvent`, `EchoEvent`, …) |
| `WhatsappWebhookReader` (port) | `application.internal.outboundservices.whatsapp` | Signature check, verify token, and parsing into `WhatsappWebhookCommand`s |
| `WhatsappGateway`, `WhatsappMediaGateway` (ports) | `application.internal.outboundservices.whatsapp` | `sendText`, `sendButtons`, `sendList`, `sendTemplate`; `download` |
| `WhatsappOnboarding` (port) | `application.internal.outboundservices.whatsapp` | Embedded Signup steps, creating the templates, unsubscribing |
| `MetaGraphClient` | `infrastructure.meta` | Quarkus REST Client for the Graph API calls |
| `MetaWhatsappGateway`, `MetaWhatsappOnboarding` | `infrastructure.meta` | Implement the ports with `MetaGraphClient`; active when `takodu.meta.enabled` (dev and test use fakes) |
| `TokenCipher` | `shared.infrastructure.crypto` | AES-256-GCM for business tokens; key from `TOKEN_ENCRYPTION_KEY` |

Meta's JSON never leaves `infrastructure.meta`: the controller passes the raw body to `WhatsappWebhookCommandService`, which checks it and reads it through `WhatsappWebhookReader` (implemented by `MetaWebhookReader` with the parser) into domain commands.

## Receiving: the webhook

1. `POST /webhooks/whatsapp` reads the body as `byte[]` and verifies `X-Hub-Signature-256` **before parsing**. Invalid → 401, nothing stored, nothing logged except the fact.
2. In **one transaction**, for each entry: find the `WhatsappLine` by `phone_number_id` (unknown or disconnected → acknowledge and ignore); find or create the customer through `ExternalCustomersService` (C2); find or start the `Conversation` and lock it (W3); store the `Message` unless its `wamid` exists (W4); update the window and unread count; fire `InboundMessageReceivedIntegrationEvent`.
3. Return 200. **No call to Meta, the LLM or another HTTP service happens in the webhook.**

| Webhook content | Stored as | Side effects |
|---|---|---|
| Customer message | `Message.inbound`, origin `customer` | Window and unread; event to `assistant` |
| Echo from the WhatsApp Business app | `Message.businessAppEcho`, origin `business_app` | `recordHumanOutbound`: pauses the assistant 12 h (A3); no event |
| History sync | origin `history` | No unread, no event, no assistant |
| Status (`sent`, `delivered`, `read`, `failed`) | Updates the existing message | `DeliveryStatus.advanceTo` only moves forward (W10); keeps pricing and error |
| Account or partner removed | `WhatsappLine.disconnect()` | Line shown as disconnected |
| Template status | `WhatsappLine.recordTemplateStatus` on the WABA's connected lines | Notifications wait until approved |

A tapped button or list row is an inbound `interactive` (or template `button`) message with the title in `body`. Unknown kinds are stored as `unsupported`, never dropped. Media keeps `media_id`; `MediaDownloader` later stores the file through `MediaStorage` at `<organization>/<message>` under `MEDIA_DIR`, within Meta's size limits, with `media_status` and at most 5 attempts.

## Sending

Every outbound message follows the same path, whoever sends it:

1. The command service checks the plan (O6), the line is connected (W15) and, for anything but a template, `conversation.isWindowOpen(now)` (W6). A closed window is `WindowClosedException` (422) with a message that tells staff to use a template.
2. It returns the existing message when the request's `Idempotency-Key` was already used, otherwise saves the `Message` as `pending` with that key in the caller's transaction (staff replies also call `recordHumanOutbound`, which pauses the assistant).
3. `OutboundMessageSender` re-checks those rules, sends it through `WhatsappGateway` and records `sent` with the `wamid`. A failure waits 30 s, doubling each time, and the fifth one, or one Meta would repeat (`WhatsappSendException.retryable()` false), stores `failed` with Meta's error ([takodu-jobs](../takodu-jobs/SKILL.md)).

Limits checked when queued: text ≤ 4,096 characters, interactive body ≤ 1,024; `InteractiveChoices` allows ≤ 3 distinct button titles of ≤ 20, or ≤ 10 list rows of ≤ 24. The row stores the choices (`interactive_style`, `interactive_options`); ids are positions, and a tap is read by its title.

## Appointment notifications

`NotificationTemplate` holds the three Spanish utility templates, each about one appointment, never promotional. Booking's events insert `ScheduledMessage` rows; `ScheduledMessageDispatcher` queues each due row as `Message.notification`: the template outside the window and for reminders, the same text inside it. Rows wait while plan, line or template forbids sending.

## Connecting a line

`POST /api/v1/establishments/{id}/whatsapp-line` with the Embedded Signup `code`, `wabaId`, `phoneNumberId` and `mode` (`coexistence` by default). Owner only (`Action.CONNECT_WHATSAPP`). The service exchanges the code, subscribes webhooks, reads the number, requests history sync (coexistence), creates the templates, and stores the line with the token encrypted. A `phone_number_id` already on another line is a conflict (W1). The owner waits, so these Meta calls run in the request, before the storing transaction ([takodu-jobs](../takodu-jobs/SKILL.md)).

## Security and privacy

- The business token is stored only encrypted, decrypted only in the Meta adapters, and never returned by an endpoint.
- **Never log message bodies, phone numbers or tokens.** Log `wamid`, line id and status.
- Webhook payloads are untrusted: validate them and ignore what matches no line.
- Media is served only through an authenticated endpoint that checks the caller can see the establishment.

## Tests

- **Recorded payloads:** keep anonymized Meta webhook JSON in `src/test/resources/meta/` and replay it against `WebhookController` with a signature computed in the test. Assert rows, not logs.
- Replaying the same payload twice stores one message; a bad signature stores nothing and returns 401.
- `Conversation.isWindowOpen`, `DeliveryStatus.advanceTo` and `ScheduledMessage.reminderFor` get plain JUnit tests at their boundaries (exactly 24 h, exactly 3 h).
- Tests never call Meta: `RecordingWhatsappGateway` records sends and fails on demand.

## Checklist

- [ ] The signature is verified on raw bytes before any parsing.
- [ ] The webhook stores in one transaction and calls nothing external.
- [ ] Every new message path is idempotent by `wamid`.
- [ ] Meta JSON is known only to `infrastructure.meta`.
- [ ] No body, phone or token is logged.
