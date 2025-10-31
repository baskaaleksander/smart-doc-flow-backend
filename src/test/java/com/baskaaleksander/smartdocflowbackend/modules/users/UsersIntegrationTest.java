package com.baskaaleksander.smartdocflowbackend.modules.users;

import com.baskaaleksander.smartdocflowbackend.modules.testsupport.AuthTestUtils;
import com.baskaaleksander.smartdocflowbackend.modules.testsupport.IntegrationTestBase;
import com.baskaaleksander.smartdocflowbackend.modules.testsupport.TestDataSeeder;
import com.baskaaleksander.smartdocflowbackend.modules.testsupport.TestDataUtils;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;


@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class UsersIntegrationTest extends IntegrationTestBase {

    @Autowired
    private TestDataSeeder.TestSeeder seeder;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SpringDataUserRepository userRepository;

    @Autowired
    private AuthTestUtils authUtils;

    @Autowired
    private TestDataUtils dataUtils;

    @BeforeAll
    void seed() {
        seeder.seedAccountsIfNotExists();
    }

    @Test
    @DisplayName("ADMIN can get paginated list of all users")
    void adminCanGetAllUsers() throws Exception {

        String accessToken = authUtils.loginAndGetAccessToken("admin", "Admin#12345");

        mockMvc.perform(get("/users/")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(3))
                .andExpect(jsonPath("$.content[?(@.username=='admin')]").exists())
                .andExpect(jsonPath("$.content[?(@.username=='user')]").exists())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.last").value(true))
                .andExpect(jsonPath("$.next").value(false));
    }

    @Test
    @DisplayName("Normal USER cannot access list of all users")
    void userCannotGetAllUsers() throws Exception {
        String accessToken = authUtils.loginAndGetAccessToken("user", "User#12345");

        mockMvc.perform(get("/users/")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + accessToken)
        ).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ADMIN can get user statistics")
    void adminCanGetUserStats() throws Exception {

        String accessToken = authUtils.loginAndGetAccessToken("admin", "Admin#12345");

        mockMvc.perform(get("/users/stats")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.active").value(3))
                .andExpect(jsonPath("$.adminsReviewers").value(2));
    }

    @Test
    @DisplayName("Normal USER cannot get user statistics")
    void userCannotGetUserStats() throws Exception {

        String accessToken = authUtils.loginAndGetAccessToken("user", "User#12345");

        mockMvc.perform(get("/users/stats")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + accessToken)
        ).andExpect(status().isForbidden());
    }

}
