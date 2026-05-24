CREATE TABLE unit_photos (
    photo_id    UUID          NOT NULL PRIMARY KEY,
    unit_id     UUID          NOT NULL REFERENCES units(unit_id) ON DELETE CASCADE,
    image_url   VARCHAR(1024) NOT NULL,
    uploaded_at TIMESTAMP     NOT NULL DEFAULT NOW()
);
