package com.baskaaleksander.smartdocflowbackend.controller;

import com.baskaaleksander.smartdocflowbackend.dto.request.UserRequest;
import com.baskaaleksander.smartdocflowbackend.dto.response.UserLoginResponse;
import com.baskaaleksander.smartdocflowbackend.dto.response.UserResponse;
import com.baskaaleksander.smartdocflowbackend.service.AuthSevice;
import com.baskaaleksander.smartdocflowbackend.utils.CookieUtil;
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
    private final CookieUtil cookieUtil;

    @Autowired
    public AuthController(
            AuthSevice authSevice,
            CookieUtil cookieUtil
    ) {
        this.authSevice = authSevice;
        this.cookieUtil = cookieUtil;
    }


    @PostMapping("/login")
    public String loginUser(@RequestBody @Valid UserRequest user, HttpServletResponse response){
        UserLoginResponse res = authSevice.loginUser(user);

        cookieUtil.sendRefreshTokenCookie(res.accessToken(), response);

        return res.accessToken();
    }

    @PostMapping("/register")
    public UserResponse registerUser(@RequestBody @Valid UserRequest user) {
        return authSevice.registerUser(user);
    }

    @PostMapping("/refresh")
    public String refreshAccessToken(HttpServletRequest request) {

        String refreshCookie = cookieUtil.parseRefreshTokenCookie(request);

        return authSevice.refreshAccessToken(refreshCookie);
    }

}
