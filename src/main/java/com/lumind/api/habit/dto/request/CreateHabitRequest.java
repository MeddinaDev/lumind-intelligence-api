package com.lumind.api.habit.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateHabitRequest(
        @NotBlank
        @Size(min = 1, max = 100)
        String name,

        @Size(max = 500)
        String description
) {
}
