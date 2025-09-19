package com.baskaaleksander.smartdocflowbackend.modules.users.mapping;

import com.baskaaleksander.smartdocflowbackend.modules.auth.api.dto.UserResponse;
import com.baskaaleksander.smartdocflowbackend.modules.auth.persistence.Role;
import com.baskaaleksander.smartdocflowbackend.modules.users.persistence.User;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserMapper INSTANCE = Mappers.getMapper(UserMapper.class);

    UserResponse toUserResponse(User user);

    default String map(Role role) {
        return role.getRole();
    }
}
