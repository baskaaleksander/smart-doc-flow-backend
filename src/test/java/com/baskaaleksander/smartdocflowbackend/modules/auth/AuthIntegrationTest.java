package com.baskaaleksander.smartdocflowbackend.modules.auth;

import com.baskaaleksander.smartdocflowbackend.modules.auth.adapters.persistence.entity.PasswordResetTokenEntity;
import com.baskaaleksander.smartdocflowbackend.modules.auth.adapters.persistence.spring.SpringDataPasswordResetTokenRepository;
import com.baskaaleksander.smartdocflowbackend.modules.testsupport.IntegrationTestBase;
import com.baskaaleksander.smartdocflowbackend.modules.testsupport.TestDataSeeder;
import com.baskaaleksander.smartdocflowbackend.modules.users.adapters.persistence.entity.UserEntity;
import com.baskaaleksander.smartdocflowbackend.modules.users.adapters.persistence.spring.SpringDataUserRepository;
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
    @DisplayName("ADMIN can create a new user via /api/auth/register")
    void adminCanRegisterUser() throws Exception {

        String adminAccessToken = loginAndGetAccessToken(
                "admin",
                "Admin#12345"
        );

        String newUserBody = """
                {
                    "email": "newuser@example.com",
                    "username": "newuser",
                    "roles": ["ROLE_USER"]
                }
                """;

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .content(newUserBody))
                .andExpect(status().isCreated());

        UserEntity saved = userRepository.findUserByUsernameWithRoles("newuser").orElseThrow();

        assertThat(saved.getEmail()).isEqualTo("newuser@example.com");
        assertThat(saved.getUsername()).isEqualTo("newuser");
    }

    @Test
    @DisplayName("USER can't create a new user via /api/auth/register")
    void unauthorizedUserCantRegisterUser() throws Exception {
        String accessToken = loginAndGetAccessToken(
                "user",
                "User#12345"
        );

        String newUserBody = """
                {
                    "email": "newuser2@example.com",
                    "username": "newuser2",
                    "roles": ["ROLE_USER"]
                }
                """;

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken)
                        .content(newUserBody))
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


    private String loginAndGetAccessToken(String username, String password) throws Exception {
        String requestBody = """
                {
                    "username": "%s",
                    "password": "%s"
                }
                """.formatted(username, password);

        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andReturn();

        String accessToken = result.getResponse().getContentAsString();

        return accessToken;
    }
}
