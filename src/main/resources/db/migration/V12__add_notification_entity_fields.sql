-- Add optional entity metadata for deep-link notifications
ALTER TABLE notifications
ADD COLUMN IF NOT EXISTS entity_type VARCHAR(50);

ALTER TABLE notifications
ADD COLUMN IF NOT EXISTS entity_id UUID;

