CREATE TABLE pomodoro_sessions (
    id                UUID      NOT NULL,
    user_id           UUID      NOT NULL,
    duration_minutes  INTEGER   NOT NULL,
    completed_minutes INTEGER   NOT NULL DEFAULT 0,
    completed         BOOLEAN   NOT NULL DEFAULT FALSE,
    started_at        TIMESTAMPTZ NOT NULL,
    finished_at       TIMESTAMPTZ,
    created_at        TIMESTAMPTZ NOT NULL,
    updated_at        TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_pomodoro_sessions PRIMARY KEY (id),
    CONSTRAINT fk_pomodoro_sessions_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_pomodoro_sessions_user_id ON pomodoro_sessions (user_id);
