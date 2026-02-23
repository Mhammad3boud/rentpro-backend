CREATE TABLE IF NOT EXISTS maintenance_photos (
  photo_id UUID PRIMARY KEY,
  request_id UUID NOT NULL,
  image_url VARCHAR(500) NOT NULL,

  CONSTRAINT fk_maintenance_photos_request
    FOREIGN KEY (request_id)
    REFERENCES maintenance_requests(request_id)
    ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_maintenance_photos_request_id
  ON maintenance_photos(request_id);
