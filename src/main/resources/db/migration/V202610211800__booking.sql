-- booking: the calendar of each establishment (B1-B16).
-- organization_id repeats the establishment's, so every query is scoped by organization without a join.

CREATE EXTENSION IF NOT EXISTS btree_gist;

-- B5, B6: only confirmed and in_progress appointments occupy time, so a cancelled or no-show appointment frees its
-- slot at once. The exclusion constraints are the guarantee when two bookings race; the domain checks first.
CREATE TABLE appointments (
    id                   uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id      uuid NOT NULL REFERENCES organizations (id) ON DELETE CASCADE,
    establishment_id     uuid NOT NULL REFERENCES establishments (id),
    customer_id          uuid NOT NULL REFERENCES customers (id),
    service_id           uuid NOT NULL REFERENCES services (id),
    staff_member_id      uuid NOT NULL REFERENCES members (id),
    starts_at            timestamptz NOT NULL,
    ends_at              timestamptz NOT NULL,
    price_cents          integer NOT NULL,
    currency             char(3) NOT NULL,
    status               text NOT NULL DEFAULT 'confirmed',
    source               text NOT NULL,
    cancel_reason        text,
    cancelled_by         text,
    created_by           uuid REFERENCES users (id),
    idempotency_key      uuid,
    last_idempotency_key uuid,
    version              integer NOT NULL DEFAULT 0,
    created_at           timestamptz NOT NULL DEFAULT now(),
    updated_at           timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT appointments_range_ck CHECK (ends_at > starts_at),
    CONSTRAINT appointments_price_ck CHECK (price_cents >= 0),
    CONSTRAINT appointments_status_ck
        CHECK (status IN ('confirmed', 'in_progress', 'completed', 'cancelled', 'no_show')),
    CONSTRAINT appointments_source_ck CHECK (source IN ('staff', 'assistant')),
    CONSTRAINT appointments_cancel_reason_ck CHECK (char_length(cancel_reason) <= 500),
    CONSTRAINT appointments_cancelled_by_ck CHECK (cancelled_by IN ('staff', 'customer')),
    CONSTRAINT appointments_cancellation_ck
        CHECK (status <> 'cancelled' OR (cancel_reason IS NOT NULL AND cancelled_by IS NOT NULL)),
    CONSTRAINT appointments_created_by_ck CHECK (source <> 'staff' OR created_by IS NOT NULL),
    CONSTRAINT appointments_staff_no_overlap EXCLUDE USING gist (
        staff_member_id WITH =, tstzrange(starts_at, ends_at) WITH &&
    ) WHERE (status IN ('confirmed', 'in_progress')),
    CONSTRAINT appointments_customer_no_overlap EXCLUDE USING gist (
        customer_id WITH =, tstzrange(starts_at, ends_at) WITH &&
    ) WHERE (status IN ('confirmed', 'in_progress'))
);
CREATE INDEX appointments_calendar_idx ON appointments (establishment_id, starts_at);
CREATE INDEX appointments_customer_idx ON appointments (customer_id, starts_at DESC);
CREATE INDEX appointments_staff_idx ON appointments (staff_member_id, starts_at);
CREATE UNIQUE INDEX appointments_idempotency_uk ON appointments (organization_id, idempotency_key);

-- B4, B15: staff_member_id NULL blocks the whole establishment.
CREATE TABLE time_blocks (
    id               uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id  uuid NOT NULL REFERENCES organizations (id) ON DELETE CASCADE,
    establishment_id uuid NOT NULL REFERENCES establishments (id) ON DELETE CASCADE,
    staff_member_id  uuid REFERENCES members (id) ON DELETE CASCADE,
    starts_at        timestamptz NOT NULL,
    ends_at          timestamptz NOT NULL,
    reason           text,
    idempotency_key  uuid,
    created_at       timestamptz NOT NULL DEFAULT now(),
    updated_at       timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT time_blocks_range_ck CHECK (ends_at > starts_at),
    CONSTRAINT time_blocks_reason_ck CHECK (char_length(reason) <= 500)
);
CREATE INDEX time_blocks_range_idx ON time_blocks (establishment_id, starts_at);
CREATE UNIQUE INDEX time_blocks_idempotency_uk ON time_blocks (organization_id, idempotency_key);
