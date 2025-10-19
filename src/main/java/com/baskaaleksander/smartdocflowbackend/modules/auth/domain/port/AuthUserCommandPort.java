package com.baskaaleksander.smartdocflowbackend.modules.auth.domain.port;

import com.baskaaleksander.smartdocflowbackend.modules.auth.domain.model.AuthUser;

public interface AuthUserCommandPort {
    AuthUser save(AuthUser user);
}
