-- Authentication lifecycle hardening. Existing sessions start at version 0 and
-- existing refresh families inherit their current token expiry as their immutable
-- family deadline.
ALTER TABLE users
    ADD COLUMN auth_version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE refresh_tokens
    ADD COLUMN auth_version BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN family_deadline DATETIME(6) NULL;

UPDATE refresh_tokens
SET family_deadline = expires_at
WHERE family_deadline IS NULL;

ALTER TABLE refresh_tokens
    MODIFY COLUMN family_deadline DATETIME(6) NOT NULL;

CREATE INDEX idx_users_role_id ON users (role, id);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens (expires_at, id);
CREATE INDEX idx_refresh_tokens_family_deadline ON refresh_tokens (family_deadline, id);
CREATE INDEX idx_refresh_tokens_user_revoked ON refresh_tokens (user_id, revoked_at, id);
