ALTER TABLE tasks
    ADD COLUMN completed_at TIMESTAMPTZ;

UPDATE tasks
SET completed_at = updated_at
WHERE completed = TRUE;

-- Supports statistics filters: user_id + created_at range
CREATE INDEX idx_tasks_user_id_created_at ON tasks (user_id, created_at);

-- Supports task completion metrics: user_id + completed_at range
CREATE INDEX idx_tasks_user_id_completed_at ON tasks (user_id, completed_at)
    WHERE completed_at IS NOT NULL;

CREATE INDEX idx_habits_user_id_created_at ON habits (user_id, created_at);

CREATE INDEX idx_pomodoro_sessions_user_id_started_at ON pomodoro_sessions (user_id, started_at);

CREATE INDEX idx_pomodoro_sessions_user_id_finished_at ON pomodoro_sessions (user_id, finished_at)
    WHERE finished_at IS NOT NULL;
