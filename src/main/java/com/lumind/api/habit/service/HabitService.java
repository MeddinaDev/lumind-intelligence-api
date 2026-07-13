package com.lumind.api.habit.service;

import com.lumind.api.habit.dto.request.CreateHabitRequest;
import com.lumind.api.habit.dto.request.UpdateHabitRequest;
import com.lumind.api.habit.dto.response.HabitResponse;
import com.lumind.api.habit.entity.Habit;
import com.lumind.api.habit.exception.HabitNotFoundException;
import com.lumind.api.habit.mapper.HabitMapper;
import com.lumind.api.habit.repository.HabitRepository;
import com.lumind.api.user.entity.User;
import com.lumind.api.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class HabitService {

    private final HabitRepository habitRepository;
    private final UserRepository userRepository;
    private final HabitMapper habitMapper;

    public HabitService(
            HabitRepository habitRepository,
            UserRepository userRepository,
            HabitMapper habitMapper
    ) {
        this.habitRepository = habitRepository;
        this.userRepository = userRepository;
        this.habitMapper = habitMapper;
    }

    @Transactional
    public HabitResponse create(UUID userId, CreateHabitRequest request) {
        User user = userRepository.getReferenceById(userId);

        Habit habit = habitMapper.toEntity(request);
        habit.setUser(user);
        habit = habitRepository.save(habit);

        log.info("Habit created: habitId={}, userId={}", habit.getId(), userId);
        return habitMapper.toResponse(habit);
    }

    @Transactional(readOnly = true)
    public HabitResponse getById(UUID userId, UUID habitId) {
        Habit habit = findHabitForUser(userId, habitId);
        return habitMapper.toResponse(habit);
    }

    @Transactional(readOnly = true)
    public List<HabitResponse> getAllByUserId(UUID userId) {
        return habitRepository.findAllByUser_Id(userId).stream()
                .map(habitMapper::toResponse)
                .toList();
    }

    @Transactional
    public HabitResponse update(UUID userId, UUID habitId, UpdateHabitRequest request) {
        Habit habit = findHabitForUser(userId, habitId);
        habitMapper.updateEntity(request, habit);
        habit = habitRepository.save(habit);

        log.info("Habit updated: habitId={}, userId={}", habitId, userId);
        return habitMapper.toResponse(habit);
    }

    @Transactional
    public void delete(UUID userId, UUID habitId) {
        Habit habit = findHabitForUser(userId, habitId);
        habitRepository.delete(habit);

        log.info("Habit deleted: habitId={}, userId={}", habitId, userId);
    }

    private Habit findHabitForUser(UUID userId, UUID habitId) {
        return habitRepository.findByIdAndUser_Id(habitId, userId)
                .orElseThrow(HabitNotFoundException::new);
    }
}
