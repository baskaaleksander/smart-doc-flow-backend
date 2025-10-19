package com.baskaaleksander.smartdocflowbackend.modules.auth.domain.port;

import com.baskaaleksander.smartdocflowbackend.modules.auth.domain.model.Tokens;

public interface TokenUtilPort {
    Tokens issueTokens(String username);
    String generateAccessToken(String username);
    String generateRefreshToken(String username, String jti);
    Tokens refreshAccessToken(String refreshToken);
    String getUsernameFromAccessToken(String accessToken);
    String getUsernameFromRefreshToken(String refreshToken);
    String getJtiFromRefreshToken(String refreshToken);
    Boolean validateAccessToken(String accessToken);
    Boolean validateRefreshToken(String refreshToken);
    void invalidateRefreshToken(String refreshToken);
}
