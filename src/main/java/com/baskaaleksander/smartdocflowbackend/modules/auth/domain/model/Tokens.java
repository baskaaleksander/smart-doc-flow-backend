package com.baskaaleksander.smartdocflowbackend.modules.auth.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor @NoArgsConstructor
@Getter @Setter
public class Tokens {
    private String accessToken;
    private String refreshToken;
}
