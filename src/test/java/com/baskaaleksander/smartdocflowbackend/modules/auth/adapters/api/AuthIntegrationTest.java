package com.baskaaleksander.smartdocflowbackend.modules.auth.adapters.api;

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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
