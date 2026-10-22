-- W11-W15: appointment notifications waiting for their time. conversations learns about appointments only through
-- booking's events, so each row keeps what it needs to be written. When due, a row becomes a message in the outbox,
-- which retries and fails it; message_id links the two.
CREATE TABLE scheduled_messages (
    id               uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id  uuid NOT NULL REFERENCES organizations (id) ON DELETE CASCADE,
    establishment_id uuid NOT NULL REFERENCES establishments (id) ON DELETE CASCADE,
    appointment_id   uuid NOT NULL REFERENCES appointments (id) ON DELETE CASCADE,
    customer_id      uuid NOT NULL REFERENCES customers (id),
    service_id       uuid NOT NULL REFERENCES services (id),
    kind             text NOT NULL,
    starts_at        timestamptz NOT NULL,
    send_at          timestamptz NOT NULL,
    status           text NOT NULL DEFAULT 'pending',
    reason           text,
    cancel_reason    text,
    message_id       uuid REFERENCES messages (id),
    created_at       timestamptz NOT NULL DEFAULT now(),
    updated_at       timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT scheduled_messages_kind_ck CHECK (kind IN ('confirmation', 'reminder', 'cancellation')),
    CONSTRAINT scheduled_messages_status_ck CHECK (status IN ('pending', 'sent', 'cancelled', 'skipped')),
    CONSTRAINT scheduled_messages_cancel_reason_ck CHECK (kind <> 'cancellation' OR cancel_reason IS NOT NULL),
    CONSTRAINT scheduled_messages_sent_ck CHECK (status <> 'sent' OR message_id IS NOT NULL)
);
-- W14: one notification per appointment and kind.
CREATE UNIQUE INDEX scheduled_messages_appointment_kind_uk ON scheduled_messages (appointment_id, kind);
CREATE INDEX scheduled_messages_due_idx ON scheduled_messages (send_at) WHERE status = 'pending';
