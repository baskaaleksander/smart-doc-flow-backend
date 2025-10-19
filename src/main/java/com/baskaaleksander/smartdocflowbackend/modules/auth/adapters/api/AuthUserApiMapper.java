package com.baskaaleksander.smartdocflowbackend.modules.auth.adapters.api;

import com.baskaaleksander.smartdocflowbackend.modules.auth.adapters.api.dto.UserResponse;
import com.baskaaleksander.smartdocflowbackend.modules.auth.domain.model.AuthUser;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AuthUserApiMapper {

    UserResponse toResponse(AuthUser user);
}
