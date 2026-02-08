CREATE TABLE maintenance_photos (
  id BIGSERIAL PRIMARY KEY,
  maintenance_request_id BIGINT NOT NULL,
  file_name VARCHAR(255) NOT NULL,
  content_type VARCHAR(100),
  uploaded_at TIMESTAMP NOT NULL DEFAULT NOW(),

  CONSTRAINT fk_mp_request
    FOREIGN KEY (maintenance_request_id)
    REFERENCES maintenance_requests(id)
    ON DELETE CASCADE
);

CREATE INDEX idx_mp_request_id ON maintenance_photos(maintenance_request_id);
