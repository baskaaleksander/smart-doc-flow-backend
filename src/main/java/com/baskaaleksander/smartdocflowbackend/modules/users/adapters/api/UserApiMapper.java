package com.baskaaleksander.smartdocflowbackend.modules.users.adapters.api;

import com.baskaaleksander.smartdocflowbackend.modules.auth.api.dto.UserResponse;
import com.baskaaleksander.smartdocflowbackend.modules.users.domain.model.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserApiMapper {

    UserResponse toUserResponse(User user);
}
