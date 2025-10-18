package com.baskaaleksander.smartdocflowbackend.modules.users.adapters.api;

import com.baskaaleksander.smartdocflowbackend.modules.auth.adapters.api.dto.UserResponse;
import com.baskaaleksander.smartdocflowbackend.modules.auth.adapters.persistence.entity.RoleEntity;
import com.baskaaleksander.smartdocflowbackend.modules.users.adapters.persistence.entity.UserEntity;
import org.mapstruct.Mapper;

// TODO: delete this
@Mapper(componentModel = "spring")
public interface UserMapper {

    UserResponse toUserResponse(UserEntity user);

    default String map(RoleEntity role) {
        return role.getRole();
    }
}
