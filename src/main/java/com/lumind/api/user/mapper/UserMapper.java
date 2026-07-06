package com.lumind.api.user.mapper;

import com.lumind.api.user.dto.response.UserSummaryResponse;
import com.lumind.api.user.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserSummaryResponse toSummaryResponse(User user);
}
