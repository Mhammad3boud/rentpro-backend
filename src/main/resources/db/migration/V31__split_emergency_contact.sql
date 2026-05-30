ALTER TABLE tenants RENAME COLUMN emergency_contact TO emergency_name;
ALTER TABLE tenants ADD COLUMN emergency_phone VARCHAR(50);
