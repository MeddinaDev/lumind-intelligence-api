package com.lumind.api.task.support;

import com.lumind.api.task.dto.request.CreateTaskRequest;
import com.lumind.api.task.dto.request.UpdateTaskRequest;
import com.lumind.api.task.dto.response.TaskResponse;
import com.lumind.api.task.entity.Task;
import com.lumind.api.user.entity.User;

import java.time.Instant;
import java.util.UUID;

public final class TaskTestData {

    public static final String TASK_TITLE = "Finish project report";
    public static final String TASK_DESCRIPTION = "Complete the Q2 summary";

    private TaskTestData() {
    }

    public static CreateTaskRequest validCreateRequest() {
        return new CreateTaskRequest(TASK_TITLE, TASK_DESCRIPTION, null);
    }

    public static CreateTaskRequest validCreateRequest(String title, String description) {
        return new CreateTaskRequest(title, description, null);
    }

    public static UpdateTaskRequest validUpdateRequest() {
        return new UpdateTaskRequest("Review pull requests", "Check open PRs", true);
    }

    public static Task sampleTask(User user) {
        Task task = new Task();
        task.setId(UUID.randomUUID());
        task.setUser(user);
        task.setTitle(TASK_TITLE);
        task.setDescription(TASK_DESCRIPTION);
        task.setCompleted(false);
        Instant now = Instant.now();
        task.setCreatedAt(now);
        task.setUpdatedAt(now);
        return task;
    }

    public static TaskResponse sampleResponse(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getUser().getId(),
                task.getTitle(),
                task.getDescription(),
                task.isCompleted(),
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }
}
