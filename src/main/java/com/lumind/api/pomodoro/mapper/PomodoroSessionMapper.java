package com.lumind.api.pomodoro.mapper;

import com.lumind.api.pomodoro.dto.request.CreatePomodoroSessionRequest;
import com.lumind.api.pomodoro.dto.request.UpdatePomodoroSessionRequest;
import com.lumind.api.pomodoro.dto.response.PomodoroSessionResponse;
import com.lumind.api.pomodoro.entity.PomodoroSession;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface PomodoroSessionMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "completedMinutes", ignore = true)
    @Mapping(target = "completed", ignore = true)
    @Mapping(target = "finishedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    PomodoroSession toEntity(CreatePomodoroSessionRequest request);

    @Mapping(target = "userId", source = "user.id")
    PomodoroSessionResponse toResponse(PomodoroSession session);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "durationMinutes", ignore = true)
    @Mapping(target = "startedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(UpdatePomodoroSessionRequest request, @MappingTarget PomodoroSession session);
}
