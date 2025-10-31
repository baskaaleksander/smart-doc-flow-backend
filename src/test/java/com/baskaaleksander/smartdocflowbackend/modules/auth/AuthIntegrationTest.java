package com.baskaaleksander.smartdocflowbackend.modules.auth;

import com.baskaaleksander.smartdocflowbackend.modules.auth.adapters.persistence.entity.PasswordResetTokenEntity;
import com.baskaaleksander.smartdocflowbackend.modules.auth.adapters.persistence.spring.SpringDataPasswordResetTokenRepository;
import com.baskaaleksander.smartdocflowbackend.modules.auth.adapters.persistence.spring.SpringDataRefreshTokenRepository;
import com.baskaaleksander.smartdocflowbackend.modules.testsupport.AuthTestUtils;
import com.baskaaleksander.smartdocflowbackend.modules.testsupport.IntegrationTestBase;
import com.baskaaleksander.smartdocflowbackend.modules.testsupport.TestDataSeeder;
import com.baskaaleksander.smartdocflowbackend.modules.testsupport.model.LoginResult;
import com.baskaaleksander.smartdocflowbackend.modules.users.adapters.persistence.entity.UserEntity;
import com.baskaaleksander.smartdocflowbackend.modules.users.adapters.persistence.spring.SpringDataUserRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AuthIntegrationTest extends IntegrationTestBase {

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

    @Autowired
    private SpringDataRefreshTokenRepository refreshTokenRepository;

    @Autowired
    private AuthTestUtils authUtils;

    @BeforeAll
    void seed() {
        seeder.seedAccountsIfNotExists();
    }

    @AfterAll
    void cleanDatabase() {
        resetTokenRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("POST /auth/login returns 200 for valid credentials")
    void loginSucceedsWithValidCredentials() throws Exception {
        String body = """
                {
                  "username": "user",
                  "password": "User#12345"
                }
                """;

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /auth/login returns 401 for invalid credentials")
    void loginFailsWithInvalidCredentials() throws Exception {
        String body = """
                {
                  "username": "user",
                  "password": "WrongPassword#999"
                }
                """;

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /auth/me returns 200 and current user data when authenticated")
    void meReturnsCurrentUserWhenAuthorized() throws Exception {
        String token = authUtils.loginAndGetAccessToken("user", "User#12345");

        mockMvc.perform(get("/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("user@smartdocflow.local"));
    }

    @Test
    @DisplayName("GET /auth/me returns 401 when no token is provided")
    void meReturnsUnauthorizedWithoutToken() throws Exception {
        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /auth/register returns 201 when called by ADMIN")
    void adminCanRegisterUser() throws Exception {
        String adminToken = authUtils.loginAndGetAccessToken("admin", "Admin#12345");

        String uid = uniqueId();
        String email = "newuser_%s@example.com".formatted(uid);
        String username = "newuser_%s".formatted(uid);

        String requestBody = """
                {
                  "email": "%s",
                  "username": "%s",
                  "roles": ["ROLE_USER"]
                }
                """.formatted(email, username);

        mockMvc.perform(post("/auth/register")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated());

        UserEntity saved = userRepository.findUserByUsernameWithRoles(username).orElseThrow();
        assertThat(saved.getEmail()).isEqualTo(email);
        assertThat(saved.getUsername()).isEqualTo(username);
    }

    @Test
    @DisplayName("POST /auth/register returns 403 when called by normal USER")
    void nonAdminCannotRegisterUser() throws Exception {
        String userToken = authUtils.loginAndGetAccessToken("user", "User#12345");

        String uid = uniqueId();
        String email = "shouldFail_%s@example.com".formatted(uid);
        String username = "shouldFail_%s".formatted(uid);

        String requestBody = """
                {
                  "email": "%s",
                  "username": "%s",
                  "roles": ["ROLE_USER"]
                }
                """.formatted(email, username);

        mockMvc.perform(post("/auth/register")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /auth/register returns 400 for invalid payload (missing email)")
    void registerUserFailsWithInvalidPayload() throws Exception {
        String adminToken = authUtils.loginAndGetAccessToken("admin", "Admin#12345");

        String invalidBody = """
                {
                  "username": "nouseremail",
                  "roles": ["ROLE_USER"]
                }
                """;

        mockMvc.perform(post("/auth/register")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /auth/update-password returns 200 and updates password when oldPassword is correct")
    void updatePasswordSucceedsWithValidOldPassword() throws Exception {
        TestUser testUser = createIsolatedUser("OrigPass#" + uniqueId());

        String accessToken = authUtils.loginAndGetAccessToken(testUser.username, testUser.rawPassword);
        String newPassword = "ChangedPass#" + uniqueId();

        String requestBody = """
                {
                  "oldPassword": "%s",
                  "newPassword": "%s"
                }
                """.formatted(testUser.rawPassword, newPassword);

        mockMvc.perform(put("/auth/update-password")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk());

        UserEntity refreshed = userRepository.findByEmail(testUser.email).orElseThrow();
        assertThat(passwordEncoder.matches(newPassword, refreshed.getPassword())).isTrue();
    }

    @Test
    @DisplayName("PUT /auth/update-password returns 401 and does not update password when oldPassword is wrong")
    void updatePasswordFailsWithWrongOldPassword() throws Exception {
        TestUser testUser = createIsolatedUser("OrigPass#" + uniqueId());

        String accessToken = authUtils.loginAndGetAccessToken(testUser.username, testUser.rawPassword);
        String attemptedNewPassword = "ShouldNotApply#" + uniqueId();

        String requestBody = """
                {
                  "oldPassword": "TotallyWrong#999",
                  "newPassword": "%s"
                }
                """.formatted(attemptedNewPassword);

        mockMvc.perform(put("/auth/update-password")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized());

        UserEntity refreshed = userRepository.findByEmail(testUser.email).orElseThrow();
        assertThat(passwordEncoder.matches(testUser.rawPassword, refreshed.getPassword())).isTrue();
        assertThat(passwordEncoder.matches(attemptedNewPassword, refreshed.getPassword())).isFalse();
    }

    @Test
    @DisplayName("POST /auth/request-password-reset creates a reset token in DB")
    void requestPasswordResetCreatesToken() throws Exception {
        String email = "user@smartdocflow.local";

        String body = """
                {
                  "email": "%s"
                }
                """.formatted(email);

        mockMvc.perform(post("/auth/request-password-reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        PasswordResetTokenEntity token = resetTokenRepository
                .findByUserEmailAndRevokedFalse(email)
                .orElseThrow();

        assertThat(token).isNotNull();
        assertThat(token.getToken()).isNotBlank();
    }

    @Test
    @DisplayName("GET /auth/check-token returns 200 for an active reset token")
    void checkTokenReturnsOkForActiveToken() throws Exception {
        TestUser testUser = createIsolatedUser("Start#" + uniqueId());

        String requestBody = """
                {
                  "email": "%s"
                }
                """.formatted(testUser.email);

        mockMvc.perform(post("/auth/request-password-reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk());

        PasswordResetTokenEntity token = resetTokenRepository
                .findByUserEmailAndRevokedFalse(testUser.email)
                .orElseThrow();

        mockMvc.perform(get("/auth/check-token")
                        .param("token", token.getToken()))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    @DisplayName("POST /auth/reset-password sets a new password using a valid reset token")
    void resetPasswordSetsNewPassword() throws Exception {
        TestUser testUser = createIsolatedUser("Orig#" + uniqueId());

        String requestResetBody = """
                {
                  "email": "%s"
                }
                """.formatted(testUser.email);

        mockMvc.perform(post("/auth/request-password-reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestResetBody))
                .andExpect(status().isOk());

        PasswordResetTokenEntity token = resetTokenRepository
                .findByUserEmailAndRevokedFalse(testUser.email)
                .orElseThrow();

        String newPassword = "NewPassword#" + uniqueId();

        String resetBody = """
                {
                  "token": "%s",
                  "newPassword": "%s"
                }
                """.formatted(token.getToken(), newPassword);

        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resetBody))
                .andExpect(status().isOk());

        UserEntity updated = userRepository.findByEmail(testUser.email).orElseThrow();
        assertThat(passwordEncoder.matches(newPassword, updated.getPassword())).isTrue();
    }

    @Test
    @DisplayName("GET /auth/refresh returns 200, issues new access token and new refresh cookie")
    void refreshIssuesNewTokens() throws Exception {
        LoginResult login = authUtils.loginFull("user", "User#12345");

        MvcResult refreshResult = mockMvc.perform(get("/auth/refresh")
                        .cookie(login.getRefreshCookie()))
                .andExpect(status().isOk())
                .andExpect(cookie().exists(login.getRefreshCookie().getName()))
                .andReturn();

        String newAccessToken = refreshResult.getResponse().getContentAsString();
        assertThat(newAccessToken).isNotBlank();
    }

    @Test
    @DisplayName("POST /auth/logout returns 200 and sets expired refresh cookie")
    void logoutClearsRefreshCookie() throws Exception {
        LoginResult login = authUtils.loginFull("user", "User#12345");

        mockMvc.perform(post("/auth/logout")
                        .cookie(login.getRefreshCookie())
                        .header("Authorization", "Bearer " + login.getAccessToken()))
                .andExpect(status().isOk())
                .andExpect(cookie().exists(login.getRefreshCookie().getName()));
    }

    private String uniqueId() {
        return String.valueOf(System.nanoTime());
    }

    private TestUser createIsolatedUser(String rawPassword) throws Exception {
        String adminToken = authUtils.loginAndGetAccessToken("admin", "Admin#12345");

        String uid = uniqueId();
        String email = "testuser_%s@example.com".formatted(uid);
        String username = "testuser_%s".formatted(uid);

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

    private static class TestUser {
        String email;
        String username;
        String rawPassword;
    }

}