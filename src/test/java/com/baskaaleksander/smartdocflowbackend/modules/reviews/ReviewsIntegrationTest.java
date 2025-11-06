package com.baskaaleksander.smartdocflowbackend.modules.reviews;

import com.baskaaleksander.smartdocflowbackend.modules.reviews.domain.model.ReviewStatus;
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

import java.util.UUID;

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

    @Autowired
    private SpringDataUserRepository userRepository;

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


    @Test
    @DisplayName("Normal USER cannot list reviews")
    void normalUserCannotListReviews() throws Exception {
        String userToken = authUtils.loginAndGetAccessToken("user", "User#12345");

        mockMvc.perform(get("/reviews")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("REVIEWER can get single review by id")
    void reviewerCanGetReviewById() throws Exception {
        String reviewerToken = authUtils.loginAndGetAccessToken("reviewer", "Reviewer#12345");

        UUID reviewId = dataUtils.getAnyExistingReviewId();

        mockMvc.perform(get("/reviews/{reviewId}", reviewId)
                        .header("Authorization", "Bearer " + reviewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(reviewId.toString()))
                .andExpect(jsonPath("$.status").exists());
    }

    @Test
    @DisplayName("Normal USER cannot get review by id")
    void normalUserCannotGetReviewById() throws Exception {
        String userToken = authUtils.loginAndGetAccessToken("user", "User#12345");
        UUID reviewId = dataUtils.getAnyExistingReviewId();

        mockMvc.perform(get("/reviews/{reviewId}", reviewId)
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("REVIEWER can get review events list for a review")
    void reviewerCanGetReviewEvents() throws Exception {
        String reviewerToken = authUtils.loginAndGetAccessToken("reviewer", "Reviewer#12345");
        UUID reviewId = dataUtils.getAnyExistingReviewId();

        mockMvc.perform(get("/reviews/{reviewId}/events", reviewId)
                        .header("Authorization", "Bearer " + reviewerToken)
                        .param("page", "0")
                        .param("size", "10")
                        .param("eventType", "ALL")
                        .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10));
    }


    @Test
    @DisplayName("Normal USER can get his review events list")
    void normalUserCannotGetReviewEvents() throws Exception {
        String userToken = authUtils.loginAndGetAccessToken("user", "User#12345");
        UUID reviewId = dataUtils.createPendingReviewForDocumentOwnedByUser();

        mockMvc.perform(get("/reviews/{reviewId}/events", reviewId)
                        .header("Authorization", "Bearer " + userToken)
                        .param("eventType", "ALL")
                        .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("REVIEWER can get single review event by id")
    void reviewerCanGetSingleReviewEvent() throws Exception {
        String reviewerToken = authUtils.loginAndGetAccessToken("reviewer", "Reviewer#12345");

        UUID eventId = dataUtils.getAnyExistingReviewEventId();

        mockMvc.perform(get("/reviews/event/{eventId}", eventId)
                        .header("Authorization", "Bearer " + reviewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(eventId.toString()))
                .andExpect(jsonPath("$.eventType").exists());
    }

    @Test
    @DisplayName("Normal USER cannot get single review event by id")
    void normalUserCannotGetSingleReviewEvent() throws Exception {
        String userToken = authUtils.loginAndGetAccessToken("user", "User#12345");

        UUID eventId = dataUtils.getAnyExistingReviewEventId();

        mockMvc.perform(get("/reviews/event/{eventId}", eventId)
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("REVIEWER can claim a PENDING review (status -> IN_PROGRESS)")
    void reviewerCanClaimPendingReview() throws Exception {
        String reviewerToken = authUtils.loginAndGetAccessToken("reviewer", "Reviewer#12345");

        UUID reviewId = dataUtils.createPendingReviewForDocumentOwnedByUser();

        String body = """
                {
                    "status": "IN_PROGRESS",
                    "comment": null
                }
                """;

        mockMvc.perform(patch("/reviews/{reviewId}", reviewId)
                        .header("Authorization", "Bearer " + reviewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(reviewId.toString()))
                .andExpect(jsonPath("$.status").value(ReviewStatus.IN_PROGRESS.name()));
    }

    @Test
    @DisplayName("REVIEWER who owns an IN_PROGRESS review can release it back to PENDING")
    void reviewerCanReleaseOwnInProgressReview() throws Exception {
        String reviewerToken = authUtils.loginAndGetAccessToken("reviewer", "Reviewer#12345");

        UUID reviewerId = userRepository.findByUsername("reviewer").orElseThrow().getId();

        UUID reviewId = dataUtils.createPendingReviewForDocumentOwnedByUser();
        dataUtils.forceAssignReviewToReviewer(reviewId, reviewerId);

        String body = """
                {
                    "status": "PENDING",
                    "comment": null
                }
                """;

        mockMvc.perform(patch("/reviews/{reviewId}", reviewId)
                        .header("Authorization", "Bearer " + reviewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(ReviewStatus.PENDING.name()));
    }

    @Test
    @DisplayName("Different reviewer cannot release review they do not own")
    void otherReviewerCannotReleaseForeignReview() throws Exception {
        UUID reviewerId = userRepository.findByUsername("reviewer").orElseThrow().getId();
        UUID reviewId = dataUtils.createPendingReviewForDocumentOwnedByUser();
        dataUtils.forceAssignReviewToReviewer(reviewId, reviewerId);

        String otherReviewerToken = authUtils.loginAndGetAccessToken("admin", "Admin#12345");

        String body = """
                {
                    "status": "PENDING",
                    "comment": null
                }
                """;

        mockMvc.perform(patch("/reviews/{reviewId}", reviewId)
                        .header("Authorization", "Bearer " + otherReviewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }


    @Test
    @DisplayName("Reviewer can approve own IN_PROGRESS review (-> APPROVED) when sending comment")
    void reviewerCanApproveOwnReview() throws Exception {
        String reviewerToken = authUtils.loginAndGetAccessToken("reviewer", "Reviewer#12345");
        UUID reviewerId = userRepository.findByUsername("reviewer").orElseThrow().getId();
        UUID reviewId = dataUtils.createPendingReviewForDocumentOwnedByUser();
        dataUtils.forceAssignReviewToReviewer(reviewId, reviewerId);

        String body = """
                {
                    "status": "APPROVED",
                    "comment": "Looks good"
                }
                """;

        mockMvc.perform(patch("/reviews/{reviewId}", reviewId)
                        .header("Authorization", "Bearer " + reviewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(ReviewStatus.APPROVED.name()))
                .andExpect(jsonPath("$.comment").value("Looks good"));
    }

    @Test
    @DisplayName("Reviewer can reject own IN_PROGRESS review (-> REJECTED) with comment")
    void reviewerCanRejectOwnReview() throws Exception {
        String reviewerToken = authUtils.loginAndGetAccessToken("reviewer", "Reviewer#12345");
        UUID reviewerId = userRepository.findByUsername("reviewer").orElseThrow().getId();
        UUID reviewId = dataUtils.createPendingReviewForDocumentOwnedByUser();
        dataUtils.forceAssignReviewToReviewer(reviewId, reviewerId);

        String body = """
                {
                    "status": "REJECTED",
                    "comment": "This is not acceptable"
                }
                """;

        mockMvc.perform(patch("/reviews/{reviewId}", reviewId)
                        .header("Authorization", "Bearer " + reviewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(ReviewStatus.REJECTED.name()))
                .andExpect(jsonPath("$.comment").value("This is not acceptable"));
    }

    @Test
    @DisplayName("Other reviewer cannot approve review they do not own")
    void otherReviewerCannotApproveForeignReview() throws Exception {
        String ownerToken = authUtils.loginAndGetAccessToken("reviewer", "Reviewer#12345");
        UUID reviewerId = userRepository.findByUsername("reviewer").orElseThrow().getId();
        UUID reviewId = dataUtils.createPendingReviewForDocumentOwnedByUser();
        dataUtils.forceAssignReviewToReviewer(reviewId, reviewerId);

        String otherReviewerToken = authUtils.loginAndGetAccessToken("admin", "Admin#12345");

        String body = """
                {
                    "status": "APPROVED",
                    "comment": "ok"
                }
                """;

        mockMvc.perform(patch("/reviews/{reviewId}", reviewId)
                        .header("Authorization", "Bearer " + otherReviewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Reviewer can add comment event to a review")
    void reviewerCanCommentOnReview() throws Exception {
        String reviewerToken = authUtils.loginAndGetAccessToken("reviewer", "Reviewer#12345");
        UUID reviewerId = userRepository.findByUsername("reviewer").orElseThrow().getId();
        UUID reviewId = dataUtils.createPendingReviewForDocumentOwnedByUser();
        dataUtils.forceAssignReviewToReviewer(reviewId, reviewerId);

        String body = """
                {
                    "comment": "Please update section 2.1"
                }
                """;

        mockMvc.perform(post("/reviews/{reviewId}/comment", reviewId)
                        .header("Authorization", "Bearer " + reviewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comment").value("Please update section 2.1"))
                .andExpect(jsonPath("$.reviewId").value(reviewId.toString()));
    }

    @Test
    @DisplayName("Normal USER cannot comment on review")
    void normalUserCannotCommentOnReview() throws Exception {
        String userToken = authUtils.loginAndGetAccessToken("user", "User#12345");
        UUID reviewerId = userRepository.findByUsername("reviewer").orElseThrow().getId();
        UUID reviewId = dataUtils.createPendingReviewForDocumentOwnedByUser();
        dataUtils.forceAssignReviewToReviewer(reviewId, reviewerId);

        String body = """
                {
                    "comment": "trying to inject comment"
                }
                """;

        mockMvc.perform(post("/reviews/{reviewId}/comment", reviewId)
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }
}
