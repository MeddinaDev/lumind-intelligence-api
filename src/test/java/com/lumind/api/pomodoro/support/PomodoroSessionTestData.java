package com.lumind.api.pomodoro.support;

import com.lumind.api.pomodoro.dto.request.CreatePomodoroSessionRequest;
import com.lumind.api.pomodoro.dto.request.UpdatePomodoroSessionRequest;
import com.lumind.api.pomodoro.dto.response.PomodoroSessionResponse;
import com.lumind.api.pomodoro.entity.PomodoroSession;
import com.lumind.api.user.entity.User;

import java.time.Instant;
import java.util.UUID;

public final class PomodoroSessionTestData {

    public static final int SESSION_DURATION_MINUTES = 25;

    private PomodoroSessionTestData() {
    }

    public static CreatePomodoroSessionRequest validCreateRequest() {
        return new CreatePomodoroSessionRequest(SESSION_DURATION_MINUTES, Instant.parse("2026-07-14T08:00:00Z"));
    }

    public static CreatePomodoroSessionRequest validCreateRequest(int durationMinutes, Instant startedAt) {
        return new CreatePomodoroSessionRequest(durationMinutes, startedAt);
    }

    public static UpdatePomodoroSessionRequest validUpdateRequest() {
        return new UpdatePomodoroSessionRequest(25, true, Instant.parse("2026-07-14T08:25:00Z"));
    }

    public static PomodoroSession sampleSession(User user) {
        PomodoroSession session = new PomodoroSession();
        session.setId(UUID.randomUUID());
        session.setUser(user);
        session.setDurationMinutes(SESSION_DURATION_MINUTES);
        session.setCompletedMinutes(0);
        session.setCompleted(false);
        Instant startedAt = Instant.parse("2026-07-14T08:00:00Z");
        session.setStartedAt(startedAt);
        session.setFinishedAt(null);
        Instant now = Instant.now();
        session.setCreatedAt(now);
        session.setUpdatedAt(now);
        return session;
    }

    public static PomodoroSessionResponse sampleResponse(PomodoroSession session) {
        return new PomodoroSessionResponse(
                session.getId(),
                session.getUser().getId(),
                session.getDurationMinutes(),
                session.getCompletedMinutes(),
                session.getCompleted(),
                session.getStartedAt(),
                session.getFinishedAt(),
                session.getCreatedAt(),
                session.getUpdatedAt()
        );
    }
}
