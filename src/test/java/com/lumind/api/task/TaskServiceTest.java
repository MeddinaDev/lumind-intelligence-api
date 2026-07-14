package com.lumind.api.task;

import com.lumind.api.auth.support.AuthTestData;
import com.lumind.api.task.dto.request.CreateTaskRequest;
import com.lumind.api.task.dto.request.UpdateTaskRequest;
import com.lumind.api.task.dto.response.TaskResponse;
import com.lumind.api.task.entity.Task;
import com.lumind.api.task.exception.TaskNotFoundException;
import com.lumind.api.task.mapper.TaskMapper;
import com.lumind.api.task.repository.TaskRepository;
import com.lumind.api.task.service.TaskService;
import com.lumind.api.task.support.TaskTestData;
import com.lumind.api.user.entity.User;
import com.lumind.api.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TaskMapper taskMapper;

    @InjectMocks
    private TaskService taskService;

    private User user;
    private Task task;
    private TaskResponse taskResponse;

    @BeforeEach
    void setUp() {
        user = AuthTestData.activeUser();
        task = TaskTestData.sampleTask(user);
        taskResponse = TaskTestData.sampleResponse(task);
    }

    @Test
    void create_validRequest_assignsUserSavesAndReturnsResponse() {
        CreateTaskRequest request = TaskTestData.validCreateRequest();
        Task mappedTask = new Task();
        mappedTask.setTitle(request.title());
        mappedTask.setDescription(request.description());

        when(userRepository.getReferenceById(user.getId())).thenReturn(user);
        when(taskMapper.toEntity(request)).thenReturn(mappedTask);
        when(taskRepository.save(mappedTask)).thenAnswer(invocation -> {
            Task saved = invocation.getArgument(0);
            saved.setId(task.getId());
            saved.setCreatedAt(task.getCreatedAt());
            saved.setUpdatedAt(task.getUpdatedAt());
            return saved;
        });
        when(taskMapper.toResponse(any(Task.class))).thenReturn(taskResponse);

        TaskResponse response = taskService.create(user.getId(), request);

        assertThat(response).isEqualTo(taskResponse);

        ArgumentCaptor<Task> taskCaptor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).save(taskCaptor.capture());
        assertThat(taskCaptor.getValue().getUser()).isEqualTo(user);
        assertThat(taskCaptor.getValue().getTitle()).isEqualTo(request.title());
    }

    @Test
    void getById_existingTask_returnsResponse() {
        when(taskRepository.findByIdAndUser_Id(task.getId(), user.getId()))
                .thenReturn(Optional.of(task));
        when(taskMapper.toResponse(task)).thenReturn(taskResponse);

        TaskResponse response = taskService.getById(user.getId(), task.getId());

        assertThat(response).isEqualTo(taskResponse);
    }

    @Test
    void getById_taskNotFound_throwsTaskNotFoundException() {
        UUID missingTaskId = UUID.randomUUID();

        when(taskRepository.findByIdAndUser_Id(missingTaskId, user.getId()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.getById(user.getId(), missingTaskId))
                .isInstanceOf(TaskNotFoundException.class);
    }

    @Test
    void getById_otherUsersTask_throwsTaskNotFoundException() {
        UUID otherUserId = UUID.randomUUID();

        when(taskRepository.findByIdAndUser_Id(task.getId(), otherUserId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.getById(otherUserId, task.getId()))
                .isInstanceOf(TaskNotFoundException.class);
    }

    @Test
    void getAllByUserId_returnsTasksForUser() {
        Task secondTask = TaskTestData.sampleTask(user);
        secondTask.setId(UUID.randomUUID());
        secondTask.setTitle("Prepare presentation");
        TaskResponse secondResponse = TaskTestData.sampleResponse(secondTask);

        when(taskRepository.findAllByUser_Id(user.getId())).thenReturn(List.of(task, secondTask));
        when(taskMapper.toResponse(task)).thenReturn(taskResponse);
        when(taskMapper.toResponse(secondTask)).thenReturn(secondResponse);

        List<TaskResponse> responses = taskService.getAllByUserId(user.getId());

        assertThat(responses).containsExactly(taskResponse, secondResponse);
        verify(taskRepository).findAllByUser_Id(user.getId());
    }

    @Test
    void update_existingTask_updatesAndReturnsResponse() {
        UpdateTaskRequest request = TaskTestData.validUpdateRequest();
        Task updatedTask = TaskTestData.sampleTask(user);
        updatedTask.setTitle(request.title());
        updatedTask.setDescription(request.description());
        updatedTask.setCompleted(request.completed());
        TaskResponse updatedResponse = TaskTestData.sampleResponse(updatedTask);

        when(taskRepository.findByIdAndUser_Id(task.getId(), user.getId()))
                .thenReturn(Optional.of(task));
        when(taskRepository.save(task)).thenReturn(updatedTask);
        when(taskMapper.toResponse(updatedTask)).thenReturn(updatedResponse);

        TaskResponse response = taskService.update(user.getId(), task.getId(), request);

        assertThat(response).isEqualTo(updatedResponse);
        verify(taskMapper).updateEntity(request, task);
    }

    @Test
    void update_taskNotFound_throwsTaskNotFoundException() {
        UpdateTaskRequest request = TaskTestData.validUpdateRequest();
        UUID missingTaskId = UUID.randomUUID();

        when(taskRepository.findByIdAndUser_Id(missingTaskId, user.getId()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.update(user.getId(), missingTaskId, request))
                .isInstanceOf(TaskNotFoundException.class);

        verify(taskMapper, never()).updateEntity(eq(request), any());
        verify(taskRepository, never()).save(any());
    }

    @Test
    void delete_existingTask_deletesFromRepository() {
        when(taskRepository.findByIdAndUser_Id(task.getId(), user.getId()))
                .thenReturn(Optional.of(task));

        taskService.delete(user.getId(), task.getId());

        verify(taskRepository).delete(task);
    }

    @Test
    void delete_taskNotFound_throwsTaskNotFoundException() {
        UUID missingTaskId = UUID.randomUUID();

        when(taskRepository.findByIdAndUser_Id(missingTaskId, user.getId()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.delete(user.getId(), missingTaskId))
                .isInstanceOf(TaskNotFoundException.class);

        verify(taskRepository, never()).delete(any());
    }
}
