package com.lumind.api.pomodoro;

import com.lumind.api.auth.support.AuthTestData;
import com.lumind.api.pomodoro.dto.request.CreatePomodoroSessionRequest;
import com.lumind.api.pomodoro.dto.request.UpdatePomodoroSessionRequest;
import com.lumind.api.pomodoro.dto.response.PomodoroSessionResponse;
import com.lumind.api.pomodoro.entity.PomodoroSession;
import com.lumind.api.pomodoro.exception.PomodoroSessionNotFoundException;
import com.lumind.api.pomodoro.mapper.PomodoroSessionMapper;
import com.lumind.api.pomodoro.repository.PomodoroSessionRepository;
import com.lumind.api.pomodoro.service.PomodoroSessionService;
import com.lumind.api.pomodoro.support.PomodoroSessionTestData;
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
class PomodoroSessionServiceTest {

    @Mock
    private PomodoroSessionRepository pomodoroSessionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PomodoroSessionMapper pomodoroSessionMapper;

    @InjectMocks
    private PomodoroSessionService pomodoroSessionService;

    private User user;
    private PomodoroSession session;
    private PomodoroSessionResponse sessionResponse;

    @BeforeEach
    void setUp() {
        user = AuthTestData.activeUser();
        session = PomodoroSessionTestData.sampleSession(user);
        sessionResponse = PomodoroSessionTestData.sampleResponse(session);
    }

    @Test
    void create_validRequest_assignsUserSavesAndReturnsResponse() {
        CreatePomodoroSessionRequest request = PomodoroSessionTestData.validCreateRequest();
        PomodoroSession mappedSession = new PomodoroSession();
        mappedSession.setDurationMinutes(request.durationMinutes());
        mappedSession.setStartedAt(request.startedAt());

        when(userRepository.getReferenceById(user.getId())).thenReturn(user);
        when(pomodoroSessionMapper.toEntity(request)).thenReturn(mappedSession);
        when(pomodoroSessionRepository.save(mappedSession)).thenAnswer(invocation -> {
            PomodoroSession saved = invocation.getArgument(0);
            saved.setId(session.getId());
            saved.setCreatedAt(session.getCreatedAt());
            saved.setUpdatedAt(session.getUpdatedAt());
            return saved;
        });
        when(pomodoroSessionMapper.toResponse(any(PomodoroSession.class))).thenReturn(sessionResponse);

        PomodoroSessionResponse response = pomodoroSessionService.create(user.getId(), request);

        assertThat(response).isEqualTo(sessionResponse);

        ArgumentCaptor<PomodoroSession> sessionCaptor = ArgumentCaptor.forClass(PomodoroSession.class);
        verify(pomodoroSessionRepository).save(sessionCaptor.capture());
        assertThat(sessionCaptor.getValue().getUser()).isEqualTo(user);
        assertThat(sessionCaptor.getValue().getDurationMinutes()).isEqualTo(request.durationMinutes());
    }

    @Test
    void getById_existingSession_returnsResponse() {
        when(pomodoroSessionRepository.findByIdAndUser_Id(session.getId(), user.getId()))
                .thenReturn(Optional.of(session));
        when(pomodoroSessionMapper.toResponse(session)).thenReturn(sessionResponse);

        PomodoroSessionResponse response = pomodoroSessionService.getById(user.getId(), session.getId());

        assertThat(response).isEqualTo(sessionResponse);
    }

    @Test
    void getById_sessionNotFound_throwsPomodoroSessionNotFoundException() {
        UUID missingSessionId = UUID.randomUUID();

        when(pomodoroSessionRepository.findByIdAndUser_Id(missingSessionId, user.getId()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> pomodoroSessionService.getById(user.getId(), missingSessionId))
                .isInstanceOf(PomodoroSessionNotFoundException.class);
    }

    @Test
    void getById_otherUsersSession_throwsPomodoroSessionNotFoundException() {
        UUID otherUserId = UUID.randomUUID();

        when(pomodoroSessionRepository.findByIdAndUser_Id(session.getId(), otherUserId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> pomodoroSessionService.getById(otherUserId, session.getId()))
                .isInstanceOf(PomodoroSessionNotFoundException.class);
    }

    @Test
    void getAllByUserId_returnsSessionsForUser() {
        PomodoroSession secondSession = PomodoroSessionTestData.sampleSession(user);
        secondSession.setId(UUID.randomUUID());
        secondSession.setDurationMinutes(50);
        PomodoroSessionResponse secondResponse = PomodoroSessionTestData.sampleResponse(secondSession);

        when(pomodoroSessionRepository.findAllByUser_Id(user.getId())).thenReturn(List.of(session, secondSession));
        when(pomodoroSessionMapper.toResponse(session)).thenReturn(sessionResponse);
        when(pomodoroSessionMapper.toResponse(secondSession)).thenReturn(secondResponse);

        List<PomodoroSessionResponse> responses = pomodoroSessionService.getAllByUserId(user.getId());

        assertThat(responses).containsExactly(sessionResponse, secondResponse);
        verify(pomodoroSessionRepository).findAllByUser_Id(user.getId());
    }

    @Test
    void update_existingSession_updatesAndReturnsResponse() {
        UpdatePomodoroSessionRequest request = PomodoroSessionTestData.validUpdateRequest();
        PomodoroSession updatedSession = PomodoroSessionTestData.sampleSession(user);
        updatedSession.setCompletedMinutes(request.completedMinutes());
        updatedSession.setCompleted(request.completed());
        updatedSession.setFinishedAt(request.finishedAt());
        PomodoroSessionResponse updatedResponse = PomodoroSessionTestData.sampleResponse(updatedSession);

        when(pomodoroSessionRepository.findByIdAndUser_Id(session.getId(), user.getId()))
                .thenReturn(Optional.of(session));
        when(pomodoroSessionRepository.save(session)).thenReturn(updatedSession);
        when(pomodoroSessionMapper.toResponse(updatedSession)).thenReturn(updatedResponse);

        PomodoroSessionResponse response = pomodoroSessionService.update(user.getId(), session.getId(), request);

        assertThat(response).isEqualTo(updatedResponse);
        verify(pomodoroSessionMapper).updateEntity(request, session);
    }

    @Test
    void update_sessionNotFound_throwsPomodoroSessionNotFoundException() {
        UpdatePomodoroSessionRequest request = PomodoroSessionTestData.validUpdateRequest();
        UUID missingSessionId = UUID.randomUUID();

        when(pomodoroSessionRepository.findByIdAndUser_Id(missingSessionId, user.getId()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> pomodoroSessionService.update(user.getId(), missingSessionId, request))
                .isInstanceOf(PomodoroSessionNotFoundException.class);

        verify(pomodoroSessionMapper, never()).updateEntity(eq(request), any());
        verify(pomodoroSessionRepository, never()).save(any());
    }

    @Test
    void delete_existingSession_deletesFromRepository() {
        when(pomodoroSessionRepository.findByIdAndUser_Id(session.getId(), user.getId()))
                .thenReturn(Optional.of(session));

        pomodoroSessionService.delete(user.getId(), session.getId());

        verify(pomodoroSessionRepository).delete(session);
    }

    @Test
    void delete_sessionNotFound_throwsPomodoroSessionNotFoundException() {
        UUID missingSessionId = UUID.randomUUID();

        when(pomodoroSessionRepository.findByIdAndUser_Id(missingSessionId, user.getId()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> pomodoroSessionService.delete(user.getId(), missingSessionId))
                .isInstanceOf(PomodoroSessionNotFoundException.class);

        verify(pomodoroSessionRepository, never()).delete(any());
    }
}
