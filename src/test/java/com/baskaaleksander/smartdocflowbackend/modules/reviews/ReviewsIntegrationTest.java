package com.baskaaleksander.smartdocflowbackend.modules.reviews;

import com.baskaaleksander.smartdocflowbackend.modules.testsupport.AuthTestUtils;
import com.baskaaleksander.smartdocflowbackend.modules.testsupport.IntegrationTestBase;
import com.baskaaleksander.smartdocflowbackend.modules.testsupport.TestDataSeeder;
import com.baskaaleksander.smartdocflowbackend.modules.testsupport.TestDataUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ReviewsIntegrationTest extends IntegrationTestBase {
    @Autowired
    private TestDataSeeder.TestSeeder seeder;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthTestUtils authUtils;

    @Autowired
    private TestDataUtils dataUtils;

    @BeforeAll
    void seed() {
        seeder.seedAccountsIfNotExists();
        dataUtils.seedBaseDataForReviews();
    }

    @Test
    @DisplayName("REVIEWER can list all reviews")
    void reviewerCanListAllReviews() throws Exception {
        String reviewerToken = authUtils.loginAndGetAccessToken("reviewer", "Reviewer#12345");

        mockMvc.perform(get("/reviews")
                        .header("Authorization", "Bearer " + reviewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10));
    }

    @Test
    @DisplayName("REVIEWER can filter reviews by status=PENDING")
    void reviewerCanFilterByStatus() throws Exception {
        String reviewerToken = authUtils.loginAndGetAccessToken("reviewer", "Reviewer#12345");

        mockMvc.perform(get("/reviews")
                        .param("status", "PENDING")
                        .header("Authorization", "Bearer " + reviewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }
}
