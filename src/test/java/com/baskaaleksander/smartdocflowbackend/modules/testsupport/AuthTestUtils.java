package com.baskaaleksander.smartdocflowbackend.modules.testsupport;

import com.baskaaleksander.smartdocflowbackend.modules.testsupport.model.LoginResult;
import jakarta.servlet.http.Cookie;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Component
public class AuthTestUtils {

    @Autowired
    private MockMvc mockMvc;

    public String loginAndGetAccessToken(String username, String password) throws Exception {
        LoginResult login = loginFull(username, password);
        return login.getAccessToken();
    }

    public LoginResult loginFull(String username, String password) throws Exception {
        String body = """
                {
                  "username": "%s",
                  "password": "%s"
                }
                """.formatted(username, password);

        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();

        String accessToken = result.getResponse().getContentAsString();

        Cookie[] cookies = result.getResponse().getCookies();
        Cookie refreshCookie = null;
        if (cookies != null) {
            for (Cookie c : cookies) {
                if (refreshCookie == null) {
                    refreshCookie = c;
                }
            }
        }

        LoginResult lr = new LoginResult();
        lr.setAccessToken(accessToken);
        lr.setRefreshCookie(refreshCookie);
        return lr;
    }
}
