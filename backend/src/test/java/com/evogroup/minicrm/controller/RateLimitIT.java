package com.evogroup.minicrm.controller;

import com.evogroup.minicrm.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Own Spring context (app.rate-limit.enabled=true overrides the integration-test profile default
 * of false) — deliberately not sharing the cached context used by the rest of the IT suite, whose
 * login-helper calls would otherwise exhaust this bucket partway through an unrelated test.
 */
@TestPropertySource(properties = "app.rate-limit.enabled=true")
class RateLimitIT extends AbstractIntegrationTest {

    @Test
    void sixthLoginAttempt_returns429WithRetryAfter() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "username", "nobody",
                "password", "wrong-password"));

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"));
    }
}
