package com.baskaaleksander.smartdocflowbackend.modules.auth.domain.port;

import com.baskaaleksander.smartdocflowbackend.modules.auth.domain.model.RefreshToken;

public interface RefreshTokenCommandPort {
    RefreshToken save(RefreshToken token);
}
