# Decision: A customer is a phone number within an organization

Status: implemented

## Problem

Customers arrive on WhatsApp with a phone number and a profile name, never an ID document. The legacy required a document validated online per establishment, so the main channel could not create a customer, an outage of the lookup API blocked every registration, and one person visiting two branches became two customers.

## Decision

`customers.domain.model.aggregates.Customer` belongs to the organization and is identified by a `PhoneNumber` in E.164, unique among its active customers through the partial index `customers_phone_uk`, which the adapter maps to 409 `CUSTOMER_CONFLICT`. Staff type numbers loosely: separators are ignored, `00` means `+`, and nine digits starting with 9 get `+51`; anything else needs its country code. The phone never changes after registration.

The first inbound WhatsApp message creates the customer through `CustomersContextFacade.findOrCreateByWhatsapp`, which needs no `TenantContext` and is safe to call twice or concurrently: the insert is `ON CONFLICT … DO NOTHING` on the same index, and the call then reads the active customer that won. The typed name and the WhatsApp profile name are separate columns; each message refreshes the profile name, and the display name is the typed name, else the profile name, else the phone. Name, identity document (type and number together) and notes are optional and checked by format only; nothing on the write path calls an external API. Deleting is soft, the phone can then be registered again, and `fetchCustomer` still returns the deleted customer so history keeps its name. Duplicates are never merged.

## Alternatives considered

**Identity document as the key, as in the legacy.** It blocks the WhatsApp flow and ties writes to an external service.

**Customers per establishment.** Matches the legacy tables, but splits one person's history across branches while plans and reports belong to the organization.

**Catch the unique violation and retry in a new transaction.** It works, but PostgreSQL aborts the caller's transaction on the violation, so the webhook's own work would need a second transaction around every lookup.

## Consequences

A WhatsApp sender becomes a customer with no form, and staff search by name or any digits of the phone. Every member, whatever their establishments, sees every customer of the organization. A number typed wrong by staff cannot be corrected: the customer is deleted and registered again, and the history stays with the deleted one. Two numbers of one person stay two customers.
