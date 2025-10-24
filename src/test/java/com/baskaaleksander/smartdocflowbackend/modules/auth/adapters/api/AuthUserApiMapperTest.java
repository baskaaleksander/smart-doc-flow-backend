package com.baskaaleksander.smartdocflowbackend.modules.auth.adapters.api;

import com.baskaaleksander.smartdocflowbackend.modules.auth.adapters.api.dto.UserResponse;
import com.baskaaleksander.smartdocflowbackend.modules.auth.domain.model.AuthUser;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AuthUserApiMapperTest {

    private final AuthUserApiMapper mapper = Mappers.getMapper(AuthUserApiMapper.class);

    @Test
    void toResponse_mapsAllFieldsCorrectly() {
        UUID id = UUID.randomUUID();
        AuthUser user = new AuthUser();
        user.setId(id);
        user.setUsername("john");
        user.setEmail("john@doe.com");
        user.setPassword("ENC");
        user.setActive(true);
        user.setRoles(Set.of("ROLE_USER", "ROLE_ADMIN"));

        UserResponse dto = mapper.toResponse(user);

        assertThat(dto).isNotNull();
        assertThat(dto.id()).isEqualTo(id);
        assertThat(dto.username()).isEqualTo("john");
        assertThat(dto.email()).isEqualTo("john@doe.com");
        assertThat(dto.active()).isTrue();
        assertThat(dto.roles()).containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
    }
}