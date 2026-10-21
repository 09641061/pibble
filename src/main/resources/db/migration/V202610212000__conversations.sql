-- conversations: WhatsApp lines, the shared inbox and every message (W1-W10).
-- organization_id repeats the establishment's, so every query is scoped by organization without a join.

-- W1: an establishment has one connected line and a number belongs to one line. A disconnected line keeps its
-- history; connecting the same number again reuses the row.
CREATE TABLE whatsapp_lines (
    id                     uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id        uuid NOT NULL REFERENCES organizations (id) ON DELETE CASCADE,
    establishment_id       uuid NOT NULL REFERENCES establishments (id) ON DELETE CASCADE,
    waba_id                text NOT NULL,
    phone_number_id        text NOT NULL,
    display_phone          text NOT NULL,
    access_token_encrypted text NOT NULL,
    mode                   text NOT NULL,
    status                 text NOT NULL DEFAULT 'connected',
    connected_at           timestamptz NOT NULL,
    last_idempotency_key   uuid,
    created_at             timestamptz NOT NULL DEFAULT now(),
    updated_at             timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT whatsapp_lines_mode_ck CHECK (mode IN ('coexistence', 'cloud_api')),
    CONSTRAINT whatsapp_lines_status_ck CHECK (status IN ('connected', 'disconnected'))
);
CREATE UNIQUE INDEX whatsapp_lines_phone_number_uk ON whatsapp_lines (phone_number_id);
CREATE UNIQUE INDEX whatsapp_lines_establishment_uk ON whatsapp_lines (establishment_id) WHERE status = 'connected';
CREATE INDEX whatsapp_lines_waba_idx ON whatsapp_lines (waba_id);

-- W3: one conversation per line and customer. Writers lock the row (SELECT ... FOR UPDATE), so a webhook and a
-- staff reply never lose each other's unread count or window.
CREATE TABLE conversations (
    id                   uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id      uuid NOT NULL REFERENCES organizations (id) ON DELETE CASCADE,
    establishment_id     uuid NOT NULL REFERENCES establishments (id) ON DELETE CASCADE,
    line_id              uuid NOT NULL REFERENCES whatsapp_lines (id) ON DELETE CASCADE,
    customer_id          uuid NOT NULL REFERENCES customers (id),
    assigned_member_id   uuid REFERENCES members (id),
    bot_paused_until     timestamptz,
    last_inbound_at      timestamptz,
    last_message_at      timestamptz NOT NULL,
    unread_count         integer NOT NULL DEFAULT 0,
    created_at           timestamptz NOT NULL DEFAULT now(),
    updated_at           timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT conversations_unread_ck CHECK (unread_count >= 0)
);
CREATE UNIQUE INDEX conversations_line_customer_uk ON conversations (line_id, customer_id);
CREATE INDEX conversations_inbox_idx ON conversations (establishment_id, last_message_at DESC, id DESC);

-- W4: a wamid is stored once. Outgoing messages are the outbox: pending rows are claimed with
-- FOR UPDATE SKIP LOCKED, retried with backoff and failed after 5 attempts.
CREATE TABLE messages (
    id                uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id   uuid NOT NULL REFERENCES organizations (id) ON DELETE CASCADE,
    conversation_id   uuid NOT NULL REFERENCES conversations (id) ON DELETE CASCADE,
    direction         text NOT NULL,
    origin            text NOT NULL,
    wamid             text,
    kind              text NOT NULL,
    body              text,
    media_id          text,
    media_mime        text,
    media_path        text,
    template_name     text,
    status            text NOT NULL,
    error             text,
    pricing_category  text,
    billable          boolean,
    sent_by_member_id uuid REFERENCES members (id),
    attempts          smallint NOT NULL DEFAULT 0,
    next_attempt_at   timestamptz,
    idempotency_key   uuid,
    occurred_at       timestamptz NOT NULL,
    created_at        timestamptz NOT NULL DEFAULT now(),
    updated_at        timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT messages_direction_ck CHECK (direction IN ('in', 'out')),
    CONSTRAINT messages_origin_ck
        CHECK (origin IN ('customer', 'assistant', 'staff', 'business_app', 'system', 'history')),
    CONSTRAINT messages_kind_ck CHECK (kind IN ('text', 'image', 'audio', 'video', 'document', 'sticker', 'location',
                                                'interactive', 'button', 'reaction', 'template', 'unsupported')),
    CONSTRAINT messages_status_ck CHECK (status IN ('received', 'pending', 'sent', 'delivered', 'read', 'failed')),
    CONSTRAINT messages_customer_in_ck CHECK (origin <> 'customer' OR direction = 'in'),
    CONSTRAINT messages_out_origin_ck CHECK (direction = 'out' OR origin IN ('customer', 'history')),
    CONSTRAINT messages_staff_sender_ck CHECK (origin <> 'staff' OR sent_by_member_id IS NOT NULL),
    CONSTRAINT messages_template_name_ck CHECK (kind <> 'template' OR template_name IS NOT NULL),
    CONSTRAINT messages_attempts_ck CHECK (attempts BETWEEN 0 AND 5)
);
CREATE UNIQUE INDEX messages_wamid_uk ON messages (wamid);
CREATE INDEX messages_thread_idx ON messages (conversation_id, occurred_at DESC, id DESC);
CREATE INDEX messages_outbox_idx ON messages (next_attempt_at) WHERE status = 'pending';
CREATE UNIQUE INDEX messages_idempotency_uk ON messages (organization_id, idempotency_key);
