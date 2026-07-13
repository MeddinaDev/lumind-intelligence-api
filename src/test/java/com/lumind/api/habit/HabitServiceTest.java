package com.lumind.api.habit;

import com.lumind.api.auth.support.AuthTestData;
import com.lumind.api.habit.dto.request.CreateHabitRequest;
import com.lumind.api.habit.dto.request.UpdateHabitRequest;
import com.lumind.api.habit.dto.response.HabitResponse;
import com.lumind.api.habit.entity.Habit;
import com.lumind.api.habit.exception.HabitNotFoundException;
import com.lumind.api.habit.mapper.HabitMapper;
import com.lumind.api.habit.repository.HabitRepository;
import com.lumind.api.habit.service.HabitService;
import com.lumind.api.habit.support.HabitTestData;
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
class HabitServiceTest {

    @Mock
    private HabitRepository habitRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private HabitMapper habitMapper;

    @InjectMocks
    private HabitService habitService;

    private User user;
    private Habit habit;
    private HabitResponse habitResponse;

    @BeforeEach
    void setUp() {
        user = AuthTestData.activeUser();
        habit = HabitTestData.sampleHabit(user);
        habitResponse = HabitTestData.sampleResponse(habit);
    }

    @Test
    void create_validRequest_assignsUserSavesAndReturnsResponse() {
        CreateHabitRequest request = HabitTestData.validCreateRequest();
        Habit mappedHabit = new Habit();
        mappedHabit.setName(request.name());
        mappedHabit.setDescription(request.description());

        when(userRepository.getReferenceById(user.getId())).thenReturn(user);
        when(habitMapper.toEntity(request)).thenReturn(mappedHabit);
        when(habitRepository.save(mappedHabit)).thenAnswer(invocation -> {
            Habit saved = invocation.getArgument(0);
            saved.setId(habit.getId());
            saved.setCreatedAt(habit.getCreatedAt());
            saved.setUpdatedAt(habit.getUpdatedAt());
            return saved;
        });
        when(habitMapper.toResponse(any(Habit.class))).thenReturn(habitResponse);

        HabitResponse response = habitService.create(user.getId(), request);

        assertThat(response).isEqualTo(habitResponse);

        ArgumentCaptor<Habit> habitCaptor = ArgumentCaptor.forClass(Habit.class);
        verify(habitRepository).save(habitCaptor.capture());
        assertThat(habitCaptor.getValue().getUser()).isEqualTo(user);
        assertThat(habitCaptor.getValue().getName()).isEqualTo(request.name());
    }

    @Test
    void getById_existingHabit_returnsResponse() {
        when(habitRepository.findByIdAndUser_Id(habit.getId(), user.getId()))
                .thenReturn(Optional.of(habit));
        when(habitMapper.toResponse(habit)).thenReturn(habitResponse);

        HabitResponse response = habitService.getById(user.getId(), habit.getId());

        assertThat(response).isEqualTo(habitResponse);
    }

    @Test
    void getById_habitNotFound_throwsHabitNotFoundException() {
        UUID missingHabitId = UUID.randomUUID();

        when(habitRepository.findByIdAndUser_Id(missingHabitId, user.getId()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> habitService.getById(user.getId(), missingHabitId))
                .isInstanceOf(HabitNotFoundException.class);
    }

    @Test
    void getById_otherUsersHabit_throwsHabitNotFoundException() {
        UUID otherUserId = UUID.randomUUID();

        when(habitRepository.findByIdAndUser_Id(habit.getId(), otherUserId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> habitService.getById(otherUserId, habit.getId()))
                .isInstanceOf(HabitNotFoundException.class);
    }

    @Test
    void getAllByUserId_returnsHabitsForUser() {
        Habit secondHabit = HabitTestData.sampleHabit(user);
        secondHabit.setId(UUID.randomUUID());
        secondHabit.setName("Evening walk");
        HabitResponse secondResponse = HabitTestData.sampleResponse(secondHabit);

        when(habitRepository.findAllByUser_Id(user.getId())).thenReturn(List.of(habit, secondHabit));
        when(habitMapper.toResponse(habit)).thenReturn(habitResponse);
        when(habitMapper.toResponse(secondHabit)).thenReturn(secondResponse);

        List<HabitResponse> responses = habitService.getAllByUserId(user.getId());

        assertThat(responses).containsExactly(habitResponse, secondResponse);
        verify(habitRepository).findAllByUser_Id(user.getId());
    }

    @Test
    void update_existingHabit_updatesAndReturnsResponse() {
        UpdateHabitRequest request = HabitTestData.validUpdateRequest();
        Habit updatedHabit = HabitTestData.sampleHabit(user);
        updatedHabit.setName(request.name());
        updatedHabit.setDescription(request.description());
        HabitResponse updatedResponse = HabitTestData.sampleResponse(updatedHabit);

        when(habitRepository.findByIdAndUser_Id(habit.getId(), user.getId()))
                .thenReturn(Optional.of(habit));
        when(habitRepository.save(habit)).thenReturn(updatedHabit);
        when(habitMapper.toResponse(updatedHabit)).thenReturn(updatedResponse);

        HabitResponse response = habitService.update(user.getId(), habit.getId(), request);

        assertThat(response).isEqualTo(updatedResponse);
        verify(habitMapper).updateEntity(request, habit);
    }

    @Test
    void update_habitNotFound_throwsHabitNotFoundException() {
        UpdateHabitRequest request = HabitTestData.validUpdateRequest();
        UUID missingHabitId = UUID.randomUUID();

        when(habitRepository.findByIdAndUser_Id(missingHabitId, user.getId()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> habitService.update(user.getId(), missingHabitId, request))
                .isInstanceOf(HabitNotFoundException.class);

        verify(habitMapper, never()).updateEntity(eq(request), any());
        verify(habitRepository, never()).save(any());
    }

    @Test
    void delete_existingHabit_deletesFromRepository() {
        when(habitRepository.findByIdAndUser_Id(habit.getId(), user.getId()))
                .thenReturn(Optional.of(habit));

        habitService.delete(user.getId(), habit.getId());

        verify(habitRepository).delete(habit);
    }

    @Test
    void delete_habitNotFound_throwsHabitNotFoundException() {
        UUID missingHabitId = UUID.randomUUID();

        when(habitRepository.findByIdAndUser_Id(missingHabitId, user.getId()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> habitService.delete(user.getId(), missingHabitId))
                .isInstanceOf(HabitNotFoundException.class);

        verify(habitRepository, never()).delete(any());
    }
}
