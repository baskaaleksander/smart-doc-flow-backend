package com.baskaaleksander.smartdocflowbackend.modules.auth;

import com.baskaaleksander.smartdocflowbackend.modules.auth.adapters.persistence.entity.PasswordResetTokenEntity;
import com.baskaaleksander.smartdocflowbackend.modules.auth.adapters.persistence.spring.SpringDataPasswordResetTokenRepository;
import com.baskaaleksander.smartdocflowbackend.modules.testsupport.IntegrationTestBase;
import com.baskaaleksander.smartdocflowbackend.modules.testsupport.TestDataSeeder;
import com.baskaaleksander.smartdocflowbackend.modules.users.adapters.persistence.entity.UserEntity;
import com.baskaaleksander.smartdocflowbackend.modules.users.adapters.persistence.spring.SpringDataUserRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AuthIntegrationTest extends IntegrationTestBase {

    @Autowired
    private TestDataSeeder.TestSeeder seeder;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SpringDataUserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private SpringDataPasswordResetTokenRepository resetTokenRepository;

    @BeforeAll
    void seed() {
        seeder.seedAccountsIfNotExists();
    }

    @Test
    @DisplayName("ADMIN can register a new user via /auth/register")
    void adminCanRegisterUser() throws Exception {
        String adminToken = loginAndGetAccessToken("admin", "Admin#12345");

        String uid = uniqueId();
        String email = "newuser_" + uid + "@example.com";
        String username = "newuser_" + uid;

        String newUserBody = """
                {
                    "email": "%s",
                    "username": "%s",
                    "roles": ["ROLE_USER"]
                }
                """.formatted(email, username);

        mockMvc.perform(post("/auth/register")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newUserBody))
                .andExpect(status().isCreated());

        UserEntity saved = userRepository.findUserByUsernameWithRoles(username).orElseThrow();
        assertThat(saved.getEmail()).isEqualTo(email);
        assertThat(saved.getUsername()).isEqualTo(username);
    }

    @Test
    @DisplayName("Normal USER cannot register a new user via /auth/register")
    void unauthorizedUserCantRegisterUser() throws Exception {
        String userToken = loginAndGetAccessToken("user", "User#12345");

        String uid = uniqueId();
        String email = "shouldFail_" + uid + "@example.com";
        String username = "shouldFail_" + uid;

        String body = """
                {
                    "email": "%s",
                    "username": "%s",
                    "roles": ["ROLE_USER"]
                }
                """.formatted(email, username);

        mockMvc.perform(post("/auth/register")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Register user fails with invalid payload (missing email)")
    void registerUserValidationError() throws Exception {
        String adminAccessToken = loginAndGetAccessToken("admin", "Admin#12345");

        String invalidBody = """
                {
                    "username": "nouseremail",
                    "roles": ["ROLE_USER"]
                }
                """;

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .content(invalidBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("User can login with valid credentials")
    void userCanLogin() throws Exception {

        String loginBody = """
                {
                    "username": "user",
                    "password": "User#12345"
                }
                """;

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("User cant login with invalid credentials")
    void userCantLoginWithInvalidCredentials() throws Exception {

        String loginBody = """
                {
                    "username": "user",
                    "password": "Admin#12345"
                }
                """;

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /auth/me returns current user data when authenticated")
    void getMeReturnsCurrentUser() throws Exception {
        String token = loginAndGetAccessToken("user", "User#12345");

        mockMvc.perform(get("/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("user@smartdocflow.local"));
    }

    @Test
    @DisplayName("GET /auth/me returns 401 when no token provided")
    void getMeUnauthorizedWithoutToken() throws Exception {
        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /auth/refresh issues new access/refresh tokens when valid refresh cookie is present")
    void refreshReturnsNewTokens() throws Exception {
        String loginBody = """
                {
                    "username": "user",
                    "password": "User#12345"
                }
                """;

        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andReturn();

        String refreshCookie = loginResult.getResponse().getHeader("Set-Cookie");

        MvcResult refreshResult = mockMvc.perform(get("/auth/refresh")
                        .header("Cookie", refreshCookie))
                .andExpect(status().isOk())
                .andReturn();

        String newAccessToken = refreshResult.getResponse().getContentAsString();
        assertThat(newAccessToken).isNotBlank();

        String newRefreshCookie = refreshResult.getResponse().getHeader("Set-Cookie");
        assertThat(newRefreshCookie).isNotBlank();
        assertThat(newRefreshCookie).isNotEqualTo(refreshCookie);
    }

    @Test
    @DisplayName("Request password reset creates in database password reset token")
    void passwordResetTokenIsCreated() throws Exception {

        String email = "user@smartdocflow.local";

        String passwordRequestBody = """
                {
                    "email": "%s"
                }
                """.formatted(email);

        mockMvc.perform(post("/auth/request-password-reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(passwordRequestBody))
                .andExpect(status().isOk());

        PasswordResetTokenEntity token = resetTokenRepository.findByUserEmailAndRevokedFalse(email).orElseThrow();

        assertThat(token).isNotNull();
    }

    @Test
    @DisplayName("Reset password changes password according to user's preference")
    void resetPasswordChangesPassword() throws Exception {

        String email = "user@smartdocflow.local";
        String password = "Password@12345";

        PasswordResetTokenEntity token = resetTokenRepository.findByUserEmailAndRevokedFalse(email).orElseThrow();

        String resetPasswordBody = """
                {
                    "token": "%s",
                    "newPassword": "%s"
                }
                """.formatted(token.getToken(), password);

        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resetPasswordBody))
                .andExpect(status().isOk());

        UserEntity user = userRepository.findByEmail(email).orElseThrow();

        assertThat(passwordEncoder.matches(password, user.getPassword())).isTrue();
    }

    @Test
    @DisplayName("Update password updates password with valid credentials")
    void updatePasswordUpdatesPassword() throws Exception {
        String token = loginAndGetAccessToken("reviewer", "Reviewer#12345");
        String newPassword = "NewPassword#12345";

        String updatePasswordBody = """
                {
                    "oldPassword": "Reviewer#12345",
                    "newPassword": "%s"
                }
                """.formatted(newPassword);

        mockMvc.perform(put("/auth/update-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(updatePasswordBody)
                .header("Authorization", "Bearer " + token)
        ).andExpect(status().isOk());

        UserEntity user = userRepository.findByEmail("reviewer@smartdocflow.local").orElseThrow();

        assertThat(passwordEncoder.matches(newPassword, user.getPassword())).isTrue();
    }

    private String uniqueId() {
        return String.valueOf(System.nanoTime());
    }

    private TestUser createIsolatedUser(String rawPassword) throws Exception {
        String adminToken = loginAndGetAccessToken("admin", "Admin#12345");

        String uid = uniqueId();
        String email = "testuser_" + uid + "@example.com";
        String username = "testuser_" + uid;

        String createUserBody = """
                {
                    "email": "%s",
                    "username": "%s",
                    "roles": ["ROLE_USER"]
                }
                """.formatted(email, username);

        mockMvc.perform(post("/auth/register")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createUserBody))
                .andExpect(status().isCreated());

        UserEntity entity = userRepository.findByEmail(email).orElseThrow();
        entity.setPassword(passwordEncoder.encode(rawPassword));
        userRepository.save(entity);

        TestUser tu = new TestUser();
        tu.email = email;
        tu.username = username;
        tu.rawPassword = rawPassword;
        return tu;
    }

    private String loginAndGetAccessToken(String username, String password) throws Exception {
        LoginResult login = loginFull(username, password);
        return login.accessToken;
    }

    private LoginResult loginFull(String username, String password) throws Exception {
        String body = """
                {
                    "username": "%s",
                    "password": "%s"
                }
                """.formatted(username, password);

        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();

        String accessToken = result.getResponse().getContentAsString();
        Cookie[] cookies = result.getResponse().getCookies();
        Cookie refreshCookie = null;
        if (cookies != null) {
            for (Cookie c : cookies) {
                if (refreshCookie == null) {
                    refreshCookie = c;
                }
            }
        }

        LoginResult lr = new LoginResult();
        lr.accessToken = accessToken;
        lr.refreshCookie = refreshCookie;
        return lr;
    }

    private static class TestUser {
        String email;
        String username;
        String rawPassword;
    }

    private static class LoginResult {
        String accessToken;
        Cookie refreshCookie;
    }
}
