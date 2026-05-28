CREATE TABLE device_tokens (
    id          UUID          NOT NULL PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID          NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    token       VARCHAR(512)  NOT NULL,
    platform    VARCHAR(10)   NOT NULL,
    created_at  TIMESTAMP     NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, token)
);
