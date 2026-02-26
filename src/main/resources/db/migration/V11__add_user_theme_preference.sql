-- Add user-specific theme preference
ALTER TABLE users
ADD COLUMN IF NOT EXISTS theme_preference VARCHAR(10);

