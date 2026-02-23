-- Add profile fields to users table
ALTER TABLE users ADD COLUMN IF NOT EXISTS profile_picture VARCHAR(500);
ALTER TABLE users ADD COLUMN IF NOT EXISTS address VARCHAR(500);
ALTER TABLE users ADD COLUMN IF NOT EXISTS notification_email BOOLEAN DEFAULT true;
ALTER TABLE users ADD COLUMN IF NOT EXISTS notification_push BOOLEAN DEFAULT true;

-- Set default values for existing records
UPDATE users SET notification_email = true WHERE notification_email IS NULL;
UPDATE users SET notification_push = true WHERE notification_push IS NULL;
