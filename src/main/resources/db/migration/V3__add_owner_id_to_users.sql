ALTER TABLE users
ADD COLUMN owner_id BIGINT;

ALTER TABLE users
ADD CONSTRAINT fk_users_owner
FOREIGN KEY (owner_id) REFERENCES users(id)
ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_users_owner_id ON users(owner_id);
