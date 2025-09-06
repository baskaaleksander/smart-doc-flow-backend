package com.baskaaleksander.smartdocflowbackend.controller;

import com.baskaaleksander.smartdocflowbackend.dto.request.UserRequest;
import com.baskaaleksander.smartdocflowbackend.dto.response.UserLoginResponse;
import com.baskaaleksander.smartdocflowbackend.service.AuthSevice;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.Optional;

@RestController()
@RequestMapping("/auth")
public class AuthController {

    private final AuthSevice authSevice;

    @Autowired
    public AuthController(AuthSevice authSevice) {
        this.authSevice = authSevice;
    }


    @PostMapping("/login")
    public String loginUser(@RequestBody @Valid UserRequest user, HttpServletResponse response){
        UserLoginResponse res = authSevice.loginUser(user);

        Cookie cookie = new Cookie("refreshToken", res.refreshToken());
        cookie.setMaxAge(7 * 24 * 60 * 60);
        cookie.setPath("/");
        cookie.setSecure(true);
        cookie.setHttpOnly(true);
        response.addCookie(cookie);

        return res.accessToken();
    }

    @PostMapping("/register")
    public String registerUser(@RequestBody @Valid UserRequest user) {
        return authSevice.registerUser(user);
    }

    @PostMapping("/refresh")
    public String refreshAccessToken(HttpServletRequest request) {
        Cookie refreshCookie = Arrays.stream(Optional.ofNullable(request.getCookies()).orElse(new Cookie[0]))
                .filter(cookie -> "refreshToken".equalsIgnoreCase(cookie.getName()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Refresh token cookie not found"));

        return authSevice.refreshAccessToken(refreshCookie.getValue());
    }

}
