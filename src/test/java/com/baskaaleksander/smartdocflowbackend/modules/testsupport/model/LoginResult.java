package com.baskaaleksander.smartdocflowbackend.modules.testsupport.model;

import jakarta.servlet.http.Cookie;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class LoginResult {
    String accessToken;
    Cookie refreshCookie;
}
