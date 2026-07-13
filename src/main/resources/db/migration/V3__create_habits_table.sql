CREATE TABLE habits (
    id          UUID         NOT NULL,
    user_id     UUID         NOT NULL,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL,
    CONSTRAINT pk_habits PRIMARY KEY (id),
    CONSTRAINT fk_habits_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_habits_user_id ON habits (user_id);
