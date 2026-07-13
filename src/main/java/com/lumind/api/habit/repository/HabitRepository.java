package com.lumind.api.habit.repository;

import com.lumind.api.habit.entity.Habit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface HabitRepository extends JpaRepository<Habit, UUID> {
}
