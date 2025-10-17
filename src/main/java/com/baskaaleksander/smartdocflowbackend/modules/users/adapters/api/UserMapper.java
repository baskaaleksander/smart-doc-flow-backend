package com.baskaaleksander.smartdocflowbackend.modules.users.adapters.api;

import com.baskaaleksander.smartdocflowbackend.modules.auth.api.dto.UserResponse;
import com.baskaaleksander.smartdocflowbackend.modules.auth.persistence.Role;
import com.baskaaleksander.smartdocflowbackend.modules.users.adapters.persistence.entity.UserEntity;
import org.mapstruct.Mapper;

// TODO: delete this
@Mapper(componentModel = "spring")
public interface UserMapper {

    UserResponse toUserResponse(UserEntity user);

    default String map(Role role) {
        return role.getRole();
    }
}
