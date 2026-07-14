package com.lumind.api.pomodoro.repository;

import com.lumind.api.pomodoro.entity.PomodoroSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PomodoroSessionRepository extends JpaRepository<PomodoroSession, UUID> {

    Optional<PomodoroSession> findByIdAndUser_Id(UUID id, UUID userId);

    List<PomodoroSession> findAllByUser_Id(UUID userId);
}
