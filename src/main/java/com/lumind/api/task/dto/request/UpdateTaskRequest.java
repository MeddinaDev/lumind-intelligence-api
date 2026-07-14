package com.lumind.api.task.dto.request;

import jakarta.validation.constraints.Size;

public record UpdateTaskRequest(
        @Size(min = 1, max = 100)
        String title,

        @Size(max = 500)
        String description,

        Boolean completed
) {
}
