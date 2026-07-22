package com.evogroup.minicrm.controller;

import com.evogroup.minicrm.AbstractIntegrationTest;
import com.evogroup.minicrm.dto.TaskResponse;
import com.evogroup.minicrm.model.Client;
import com.evogroup.minicrm.repository.ClientRepository;
import com.evogroup.minicrm.repository.TaskRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class TaskControllerIT extends AbstractIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired ClientRepository clientRepo;
    @Autowired TaskRepository taskRepo;

    private Client savedClient;

    @BeforeEach
    void setUp() {
        taskRepo.deleteAll();
        clientRepo.deleteAll();

        Client c = new Client();
        c.setName("Alice");
        c.setEmail("alice@example.com");
        savedClient = clientRepo.save(c);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private TaskResponse createTaskViaApi(String title, String status, Long clientId) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "title", title,
                "status", status,
                "priority", "MEDIUM",
                "clientId", clientId));
        String content = mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readValue(content, TaskResponse.class);
    }

    // ── tests ─────────────────────────────────────────────────────────────────

    @Test
    void createTask_returns201WithBody() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "title", "Fix bug",
                "status", "NEW",
                "priority", "HIGH",
                "clientId", savedClient.getId()));

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.title", is("Fix bug")))
                .andExpect(jsonPath("$.priority", is("HIGH")))
                .andExpect(jsonPath("$.clientId", is(savedClient.getId().intValue())));
    }

    @Test
    void createTask_unknownClientId_returns404() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "title", "Fix bug",
                "status", "NEW",
                "priority", "MEDIUM",
                "clientId", 99999));

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    void createTask_missingStatus_returns400() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "title", "Fix bug",
                "priority", "MEDIUM",
                "clientId", savedClient.getId()));

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createTask_missingPriority_returns400() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "title", "Fix bug",
                "status", "NEW",
                "clientId", savedClient.getId()));

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void findAllTasks_returnsAll() throws Exception {
        createTaskViaApi("Task A", "NEW",  savedClient.getId());
        createTaskViaApi("Task B", "DONE", savedClient.getId());

        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void findAllTasks_filterByStatus() throws Exception {
        createTaskViaApi("New task",  "NEW",  savedClient.getId());
        createTaskViaApi("Done task", "DONE", savedClient.getId());

        mockMvc.perform(get("/api/tasks").param("status", "NEW"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].status", is("NEW")));
    }

    @Test
    void findAllTasks_filterByClientId() throws Exception {
        Client other = new Client();
        other.setName("Bob");
        other.setEmail("bob@example.com");
        Client savedOther = clientRepo.save(other);

        createTaskViaApi("Alice task", "NEW", savedClient.getId());
        createTaskViaApi("Bob task",   "NEW", savedOther.getId());

        mockMvc.perform(get("/api/tasks").param("clientId", savedClient.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].clientId", is(savedClient.getId().intValue())));
    }

    @Test
    void findAllTasks_filterByStatusAndClientId() throws Exception {
        createTaskViaApi("New task",  "NEW",  savedClient.getId());
        createTaskViaApi("Done task", "DONE", savedClient.getId());

        mockMvc.perform(get("/api/tasks")
                        .param("status",   "NEW")
                        .param("clientId", savedClient.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].status", is("NEW")));
    }

    @Test
    void findTaskById_returnsTask() throws Exception {
        TaskResponse created = createTaskViaApi("Fix bug", "NEW", savedClient.getId());

        mockMvc.perform(get("/api/tasks/{id}", created.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(created.getId().intValue())))
                .andExpect(jsonPath("$.title", is("Fix bug")));
    }

    @Test
    void findTaskById_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/tasks/99999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateTask_returnsUpdatedData() throws Exception {
        TaskResponse created = createTaskViaApi("Fix bug", "NEW", savedClient.getId());

        String updateBody = objectMapper.writeValueAsString(Map.of(
                "title",    "Fix bug",
                "status",   "IN_PROGRESS",
                "priority", "HIGH",
                "clientId", savedClient.getId()));

        mockMvc.perform(put("/api/tasks/{id}", created.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("IN_PROGRESS")))
                .andExpect(jsonPath("$.priority", is("HIGH")));
    }

    @Test
    void deleteTask_returns204_thenGetReturns404() throws Exception {
        TaskResponse created = createTaskViaApi("Fix bug", "NEW", savedClient.getId());

        mockMvc.perform(delete("/api/tasks/{id}", created.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/tasks/{id}", created.getId()))
                .andExpect(status().isNotFound());
    }
}
