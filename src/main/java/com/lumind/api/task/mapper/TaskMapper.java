package com.lumind.api.task.mapper;

import com.lumind.api.task.dto.request.CreateTaskRequest;
import com.lumind.api.task.dto.request.UpdateTaskRequest;
import com.lumind.api.task.dto.response.TaskResponse;
import com.lumind.api.task.entity.Task;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface TaskMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "completed", defaultValue = "false")
    Task toEntity(CreateTaskRequest request);

    @Mapping(target = "userId", source = "user.id")
    TaskResponse toResponse(Task task);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(UpdateTaskRequest request, @MappingTarget Task task);
}
