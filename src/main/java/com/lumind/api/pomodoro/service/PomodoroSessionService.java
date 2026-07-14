package com.lumind.api.pomodoro.service;

import com.lumind.api.pomodoro.dto.request.CreatePomodoroSessionRequest;
import com.lumind.api.pomodoro.dto.request.UpdatePomodoroSessionRequest;
import com.lumind.api.pomodoro.dto.response.PomodoroSessionResponse;
import com.lumind.api.pomodoro.entity.PomodoroSession;
import com.lumind.api.pomodoro.exception.PomodoroSessionNotFoundException;
import com.lumind.api.pomodoro.mapper.PomodoroSessionMapper;
import com.lumind.api.pomodoro.repository.PomodoroSessionRepository;
import com.lumind.api.user.entity.User;
import com.lumind.api.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class PomodoroSessionService {

    private final PomodoroSessionRepository pomodoroSessionRepository;
    private final UserRepository userRepository;
    private final PomodoroSessionMapper pomodoroSessionMapper;

    public PomodoroSessionService(
            PomodoroSessionRepository pomodoroSessionRepository,
            UserRepository userRepository,
            PomodoroSessionMapper pomodoroSessionMapper
    ) {
        this.pomodoroSessionRepository = pomodoroSessionRepository;
        this.userRepository = userRepository;
        this.pomodoroSessionMapper = pomodoroSessionMapper;
    }

    @Transactional
    public PomodoroSessionResponse create(UUID userId, CreatePomodoroSessionRequest request) {
        User user = userRepository.getReferenceById(userId);

        PomodoroSession session = pomodoroSessionMapper.toEntity(request);
        session.setUser(user);
        session = pomodoroSessionRepository.save(session);

        log.info("Pomodoro session created: sessionId={}, userId={}", session.getId(), userId);
        return pomodoroSessionMapper.toResponse(session);
    }

    @Transactional(readOnly = true)
    public PomodoroSessionResponse getById(UUID userId, UUID sessionId) {
        PomodoroSession session = findSessionForUser(userId, sessionId);
        return pomodoroSessionMapper.toResponse(session);
    }

    @Transactional(readOnly = true)
    public List<PomodoroSessionResponse> getAllByUserId(UUID userId) {
        return pomodoroSessionRepository.findAllByUser_Id(userId).stream()
                .map(pomodoroSessionMapper::toResponse)
                .toList();
    }

    @Transactional
    public PomodoroSessionResponse update(UUID userId, UUID sessionId, UpdatePomodoroSessionRequest request) {
        PomodoroSession session = findSessionForUser(userId, sessionId);
        pomodoroSessionMapper.updateEntity(request, session);
        session = pomodoroSessionRepository.save(session);

        log.info("Pomodoro session updated: sessionId={}, userId={}", sessionId, userId);
        return pomodoroSessionMapper.toResponse(session);
    }

    @Transactional
    public void delete(UUID userId, UUID sessionId) {
        PomodoroSession session = findSessionForUser(userId, sessionId);
        pomodoroSessionRepository.delete(session);

        log.info("Pomodoro session deleted: sessionId={}, userId={}", sessionId, userId);
    }

    private PomodoroSession findSessionForUser(UUID userId, UUID sessionId) {
        return pomodoroSessionRepository.findByIdAndUser_Id(sessionId, userId)
                .orElseThrow(PomodoroSessionNotFoundException::new);
    }
}
