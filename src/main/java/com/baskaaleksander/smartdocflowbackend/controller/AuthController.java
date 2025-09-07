package com.baskaaleksander.smartdocflowbackend.controller;

import com.baskaaleksander.smartdocflowbackend.dto.request.UserRequest;
import com.baskaaleksander.smartdocflowbackend.dto.response.UserLoginResponse;
import com.baskaaleksander.smartdocflowbackend.dto.response.UserResponse;
import com.baskaaleksander.smartdocflowbackend.service.AuthSevice;
import com.baskaaleksander.smartdocflowbackend.utils.CookieUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;



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
    public ResponseEntity<String> loginUser(@RequestBody @Valid UserRequest user, HttpServletResponse response){
        UserLoginResponse res = authSevice.loginUser(user);

        cookieUtil.sendRefreshTokenCookie(res.accessToken(), response);

        return new ResponseEntity<>(res.accessToken(), HttpStatus.OK);
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> registerUser(@RequestBody @Valid UserRequest user) {
        return new ResponseEntity<>(authSevice.registerUser(user), HttpStatus.CREATED);
    }

    @PostMapping("/refresh")
    public ResponseEntity<String> refreshAccessToken(HttpServletRequest request) {

        String refreshCookie = cookieUtil.parseRefreshTokenCookie(request);

        return new ResponseEntity<>(authSevice.refreshAccessToken(refreshCookie), HttpStatus.OK);
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logoutUser(HttpServletRequest request, HttpServletResponse response) {
        return new ResponseEntity<>(authSevice.logoutUser(request, response), HttpStatus.OK);
    }

}
