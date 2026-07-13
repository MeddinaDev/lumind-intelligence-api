package com.lumind.api.habit.mapper;

import com.lumind.api.habit.dto.request.CreateHabitRequest;
import com.lumind.api.habit.dto.request.UpdateHabitRequest;
import com.lumind.api.habit.dto.response.HabitResponse;
import com.lumind.api.habit.entity.Habit;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface HabitMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Habit toEntity(CreateHabitRequest request);

    @Mapping(target = "userId", source = "user.id")
    HabitResponse toResponse(Habit habit);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(UpdateHabitRequest request, @MappingTarget Habit habit);
}
