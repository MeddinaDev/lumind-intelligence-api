package com.lumind.api.habit.support;

import com.lumind.api.habit.dto.request.CreateHabitRequest;
import com.lumind.api.habit.dto.request.UpdateHabitRequest;
import com.lumind.api.habit.dto.response.HabitResponse;
import com.lumind.api.habit.entity.Habit;
import com.lumind.api.user.entity.User;

import java.time.Instant;
import java.util.UUID;

public final class HabitTestData {

    public static final String HABIT_NAME = "Morning meditation";
    public static final String HABIT_DESCRIPTION = "10 minutes daily";

    private HabitTestData() {
    }

    public static CreateHabitRequest validCreateRequest() {
        return new CreateHabitRequest(HABIT_NAME, HABIT_DESCRIPTION);
    }

    public static CreateHabitRequest validCreateRequest(String name, String description) {
        return new CreateHabitRequest(name, description);
    }

    public static UpdateHabitRequest validUpdateRequest() {
        return new UpdateHabitRequest("Evening reading", "Read for 20 minutes");
    }

    public static Habit sampleHabit(User user) {
        Habit habit = new Habit();
        habit.setId(UUID.randomUUID());
        habit.setUser(user);
        habit.setName(HABIT_NAME);
        habit.setDescription(HABIT_DESCRIPTION);
        Instant now = Instant.now();
        habit.setCreatedAt(now);
        habit.setUpdatedAt(now);
        return habit;
    }

    public static HabitResponse sampleResponse(Habit habit) {
        return new HabitResponse(
                habit.getId(),
                habit.getUser().getId(),
                habit.getName(),
                habit.getDescription(),
                habit.getCreatedAt(),
                habit.getUpdatedAt()
        );
    }
}
