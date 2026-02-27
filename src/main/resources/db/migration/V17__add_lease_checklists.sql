ALTER TABLE leases
    ADD COLUMN IF NOT EXISTS check_in_checklist_json TEXT,
    ADD COLUMN IF NOT EXISTS check_out_checklist_json TEXT;
