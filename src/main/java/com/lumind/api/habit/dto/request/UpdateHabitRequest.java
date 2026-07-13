package com.lumind.api.habit.dto.request;

import jakarta.validation.constraints.Size;

public record UpdateHabitRequest(
        @Size(min = 1, max = 100)
        String name,

        @Size(max = 500)
        String description
) {
}
