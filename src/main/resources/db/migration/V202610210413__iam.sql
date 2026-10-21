-- iam: who can sign in (I1-I6). Emails are stored normalized to lower case by the Email value object.

CREATE TABLE users (
    id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    email         text NOT NULL,
    name          text,
    disabled_at   timestamptz,
    last_login_at timestamptz,
    created_at    timestamptz NOT NULL DEFAULT now(),
    updated_at    timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT users_email_lower_ck CHECK (email = lower(email))
);
CREATE UNIQUE INDEX users_email_uk ON users (email);

-- I3: the rate limit counts this email's rows in the last hour.
CREATE TABLE login_codes (
    id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    email       text NOT NULL,
    code_hash   text NOT NULL,
    attempts    smallint NOT NULL DEFAULT 0,
    expires_at  timestamptz NOT NULL,
    consumed_at timestamptz,
    created_at  timestamptz NOT NULL DEFAULT now(),
    updated_at  timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT login_codes_attempts_ck CHECK (attempts BETWEEN 0 AND 5)
);
CREATE INDEX login_codes_email_idx ON login_codes (email, created_at DESC);

-- I5: one row per signed-in device. Only token hashes are stored. The previous hash is accepted again for a
-- short grace period after rotation, so a refresh whose response was lost can be retried.
CREATE TABLE sessions (
    id                          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                     uuid NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    refresh_token_hash          text NOT NULL,
    previous_refresh_token_hash text,
    rotated_at                  timestamptz,
    expires_at                  timestamptz NOT NULL,
    revoked_at                  timestamptz,
    last_used_at                timestamptz NOT NULL,
    created_at                  timestamptz NOT NULL DEFAULT now(),
    updated_at                  timestamptz NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX sessions_refresh_token_uk ON sessions (refresh_token_hash);
CREATE INDEX sessions_previous_refresh_token_idx ON sessions (previous_refresh_token_hash);
CREATE INDEX sessions_user_idx ON sessions (user_id);
