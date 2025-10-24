package com.baskaaleksander.smartdocflowbackend.modules.users.adapters.api;

import com.baskaaleksander.smartdocflowbackend.modules.auth.adapters.api.dto.UserResponse;
import com.baskaaleksander.smartdocflowbackend.modules.users.domain.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class UserApiMapperTest {

    private final UserApiMapper mapper = Mappers.getMapper(UserApiMapper.class);

    @Test
    void toUserResponse_mapsAllFieldsCorrectly() {
        UUID id = UUID.randomUUID();
        User user = new User();
        user.setId(id);
        user.setEmail("john@doe.com");
        user.setRoles(Set.of("ROLE_USER", "ROLE_ADMIN"));
        user.setActive(true);

        UserResponse response = mapper.toUserResponse(user);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(id);
        assertThat(response.email()).isEqualTo("john@doe.com");
        assertThat(response.roles()).containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
        assertThat(response.active()).isTrue();
    }
}
