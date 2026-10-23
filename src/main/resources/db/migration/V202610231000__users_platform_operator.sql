-- The Takodu team: users allowed on the internal API (/internal/v1). Only the database grants it, by hand:
-- UPDATE users SET platform_operator = true WHERE email = '...'.
ALTER TABLE users ADD COLUMN platform_operator boolean NOT NULL DEFAULT false;
