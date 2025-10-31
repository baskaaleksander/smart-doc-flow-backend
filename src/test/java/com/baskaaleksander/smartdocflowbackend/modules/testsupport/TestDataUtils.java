package com.baskaaleksander.smartdocflowbackend.modules.testsupport;

import com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.persistence.entity.DocumentEntity;
import com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.persistence.spring.SpringDataDocumentRepository;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model.DocumentStatus;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.adapters.persistence.entity.ReviewEntity;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.adapters.persistence.spring.SpringDataReviewRepository;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.domain.model.ReviewStatus;
import com.baskaaleksander.smartdocflowbackend.modules.testsupport.model.TestUser;
import com.baskaaleksander.smartdocflowbackend.modules.users.adapters.persistence.entity.UserEntity;
import com.baskaaleksander.smartdocflowbackend.modules.users.adapters.persistence.spring.SpringDataUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

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
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

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
        DocumentEntity doc = new DocumentEntity();
        ReviewEntity review = new ReviewEntity();

        review.setStatus(ReviewStatus.PENDING);
        review.setDocument(doc);

        doc.setReview(review);
        doc.setId(UUID.randomUUID());
        doc.setFilename("testfile");
        doc.setMime("application/pdf");
        doc.setSize(1.0);
        doc.setStorageKey("test-key");
        doc.setPageSize(0);
        doc.setStatus(DocumentStatus.PROCESSED);
        doc.setOwner(userRepository.getReferenceById(userId));

        DocumentEntity saved = documentRepository.save(doc);

        return saved.getId();
    }

    private String uniqueId() {
        return String.valueOf(System.nanoTime());
    }
}
