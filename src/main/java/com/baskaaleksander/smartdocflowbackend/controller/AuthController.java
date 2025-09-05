package com.baskaaleksander.smartdocflowbackend.controller;

import com.baskaaleksander.smartdocflowbackend.dto.request.UserRequest;
import com.baskaaleksander.smartdocflowbackend.service.AuthSevice;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController()
@RequestMapping("/auth")
public class AuthController {

    private final AuthSevice authSevice;

    @Autowired
    public AuthController(AuthSevice authSevice) {
        this.authSevice = authSevice;
    }


    @PostMapping("/login")
    private String loginUser(@RequestBody @Valid UserRequest user){
        return authSevice.loginUser(user);
    }

    @PostMapping("/register")
    private String registerUser(@RequestBody @Valid UserRequest user) {
        return authSevice.registerUser(user);
    }

}
