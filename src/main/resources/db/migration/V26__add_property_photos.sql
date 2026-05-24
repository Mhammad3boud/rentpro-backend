CREATE TABLE property_photos (
    photo_id    UUID         NOT NULL PRIMARY KEY,
    property_id UUID         NOT NULL REFERENCES properties(property_id) ON DELETE CASCADE,
    image_url   VARCHAR(1024) NOT NULL,
    uploaded_at TIMESTAMP    NOT NULL DEFAULT NOW()
);
