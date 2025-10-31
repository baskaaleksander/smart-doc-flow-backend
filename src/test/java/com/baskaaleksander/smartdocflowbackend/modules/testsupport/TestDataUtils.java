package com.baskaaleksander.smartdocflowbackend.modules.testsupport;

import com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.persistence.entity.DocumentEntity;
import com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.persistence.spring.SpringDataDocumentRepository;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model.DocumentStatus;
import com.baskaaleksander.smartdocflowbackend.modules.notifications.adapters.persistence.entity.NotificationEntity;
import com.baskaaleksander.smartdocflowbackend.modules.notifications.adapters.persistence.spring.SpringDataNotificationRepository;
import com.baskaaleksander.smartdocflowbackend.modules.notifications.domain.model.NotificationType;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.adapters.persistence.entity.ReviewEntity;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.adapters.persistence.entity.ReviewEventEntity;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.adapters.persistence.spring.SpringDataReviewEventRepository;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.adapters.persistence.spring.SpringDataReviewRepository;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.domain.model.ReviewEventType;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.domain.model.ReviewStatus;
import com.baskaaleksander.smartdocflowbackend.modules.testsupport.model.ReviewWithEvents;
import com.baskaaleksander.smartdocflowbackend.modules.testsupport.model.TestUser;
import com.baskaaleksander.smartdocflowbackend.modules.users.adapters.persistence.entity.UserEntity;
import com.baskaaleksander.smartdocflowbackend.modules.users.adapters.persistence.spring.SpringDataUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Component
public class TestDataUtils {

    @Autowired
    private AuthTestUtils authUtils;

    @Autowired
    private SpringDataUserRepository userRepository;

    @Autowired
    private SpringDataDocumentRepository documentRepository;

    @Autowired
    private SpringDataReviewRepository reviewRepository;

    @Autowired
    private SpringDataReviewEventRepository reviewEventRepository;

    @Autowired
    private SpringDataNotificationRepository notificationRepository;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String uniqueId() {
        return String.valueOf(System.nanoTime());
    }

    @Transactional
    public TestUser createIsolatedUser(String rawPassword) throws Exception {
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
        tu.setEmail(email);
        tu.setUsername(username);
        tu.setRawPassword(rawPassword);
        return tu;
    }

    @Transactional
    public UUID uploadDocForUser(UUID userId) {
        UserEntity owner = userRepository.findById(userId).orElseThrow();

        DocumentEntity doc = new DocumentEntity();
        doc.setId(UUID.randomUUID());
        doc.setFilename("testfile");
        doc.setMime("application/pdf");
        doc.setSize(1.0);
        doc.setStorageKey("test-key");
        doc.setPageSize(0);
        doc.setStatus(DocumentStatus.PROCESSED);
        doc.setOwner(owner);
        documentRepository.save(doc);

        DocumentEntity managedDoc = documentRepository.findById(doc.getId())
                .orElseThrow(() -> new IllegalStateException("Document not persisted"));

        ReviewEntity review = new ReviewEntity();
        review.setStatus(ReviewStatus.PENDING);
        review.setDocument(managedDoc);
        reviewRepository.save(review);

        managedDoc.setReview(review);
        documentRepository.save(managedDoc);

        return managedDoc.getId();
    }

    @Transactional
    public void seedBaseDataForReviews() {
        if (reviewRepository.count() == 0) {
            createReviewWithSomeEvents();
        }
    }

    @Transactional
    public UUID createPendingReviewForDocumentOwnedByUser() {
        UserEntity owner = userRepository.findByUsername("user")
                .orElseThrow(() -> new IllegalStateException("Seed user 'user' not found"));

        DocumentEntity doc = new DocumentEntity();
        doc.setId(UUID.randomUUID());
        doc.setFilename("pending_doc.pdf");
        doc.setMime("application/pdf");
        doc.setSize(1.0);
        doc.setStorageKey("pending-key");
        doc.setPageSize(0);
        doc.setStatus(DocumentStatus.PROCESSED);
        doc.setOwner(owner);
        documentRepository.save(doc);

        DocumentEntity managedDoc = documentRepository.findById(doc.getId())
                .orElseThrow(() -> new IllegalStateException("Document not persisted"));

        ReviewEntity review = new ReviewEntity();
        review.setStatus(ReviewStatus.PENDING);
        review.setDocument(managedDoc);
        reviewRepository.save(review);

        managedDoc.setReview(review);
        documentRepository.save(managedDoc);

        return review.getId();
    }

    @Transactional
    public UUID forceAssignReviewToReviewer(UUID reviewId, UUID reviewerId) {
        ReviewEntity review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalStateException("Review not found: " + reviewId));
        UserEntity reviewer = userRepository.findById(reviewerId)
                .orElseThrow(() -> new IllegalStateException("Reviewer not found: " + reviewerId));

        review.setReviewer(reviewer);
        review.setStatus(ReviewStatus.IN_PROGRESS);
        reviewRepository.save(review);

        DocumentEntity doc = review.getDocument();
        if (doc != null) {
            doc.setStatus(DocumentStatus.IN_REVIEW);
            documentRepository.save(doc);
        }

        return review.getId();
    }

    @Transactional
    public UUID createInProgressReviewForReviewerUser() {
        UserEntity owner = userRepository.findByUsername("user")
                .orElseThrow(() -> new IllegalStateException("Seed user 'user' not found"));
        UserEntity reviewer = userRepository.findByUsername("reviewer")
                .orElseThrow(() -> new IllegalStateException("Seed user 'reviewer' not found"));

        DocumentEntity doc = new DocumentEntity();
        doc.setId(UUID.randomUUID());
        doc.setFilename("in_progress_doc.pdf");
        doc.setMime("application/pdf");
        doc.setSize(1.0);
        doc.setStorageKey("in-progress-key");
        doc.setPageSize(0);
        doc.setStatus(DocumentStatus.IN_REVIEW);
        doc.setOwner(owner);
        documentRepository.save(doc);

        DocumentEntity managedDoc = documentRepository.findById(doc.getId())
                .orElseThrow(() -> new IllegalStateException("Document not persisted"));

        ReviewEntity review = new ReviewEntity();
        review.setStatus(ReviewStatus.IN_PROGRESS);
        review.setDocument(managedDoc);
        review.setReviewer(reviewer);
        reviewRepository.save(review);

        managedDoc.setReview(review);
        documentRepository.save(managedDoc);

        ReviewEventEntity assigned = new ReviewEventEntity();
        assigned.setReview(review);
        assigned.setEventType(ReviewEventType.ASSIGNED);
        assigned.setReviewer(reviewer);
        assigned.setComment(null);
        reviewEventRepository.save(assigned);

        return review.getId();
    }

    @Transactional
    public ReviewWithEvents createReviewWithSomeEvents() {
        UserEntity owner = userRepository.findByUsername("user")
                .orElseThrow(() -> new IllegalStateException("Seed user 'user' not found"));
        UserEntity reviewer = userRepository.findByUsername("reviewer")
                .orElseThrow(() -> new IllegalStateException("Seed user 'reviewer' not found"));

        DocumentEntity doc = new DocumentEntity();
        doc.setId(UUID.randomUUID());
        doc.setFilename("seeded_doc.pdf");
        doc.setMime("application/pdf");
        doc.setSize(1.0);
        doc.setStorageKey("seed-key");
        doc.setPageSize(0);
        doc.setStatus(DocumentStatus.IN_REVIEW);
        doc.setOwner(owner);
        documentRepository.save(doc);

        DocumentEntity managedDoc = documentRepository.findById(doc.getId())
                .orElseThrow(() -> new IllegalStateException("Document not persisted"));

        ReviewEntity review = new ReviewEntity();
        review.setStatus(ReviewStatus.IN_PROGRESS);
        review.setDocument(managedDoc);
        review.setReviewer(reviewer);
        reviewRepository.save(review);

        managedDoc.setReview(review);
        documentRepository.save(managedDoc);

        ReviewEventEntity assigned = new ReviewEventEntity();
        assigned.setReview(review);
        assigned.setEventType(ReviewEventType.ASSIGNED);
        assigned.setReviewer(reviewer);
        assigned.setComment(null);
        reviewEventRepository.save(assigned);

        ReviewEventEntity comment = new ReviewEventEntity();
        comment.setReview(review);
        comment.setEventType(ReviewEventType.COMMENT);
        comment.setReviewer(reviewer);
        comment.setComment("Please fix formatting in section 2.1");
        reviewEventRepository.save(comment);

        return new ReviewWithEvents(review.getId(), comment.getId());
    }

    public UUID getAnyExistingReviewId() {
        Optional<ReviewEntity> any = reviewRepository.findFirst();
        return any
                .map(ReviewEntity::getId)
                .orElseThrow(() -> new IllegalStateException("No review in DB. Call seedBaseDataForReviews() first."));
    }

    public UUID getAnyExistingReviewEventId() {
        Optional<ReviewEventEntity> anyEvent = reviewEventRepository.findFirst();
        return anyEvent
                .map(ReviewEventEntity::getId)
                .orElseThrow(() -> new IllegalStateException("No review event in DB. Call seedBaseDataForReviews() first."));
    }

    @Transactional
    public UUID createNotificationForUser(String username, boolean read, String message, NotificationType type) {
        NotificationEntity n = new NotificationEntity();
        n.setUsername(username);
        n.setMessage(message);
        n.setType(type);
        n.setRead(read);
        return notificationRepository.save(n).getId();
    }

    @Transactional
    public void createSampleNotificationsForUser(String username) {
        createNotificationForUser(username, false, "Doc ABC assigned to you", NotificationType.DOCUMENT_IN_REVIEW);
        createNotificationForUser(username, true, "Doc XYZ approved", NotificationType.DOCUMENT_REVIEWED);
    }

    public long countUnreadForUser(String username) {
        return notificationRepository.getNotificationsCountByUsernameAndRead(username, false);
    }

    public boolean isNotificationRead(UUID id) {
        return notificationRepository.findById(id)
                .orElseThrow()
                .isRead();
    }
}