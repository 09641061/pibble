# Decision: A customer is a phone number within an organization

Status: proposed

## Problem

Customers arrive on WhatsApp with a phone number and a profile name, never an ID document. The legacy required a document validated online per establishment, so the main channel could not create a customer, an outage of the lookup API blocked every registration, and one person visiting two branches became two customers.

## Proposal

`Customer` belongs to the organization and is identified by a `PhoneNumber` in E.164, unique among active customers of that organization (a partial unique index). The first inbound WhatsApp message creates the customer through `CustomersContextFacade.findOrCreateByWhatsapp`, idempotently. Name, identity document and notes are optional and validated by format only; nothing on the write path calls an external API. Deleting is soft, and the phone can then be registered again.

## Alternatives considered

**Identity document as the key, as in the legacy.** It blocks the WhatsApp flow and ties writes to an external service.

**Customers per establishment.** Matches the legacy tables, but splits one person's history across branches while plans and reports belong to the organization.

## Acceptance criteria

- Two concurrent inbound messages from a new number create one customer.
- Registering an active phone again in the same organization returns 409; in another organization it succeeds.
- Creating or editing a customer makes no outbound HTTP call.
