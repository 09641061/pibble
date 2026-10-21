# Decision: Official WhatsApp Cloud API, coexistence by default

Status: proposed

## Problem

Small Peruvian businesses run on the WhatsApp Business app on the owner's phone. Takodu must receive and send on that same number without risking a ban, without a reseller's markup and without carrying Meta's billing.

## Proposal

Takodu is a direct Meta Tech Provider. The owner connects a number through Embedded Signup; the default mode is **coexistence**, so the phone app keeps working, and owner replies from the app arrive as echoes stored as `business_app` messages. Each business pays Meta directly with its own card on its WABA. Business tokens are encrypted with AES-256-GCM with a key from the environment. All Meta payloads are parsed in one class (`MetaWebhookParser`) and all calls go through one client in `conversations.infrastructure.meta` behind the `WhatsappGateway` port. Message bodies are never logged. Unofficial WhatsApp Web libraries are never used.

## Alternatives considered

**API-only numbers.** Cleaner integration, but owners must give up the phone app, which blocks adoption.

**A BSP (360dialog, Twilio, Gupshup).** Faster setup, at a markup per number or message and with less control of onboarding and webhooks.

**Takodu pays Meta and re-bills.** Takodu would carry billing risk and Meta invoices for every business.

**Unofficial libraries (Baileys, Evolution API, whatsapp-web.js).** Against WhatsApp's terms; the business's number can be banned and the library breaks on WhatsApp updates.

## Acceptance criteria

- No dependency on an unofficial WhatsApp library appears in `pom.xml`.
- Recorded Meta payloads (message, echo, status, duplicate) replayed against `WebhookController` produce the expected rows.
- The stored business token is not readable without the encryption key.
