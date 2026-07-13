package com.lumind.api.habit.repository;

import com.lumind.api.habit.entity.Habit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HabitRepository extends JpaRepository<Habit, UUID> {

    Optional<Habit> findByIdAndUser_Id(UUID id, UUID userId);

    List<Habit> findAllByUser_Id(UUID userId);
}
