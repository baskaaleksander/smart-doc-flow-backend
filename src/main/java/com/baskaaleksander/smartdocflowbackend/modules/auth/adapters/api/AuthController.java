package com.baskaaleksander.smartdocflowbackend.modules.auth.adapters.api;

import com.baskaaleksander.smartdocflowbackend.common.security.CustomUserDetails;
import com.baskaaleksander.smartdocflowbackend.modules.auth.adapters.api.dto.*;
import com.baskaaleksander.smartdocflowbackend.modules.auth.application.AuthApplicationService;
import com.baskaaleksander.smartdocflowbackend.common.util.CookieUtil;
import com.baskaaleksander.smartdocflowbackend.modules.auth.application.PasswordChangeApplicationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Endpoints for authentication")
public class AuthController {

    private final AuthApplicationService authService;
    private final CookieUtil cookieUtil;
    private final PasswordChangeApplicationService passwordChangeService;

    @PostMapping("/login")
    public ResponseEntity<String> loginUser(@RequestBody @Valid UserLoginRequest user, HttpServletResponse response) {
        TokenResponse res = authService.loginUser(user);

        cookieUtil.sendRefreshTokenCookie(res.refreshToken(), response);

        return new ResponseEntity<>(res.accessToken(), HttpStatus.OK);
    }

    @PostMapping("/register")
    @PreAuthorize("hasAnyRole('ADMIN')")
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
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal CustomUserDetails user) {

        return new ResponseEntity<>(authService.getMe(user.getId()), HttpStatus.OK);
    }

    @PutMapping("/update-password")
    public ResponseEntity<String> updatePassword(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody UpdatePasswordRequest updatePasswordRequest
    ) {
        return new ResponseEntity<>(passwordChangeService.updatePassword(userDetails.getId(), updatePasswordRequest), HttpStatus.OK);
    }

    @PostMapping("/request-password-reset")
    public ResponseEntity<String> requestPasswordReset(
            @Valid @RequestBody PasswordResetRequest passwordResetRequest
    ) {
        return new ResponseEntity<>(passwordChangeService.requestPasswordReset(passwordResetRequest.email()), HttpStatus.OK);
    }

    @GetMapping("/check-token")
    public ResponseEntity<Boolean> checkPasswordResetToken(@RequestParam("token") String token) {
        return new ResponseEntity<>(passwordChangeService.checkToken(token), HttpStatus.OK);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(
            @Valid @RequestBody ResetPasswordRequest resetPasswordRequest
    ) {
        return new ResponseEntity<>(passwordChangeService.resetPassword(resetPasswordRequest), HttpStatus.OK);
    }

}
