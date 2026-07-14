package com.lumind.api.task.repository;

import com.lumind.api.task.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID> {

    Optional<Task> findByIdAndUser_Id(UUID id, UUID userId);

    List<Task> findAllByUser_Id(UUID userId);
}
