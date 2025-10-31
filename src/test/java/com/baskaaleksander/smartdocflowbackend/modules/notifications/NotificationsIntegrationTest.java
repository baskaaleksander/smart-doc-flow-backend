package com.baskaaleksander.smartdocflowbackend.modules.notifications;

import com.baskaaleksander.smartdocflowbackend.modules.notifications.domain.model.NotificationType;
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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class NotificationsIntegrationTest extends IntegrationTestBase {

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

        dataUtils.createNotificationForUser(
                "user",
                false,
                "Doc A assigned to you",
                NotificationType.DOCUMENT_IN_REVIEW
        );
        dataUtils.createNotificationForUser(
                "user",
                true,
                "Doc B approved",
                NotificationType.DOCUMENT_REVIEWED
        );

        dataUtils.createNotificationForUser(
                "reviewer",
                false,
                "Please review Doc C",
                NotificationType.DOCUMENT_IN_REVIEW
        );
    }

    @Test
    @DisplayName("GET /notifications returns notifications for logged-in user")
    void getNotificationsReturnsUserNotifications() throws Exception {
        String token = authUtils.loginAndGetAccessToken("user", "User#12345");

        mockMvc.perform(get("/notifications")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                // PagingResult shape
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].username").value("user"))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10));
    }

    @Test
    @DisplayName("GET /notifications?read=false filters only unread notifications")
    void getNotificationsCanFilterByReadFlag() throws Exception {
        String token = authUtils.loginAndGetAccessToken("user", "User#12345");

        mockMvc.perform(get("/notifications")
                        .header("Authorization", "Bearer " + token)
                        .param("read", "false")
                        .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].read").value(false))
                .andExpect(jsonPath("$.content[0].username").value("user"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /notifications without token returns 401")
    void getNotificationsUnauthorizedWithoutToken() throws Exception {
        mockMvc.perform(get("/notifications")
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /notifications/unread-count returns only unread count for this user")
    void getUnreadNotificationsCountReturnsOnlyUnread() throws Exception {
        String token = authUtils.loginAndGetAccessToken("user", "User#12345");

        mockMvc.perform(get("/notifications/unread-count")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(content().string("1"));
    }


    @Test
    @DisplayName("PATCH /notifications with read=true marks all as read for this user")
    void markAllAsReadMarksAllForUser() throws Exception {
        String token = authUtils.loginAndGetAccessToken("reviewer", "Reviewer#12345");

        long beforeUnread = dataUtils.countUnreadForUser("reviewer");
        assertThat(beforeUnread).isEqualTo(1L);

        String body = """
                {
                  "read": true
                }
                """;

        mockMvc.perform(patch("/notifications")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                )
                .andExpect(status().isOk())
                .andExpect(content().string("1"));

        long afterUnread = dataUtils.countUnreadForUser("reviewer");
        assertThat(afterUnread).isEqualTo(0L);
    }

    @Test
    @DisplayName("PATCH /notifications with read=false returns 0 and does nothing")
    void markAllAsReadWithFalseDoesNothing() throws Exception {
        String token = authUtils.loginAndGetAccessToken("user", "User#12345");

        long beforeUnread = dataUtils.countUnreadForUser("user");
        assertThat(beforeUnread).isEqualTo(1L);

        String body = """
                {
                  "read": false
                }
                """;

        mockMvc.perform(patch("/notifications")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                )
                .andExpect(status().isOk())
                .andExpect(content().string("0"));

        long afterUnread = dataUtils.countUnreadForUser("user");
        assertThat(afterUnread).isEqualTo(1L);
    }
}
