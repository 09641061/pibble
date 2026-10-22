-- A1: whether the assistant answers an establishment's customers, and the owner's free-text business info.
-- An establishment without a row has the assistant off.
CREATE TABLE assistant_settings (
    id               uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id  uuid NOT NULL REFERENCES organizations (id) ON DELETE CASCADE,
    establishment_id uuid NOT NULL REFERENCES establishments (id) ON DELETE CASCADE,
    enabled          boolean NOT NULL DEFAULT false,
    business_info    text NOT NULL DEFAULT '',
    created_at       timestamptz NOT NULL DEFAULT now(),
    updated_at       timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT assistant_settings_business_info_ck CHECK (char_length(business_info) <= 4000)
);
CREATE UNIQUE INDEX assistant_settings_establishment_uk ON assistant_settings (establishment_id);
