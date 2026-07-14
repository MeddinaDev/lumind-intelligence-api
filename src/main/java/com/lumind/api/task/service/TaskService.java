package com.lumind.api.task.service;

import com.lumind.api.task.dto.request.CreateTaskRequest;
import com.lumind.api.task.dto.request.UpdateTaskRequest;
import com.lumind.api.task.dto.response.TaskResponse;
import com.lumind.api.task.entity.Task;
import com.lumind.api.task.exception.TaskNotFoundException;
import com.lumind.api.task.mapper.TaskMapper;
import com.lumind.api.task.repository.TaskRepository;
import com.lumind.api.user.entity.User;
import com.lumind.api.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final TaskMapper taskMapper;

    public TaskService(
            TaskRepository taskRepository,
            UserRepository userRepository,
            TaskMapper taskMapper
    ) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.taskMapper = taskMapper;
    }

    @Transactional
    public TaskResponse create(UUID userId, CreateTaskRequest request) {
        User user = userRepository.getReferenceById(userId);

        Task task = taskMapper.toEntity(request);
        task.setUser(user);
        task = taskRepository.save(task);

        log.info("Task created: taskId={}, userId={}", task.getId(), userId);
        return taskMapper.toResponse(task);
    }

    @Transactional(readOnly = true)
    public TaskResponse getById(UUID userId, UUID taskId) {
        Task task = findTaskForUser(userId, taskId);
        return taskMapper.toResponse(task);
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getAllByUserId(UUID userId) {
        return taskRepository.findAllByUser_Id(userId).stream()
                .map(taskMapper::toResponse)
                .toList();
    }

    @Transactional
    public TaskResponse update(UUID userId, UUID taskId, UpdateTaskRequest request) {
        Task task = findTaskForUser(userId, taskId);
        taskMapper.updateEntity(request, task);
        task = taskRepository.save(task);

        log.info("Task updated: taskId={}, userId={}", taskId, userId);
        return taskMapper.toResponse(task);
    }

    @Transactional
    public void delete(UUID userId, UUID taskId) {
        Task task = findTaskForUser(userId, taskId);
        taskRepository.delete(task);

        log.info("Task deleted: taskId={}, userId={}", taskId, userId);
    }

    private Task findTaskForUser(UUID userId, UUID taskId) {
        return taskRepository.findByIdAndUser_Id(taskId, userId)
                .orElseThrow(TaskNotFoundException::new);
    }
}
