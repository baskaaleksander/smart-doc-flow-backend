package com.baskaaleksander.smartdocflowbackend.utils;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Optional;

@Component
public class CookieUtil {

    public void sendRefreshTokenCookie(String token, HttpServletResponse response) {
        Cookie cookie = new Cookie("refreshToken", token);
        cookie.setMaxAge(7 * 24 * 60 * 60);
        cookie.setPath("/");
        cookie.setSecure(true);
        cookie.setHttpOnly(true);
        cookie.setAttribute("SameSite", "Strict");
        response.addCookie(cookie);

    }

    public String parseRefreshTokenCookie(HttpServletRequest request) {
        Cookie refreshCookie = Arrays.stream(Optional.ofNullable(request.getCookies()).orElse(new Cookie[0]))
                .filter(cookie -> "refreshToken".equalsIgnoreCase(cookie.getName()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Refresh token cookie not found"));

        return refreshCookie.getValue();
    }
}
