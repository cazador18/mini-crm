package com.evogroup.minicrm;

import com.evogroup.minicrm.model.User;
import com.evogroup.minicrm.model.UserRole;
import com.evogroup.minicrm.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration-test")
public abstract class AbstractIntegrationTest {

    protected static final PostgreSQLContainer<?> POSTGRES;

    static {
        POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void overrideDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",                POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username",           POSTGRES::getUsername);
        registry.add("spring.datasource.password",           POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @Autowired protected MockMvc mockMvc;
    @Autowired protected ObjectMapper objectMapper;
    @Autowired protected UserRepository userRepository;
    @Autowired protected PasswordEncoder passwordEncoder;

    // ── auth helpers ─────────────────────────────────────────────────────────

    protected String loginAs(String username, String password) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("username", username, "password", password));
        String content = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(content).get("token").asText();
    }

    /** admin/admin123 — seeded by Flyway V4__seed_admin.sql. */
    protected String adminToken() throws Exception {
        return loginAs("admin", "admin123");
    }

    /** Self-register always yields role MANAGER — see AuthServiceImpl. Unique username per call. */
    protected String registerManagerAndLogin() throws Exception {
        String username = "manager-" + System.nanoTime();
        String body = objectMapper.writeValueAsString(Map.of(
                "username", username,
                "email", username + "@example.com",
                "password", "password123"));
        String content = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(content).get("token").asText();
    }

    /** No self-service path to VIEWER — inserted directly, same as a real deployment would need. */
    protected String createViewerAndLogin() throws Exception {
        String username = "viewer-" + System.nanoTime();
        User viewer = new User();
        viewer.setUsername(username);
        viewer.setEmail(username + "@example.com");
        viewer.setPasswordHash(passwordEncoder.encode("password123"));
        viewer.setRole(UserRole.VIEWER);
        viewer.setEnabled(true);
        userRepository.save(viewer);
        return loginAs(username, "password123");
    }
}
