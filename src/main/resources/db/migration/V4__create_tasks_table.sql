CREATE TABLE tasks (
    id          UUID         NOT NULL,
    user_id     UUID         NOT NULL,
    title       VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    completed   BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL,
    CONSTRAINT pk_tasks PRIMARY KEY (id),
    CONSTRAINT fk_tasks_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_tasks_user_id ON tasks (user_id);
