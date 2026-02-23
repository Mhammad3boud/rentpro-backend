-- V5: Add profile fields to users

ALTER TABLE users
  ADD COLUMN IF NOT EXISTS full_name VARCHAR(150),
  ADD COLUMN IF NOT EXISTS phone VARCHAR(30);

-- Optional: if you want every user to have a name, set a default for existing rows
UPDATE users
SET full_name = COALESCE(full_name, split_part(email, '@', 1))
WHERE full_name IS NULL;

-- Optional: enforce uniqueness for phone (only if you want it)
-- CREATE UNIQUE INDEX IF NOT EXISTS ux_users_phone ON users(phone) WHERE phone IS NOT NULL;
