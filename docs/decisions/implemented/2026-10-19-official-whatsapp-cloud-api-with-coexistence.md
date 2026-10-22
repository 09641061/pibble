# Decision: Official WhatsApp Cloud API, coexistence by default

Status: implemented

## Problem

Small Peruvian businesses run on the WhatsApp Business app on the owner's phone. Takodu must receive and send on that same number without risking a ban, without a reseller's markup and without carrying Meta's billing.

## Decision

Takodu is a direct Meta Tech Provider. The owner connects a number through Embedded Signup; the default mode is **coexistence**, so the phone app keeps working, and owner replies from the app arrive as echoes stored as `business_app` messages. Signup also creates Takodu's utility templates on the business's WABA, and the `message_template_status_update` webhook keeps their review status on the line. Each business pays Meta directly with its own card on its WABA. Business tokens are encrypted with AES-256-GCM (`TokenCipher`) with a key from the environment. Meta's webhook JSON is parsed only in `MetaWebhookParser`, and every call goes through `MetaGraphClient` behind the `WhatsappOnboarding` and `WhatsappGateway` ports in `conversations`. Message bodies are never logged. Unofficial WhatsApp Web libraries are never used.

## Alternatives considered

**API-only numbers.** Cleaner integration, but owners must give up the phone app, which blocks adoption.

**A BSP (360dialog, Twilio, Gupshup).** Faster setup, at a markup per number or message and with less control of onboarding and webhooks.

**Takodu pays Meta and re-bills.** Takodu would carry billing risk and Meta invoices for every business.

**Unofficial libraries (Baileys, Evolution API, whatsapp-web.js).** Against WhatsApp's terms; the business's number can be banned and the library breaks on WhatsApp updates.

## Consequences

Owners keep their phone app and their number's history, and Takodu never touches Meta's invoices. In exchange, Takodu follows Meta's rules: templates need Meta's approval before reminders go out, the 24-hour window limits free-form replies, and every Meta detail (fields, limits, error codes) must be checked against Meta's current docs and pinned to `takodu.meta.graph-version`. Tests never call Meta: recorded payloads in `src/test/resources/meta/` replay the webhooks, and recording fakes replace both ports.
