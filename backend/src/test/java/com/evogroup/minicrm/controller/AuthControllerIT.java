package com.evogroup.minicrm.controller;

import com.evogroup.minicrm.AbstractIntegrationTest;
import com.evogroup.minicrm.repository.ClientRepository;
import com.evogroup.minicrm.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthControllerIT extends AbstractIntegrationTest {

    @Autowired ClientRepository clientRepo;
    @Autowired TaskRepository taskRepo;

    @BeforeEach
    void cleanUp() {
        taskRepo.deleteAll();
        clientRepo.deleteAll();
    }

    @Test
    void register_returns201WithToken() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "username", "newuser",
                "email", "newuser@example.com",
                "password", "password123"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.username", is("newuser")))
                .andExpect(jsonPath("$.role", is("MANAGER")));
    }

    @Test
    void register_duplicateUsername_returns409() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "username", "dupeuser",
                "email", "dupeuser@example.com",
                "password", "password123"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void register_missingPassword_returns400() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "username", "incomplete",
                "email", "incomplete@example.com"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_returns200WithToken() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "username", "admin",
                "password", "admin123"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.username", is("admin")))
                .andExpect(jsonPath("$.role", is("ADMIN")));
    }

    @Test
    void login_wrongPassword_returns401() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "username", "admin",
                "password", "wrong-password"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_unknownUsername_returns401() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "username", "nobody-registered",
                "password", "whatever123"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_thenAccessProtectedResource_succeeds() throws Exception {
        String loginBody = objectMapper.writeValueAsString(Map.of(
                "username", "admin",
                "password", "admin123"));

        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String token = objectMapper.readTree(loginResponse).get("token").asText();

        mockMvc.perform(get("/api/clients").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", notNullValue()));
    }
}
