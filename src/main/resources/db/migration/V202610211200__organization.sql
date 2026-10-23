-- organization: the business, its establishments, its team and its catalog (O1-O15).
-- Names are any non-blank text (O2). Emails are stored normalized to lower case by the Email value object.

CREATE TABLE organizations (
    id                   uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    name                 text NOT NULL,
    plan                 text NOT NULL DEFAULT 'standard',
    plan_active_until    timestamptz,
    idempotency_key      uuid,
    last_idempotency_key uuid,
    created_at           timestamptz NOT NULL DEFAULT now(),
    updated_at           timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT organizations_name_ck CHECK (btrim(name) <> ''),
    CONSTRAINT organizations_plan_ck CHECK (plan IN ('standard', 'max'))
);
CREATE UNIQUE INDEX organizations_idempotency_uk ON organizations (idempotency_key);

CREATE TABLE establishments (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id uuid NOT NULL REFERENCES organizations (id) ON DELETE CASCADE,
    name            text NOT NULL,
    address         text,
    time_zone       text NOT NULL DEFAULT 'America/Lima',
    archived_at     timestamptz,
    idempotency_key uuid,
    created_at      timestamptz NOT NULL DEFAULT now(),
    updated_at      timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT establishments_name_ck CHECK (btrim(name) <> '')
);
CREATE INDEX establishments_organization_idx ON establishments (organization_id);
CREATE UNIQUE INDEX establishments_idempotency_uk ON establishments (organization_id, idempotency_key);

-- O8: several rows per weekday allow split shifts (09:00-13:00, 15:00-20:00). No row = closed. No range crosses
-- midnight.
CREATE TABLE opening_hours (
    establishment_id uuid NOT NULL REFERENCES establishments (id) ON DELETE CASCADE,
    weekday          smallint NOT NULL,
    opens_at         time NOT NULL,
    closes_at        time NOT NULL,
    PRIMARY KEY (establishment_id, weekday, opens_at),
    CONSTRAINT opening_hours_weekday_ck CHECK (weekday BETWEEN 1 AND 7),
    CONSTRAINT opening_hours_range_ck CHECK (closes_at > opens_at)
);

-- O9-O12: a pending invitation is a member without user_id. The token hash stays after acceptance, so a retried
-- acceptance finds the same membership.
CREATE TABLE members (
    id                   uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id      uuid NOT NULL REFERENCES organizations (id) ON DELETE CASCADE,
    user_id              uuid REFERENCES users (id),
    email                text NOT NULL,
    display_name         text NOT NULL,
    role                 text NOT NULL,
    bookable             boolean NOT NULL DEFAULT true,
    invite_token_hash    text,
    invite_expires_at    timestamptz,
    joined_at            timestamptz,
    removed_at           timestamptz,
    idempotency_key      uuid,
    last_idempotency_key uuid,
    created_at           timestamptz NOT NULL DEFAULT now(),
    updated_at           timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT members_email_lower_ck CHECK (email = lower(email)),
    CONSTRAINT members_display_name_ck CHECK (btrim(display_name) <> ''),
    CONSTRAINT members_role_ck CHECK (role IN ('owner', 'manager', 'staff')),
    CONSTRAINT members_user_or_invitation_ck CHECK (user_id IS NOT NULL OR invite_token_hash IS NOT NULL)
);
CREATE UNIQUE INDEX members_invite_token_uk ON members (invite_token_hash);
CREATE UNIQUE INDEX members_active_user_uk ON members (organization_id, user_id)
    WHERE user_id IS NOT NULL AND removed_at IS NULL;
CREATE UNIQUE INDEX members_active_email_uk ON members (organization_id, email) WHERE removed_at IS NULL;
CREATE UNIQUE INDEX members_one_owner_uk ON members (organization_id) WHERE role = 'owner' AND removed_at IS NULL;
CREATE UNIQUE INDEX members_idempotency_uk ON members (organization_id, idempotency_key);
CREATE INDEX members_user_idx ON members (user_id);

-- O13: where a staff member works, which decides the calendars and inboxes they see. Owners and managers see every
-- establishment and have no rows here.
CREATE TABLE member_establishments (
    member_id        uuid NOT NULL REFERENCES members (id) ON DELETE CASCADE,
    establishment_id uuid NOT NULL REFERENCES establishments (id) ON DELETE CASCADE,
    PRIMARY KEY (member_id, establishment_id)
);
CREATE INDEX member_establishments_establishment_idx ON member_establishments (establishment_id);

-- O14: organization_id repeats the establishment's, so every query is scoped by organization without a join.
CREATE TABLE services (
    id               uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id  uuid NOT NULL REFERENCES organizations (id) ON DELETE CASCADE,
    establishment_id uuid NOT NULL REFERENCES establishments (id) ON DELETE CASCADE,
    name             text NOT NULL,
    duration_minutes integer NOT NULL,
    price_cents      integer NOT NULL,
    currency         char(3) NOT NULL DEFAULT 'PEN',
    instructions     text,
    active           boolean NOT NULL DEFAULT true,
    idempotency_key  uuid,
    created_at       timestamptz NOT NULL DEFAULT now(),
    updated_at       timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT services_name_ck CHECK (btrim(name) <> ''),
    CONSTRAINT services_duration_ck CHECK (duration_minutes > 0),
    CONSTRAINT services_price_ck CHECK (price_cents >= 0)
);
CREATE INDEX services_establishment_idx ON services (establishment_id);
CREATE UNIQUE INDEX services_idempotency_uk ON services (organization_id, idempotency_key);

-- O15: the members who perform a service.
CREATE TABLE service_staff (
    service_id uuid NOT NULL REFERENCES services (id) ON DELETE CASCADE,
    member_id  uuid NOT NULL REFERENCES members (id) ON DELETE CASCADE,
    PRIMARY KEY (service_id, member_id)
);
CREATE INDEX service_staff_member_idx ON service_staff (member_id);
