package com.lumind.api.auth.mapper;

import com.lumind.api.auth.dto.request.RegisterRequest;
import com.lumind.api.auth.dto.response.AuthResponse;
import com.lumind.api.auth.model.IssuedTokens;
import com.lumind.api.user.entity.User;
import com.lumind.api.user.mapper.UserMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = UserMapper.class)
public interface AuthMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "enabled", constant = "true")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    User toUser(RegisterRequest request);

    @Mapping(target = "accessToken", source = "issuedTokens.accessToken")
    @Mapping(target = "refreshToken", source = "issuedTokens.refreshToken")
    @Mapping(target = "tokenType", constant = "Bearer")
    @Mapping(target = "expiresIn", source = "expiresIn")
    @Mapping(target = "user", source = "user")
    AuthResponse toAuthResponse(IssuedTokens issuedTokens, User user, long expiresIn);
}
