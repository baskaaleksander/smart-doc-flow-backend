package com.baskaaleksander.smartdocflowbackend.modules.auth.api;

import com.baskaaleksander.smartdocflowbackend.common.security.CustomUserDetails;
import com.baskaaleksander.smartdocflowbackend.modules.auth.api.dto.*;
import com.baskaaleksander.smartdocflowbackend.modules.auth.application.AuthService;
import com.baskaaleksander.smartdocflowbackend.common.util.CookieUtil;
import com.baskaaleksander.smartdocflowbackend.modules.auth.application.PasswordChangeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;


@RestController()
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final CookieUtil cookieUtil;
    private final PasswordChangeService passwordChangeService;

    @Autowired
    public AuthController(
            AuthService authService,
            CookieUtil cookieUtil,
            PasswordChangeService passwordChangeService
    ) {
        this.authService = authService;
        this.cookieUtil = cookieUtil;
        this.passwordChangeService = passwordChangeService;
    }


    @PostMapping("/login")
    public ResponseEntity<String> loginUser(@RequestBody @Valid UserLoginRequest user, HttpServletResponse response){
        TokenResponse res = authService.loginUser(user);

        cookieUtil.sendRefreshTokenCookie(res.refreshToken(), response);

        return new ResponseEntity<>(res.accessToken(), HttpStatus.OK);
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> registerUser(@RequestBody @Valid UserRegisterRequest user) {
        return new ResponseEntity<>(authService.registerUser(user), HttpStatus.CREATED);
    }

    @GetMapping("/refresh")
    public ResponseEntity<String> refreshAccessToken(HttpServletRequest request, HttpServletResponse response) {

        String refreshCookie = cookieUtil.parseRefreshTokenCookie(request);

        TokenResponse tokenResponse = authService.refreshAccessToken(refreshCookie);

        cookieUtil.sendRefreshTokenCookie(tokenResponse.refreshToken(), response);

        return new ResponseEntity<>(tokenResponse.accessToken(), HttpStatus.OK);
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logoutUser(HttpServletRequest request, HttpServletResponse response) {
        return new ResponseEntity<>(authService.logoutUser(request, response), HttpStatus.OK);
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal UserDetails user) {

        return new ResponseEntity<>(authService.getMe(user), HttpStatus.OK);
    }

    @PutMapping("/update-password")
    public ResponseEntity<String> updatePassword(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ChangePasswordRequest changePasswordRequest
            ) {
        return new ResponseEntity<>(passwordChangeService.updatePassword(userDetails.getId(), changePasswordRequest), HttpStatus.OK);
    }

}
