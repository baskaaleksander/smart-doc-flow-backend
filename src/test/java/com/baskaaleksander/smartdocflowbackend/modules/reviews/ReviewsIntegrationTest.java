package com.baskaaleksander.smartdocflowbackend.modules.reviews;

import com.baskaaleksander.smartdocflowbackend.modules.testsupport.AuthTestUtils;
import com.baskaaleksander.smartdocflowbackend.modules.testsupport.IntegrationTestBase;
import com.baskaaleksander.smartdocflowbackend.modules.testsupport.TestDataSeeder;
import com.baskaaleksander.smartdocflowbackend.modules.testsupport.TestDataUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

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
}
