ALTER TABLE properties
  ALTER COLUMN latitude TYPE double precision
  USING latitude::double precision;

ALTER TABLE properties
  ALTER COLUMN longitude TYPE double precision
  USING longitude::double precision;
