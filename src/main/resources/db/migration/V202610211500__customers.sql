-- customers: the people an organization serves, identified by phone (C1-C5).
-- Phones are stored in E.164 by the PhoneNumber value object; deleting is soft and frees the phone (C5).

CREATE TABLE customers (
    id                    uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id       uuid NOT NULL REFERENCES organizations (id) ON DELETE CASCADE,
    phone_e164            text NOT NULL,
    name                  text,
    whatsapp_profile_name text,
    document_type         text,
    document_number       text,
    notes                 text,
    idempotency_key       uuid,
    deleted_at            timestamptz,
    created_at            timestamptz NOT NULL DEFAULT now(),
    updated_at            timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT customers_phone_ck CHECK (phone_e164 ~ '^\+[1-9][0-9]{6,14}$'),
    CONSTRAINT customers_document_type_ck CHECK (document_type IN ('dni', 'ruc', 'ce', 'passport')),
    CONSTRAINT customers_document_ck CHECK ((document_type IS NULL) = (document_number IS NULL))
);
-- C1: one active customer per phone in an organization.
CREATE UNIQUE INDEX customers_phone_uk ON customers (organization_id, phone_e164) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX customers_idempotency_uk ON customers (organization_id, idempotency_key);
