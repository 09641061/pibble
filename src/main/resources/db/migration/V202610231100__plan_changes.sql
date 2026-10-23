-- O5: every plan change by the Takodu team, so a payment dispute can be answered with who, when, from, to and why.
-- Rows are never updated or deleted; organizations are never deleted, so the foreign key does not cascade.
CREATE TABLE plan_changes (
    id                uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id   uuid NOT NULL REFERENCES organizations (id),
    changed_by        uuid NOT NULL REFERENCES users (id),
    from_plan         text NOT NULL,
    to_plan           text NOT NULL,
    from_active_until timestamptz,
    to_active_until   timestamptz NOT NULL,
    note              text NOT NULL,
    idempotency_key   uuid NOT NULL,
    changed_at        timestamptz NOT NULL,
    created_at        timestamptz NOT NULL DEFAULT now(),
    updated_at        timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT plan_changes_plans_ck CHECK (from_plan IN ('standard', 'max') AND to_plan IN ('standard', 'max')),
    CONSTRAINT plan_changes_note_ck CHECK (btrim(note) <> '')
);
CREATE INDEX plan_changes_organization_idx ON plan_changes (organization_id, changed_at DESC);
CREATE UNIQUE INDEX plan_changes_idempotency_uk ON plan_changes (organization_id, idempotency_key);

CREATE FUNCTION plan_changes_append_only() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'plan_changes is append-only';
END;
$$;
CREATE TRIGGER plan_changes_append_only BEFORE UPDATE OR DELETE ON plan_changes
    FOR EACH ROW EXECUTE FUNCTION plan_changes_append_only();
