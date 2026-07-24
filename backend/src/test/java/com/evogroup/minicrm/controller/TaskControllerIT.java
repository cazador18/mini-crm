package com.evogroup.minicrm.controller;

import com.evogroup.minicrm.AbstractIntegrationTest;
import com.evogroup.minicrm.dto.TaskResponse;
import com.evogroup.minicrm.model.Client;
import com.evogroup.minicrm.model.User;
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

class TaskControllerIT extends AbstractIntegrationTest {

    @Autowired ClientRepository clientRepo;
    @Autowired TaskRepository taskRepo;

    private String adminToken;
    private Client savedClient;

    @BeforeEach
    void setUp() throws Exception {
        taskRepo.deleteAll();
        clientRepo.deleteAll();
        adminToken = adminToken();

        User admin = userRepository.findByUsername("admin").orElseThrow();
        Client c = new Client();
        c.setName("Alice");
        c.setEmail("alice@example.com");
        c.setOwner(admin);
        savedClient = clientRepo.save(c);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private TaskResponse createTaskViaApi(String title, String status, Long clientId, String token) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "title", title,
                "status", status,
                "priority", "MEDIUM",
                "clientId", clientId));
        String content = mockMvc.perform(post("/api/tasks")
                        .header("Authorization", "Bearer " + token)
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
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.title", is("Fix bug")))
                .andExpect(jsonPath("$.priority", is("HIGH")))
                .andExpect(jsonPath("$.clientId", is(savedClient.getId().intValue())));
    }

    @Test
    void createTask_withoutToken_returns401() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "title", "Fix bug",
                "status", "NEW",
                "priority", "HIGH",
                "clientId", savedClient.getId()));

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createTask_asViewer_returns403() throws Exception {
        String viewerToken = createViewerAndLogin();
        String body = objectMapper.writeValueAsString(Map.of(
                "title", "Fix bug",
                "status", "NEW",
                "priority", "HIGH",
                "clientId", savedClient.getId()));

        mockMvc.perform(post("/api/tasks")
                        .header("Authorization", "Bearer " + viewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void createTask_managerNotOwnerOfClient_returns403() throws Exception {
        String otherManagerToken = registerManagerAndLogin();
        String body = objectMapper.writeValueAsString(Map.of(
                "title", "Fix bug",
                "status", "NEW",
                "priority", "HIGH",
                "clientId", savedClient.getId()));

        mockMvc.perform(post("/api/tasks")
                        .header("Authorization", "Bearer " + otherManagerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void createTask_unknownClientId_returns404() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "title", "Fix bug",
                "status", "NEW",
                "priority", "MEDIUM",
                "clientId", 99999));

        mockMvc.perform(post("/api/tasks")
                        .header("Authorization", "Bearer " + adminToken)
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
                        .header("Authorization", "Bearer " + adminToken)
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
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void findAllTasks_returnsAll() throws Exception {
        createTaskViaApi("Task A", "NEW",  savedClient.getId(), adminToken);
        createTaskViaApi("Task B", "DONE", savedClient.getId(), adminToken);

        mockMvc.perform(get("/api/tasks").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements", is(2)))
                .andExpect(jsonPath("$.size", is(20)));
    }

    @Test
    void findAllTasks_defaultPageSize_returns20() throws Exception {
        for (int i = 0; i < 21; i++) {
            createTaskViaApi("Task " + i, "NEW", savedClient.getId(), adminToken);
        }

        mockMvc.perform(get("/api/tasks").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(20)))
                .andExpect(jsonPath("$.totalElements", is(21)))
                .andExpect(jsonPath("$.totalPages", is(2)));
    }

    @Test
    void findAllTasks_filterByStatus() throws Exception {
        createTaskViaApi("New task",  "NEW",  savedClient.getId(), adminToken);
        createTaskViaApi("Done task", "DONE", savedClient.getId(), adminToken);

        mockMvc.perform(get("/api/tasks").param("status", "NEW").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].status", is("NEW")));
    }

    @Test
    void findAllTasks_filterByClientId() throws Exception {
        User admin = userRepository.findByUsername("admin").orElseThrow();
        Client other = new Client();
        other.setName("Bob");
        other.setEmail("bob@example.com");
        other.setOwner(admin);
        Client savedOther = clientRepo.save(other);

        createTaskViaApi("Alice task", "NEW", savedClient.getId(), adminToken);
        createTaskViaApi("Bob task",   "NEW", savedOther.getId(), adminToken);

        mockMvc.perform(get("/api/tasks")
                        .param("clientId", savedClient.getId().toString())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].clientId", is(savedClient.getId().intValue())));
    }

    @Test
    void findAllTasks_filterByStatusAndClientId() throws Exception {
        createTaskViaApi("New task",  "NEW",  savedClient.getId(), adminToken);
        createTaskViaApi("Done task", "DONE", savedClient.getId(), adminToken);

        mockMvc.perform(get("/api/tasks")
                        .param("status",   "NEW")
                        .param("clientId", savedClient.getId().toString())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].status", is("NEW")));
    }

    @Test
    void findAllTasks_manager_seesOnlyOwnClientsTasks() throws Exception {
        String managerAToken = registerManagerAndLogin();
        String managerBToken = registerManagerAndLogin();

        String clientABody = objectMapper.writeValueAsString(Map.of("name", "A-Client", "email", "a@example.com"));
        String clientAContent = mockMvc.perform(post("/api/clients")
                        .header("Authorization", "Bearer " + managerAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(clientABody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long clientAId = objectMapper.readTree(clientAContent).get("id").asLong();

        createTaskViaApi("Task for A", "NEW", clientAId, managerAToken);
        createTaskViaApi("Task for house", "NEW", savedClient.getId(), adminToken);

        mockMvc.perform(get("/api/tasks").header("Authorization", "Bearer " + managerAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title", is("Task for A")));

        mockMvc.perform(get("/api/tasks").header("Authorization", "Bearer " + managerBToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test
    void findTaskById_returnsTask() throws Exception {
        TaskResponse created = createTaskViaApi("Fix bug", "NEW", savedClient.getId(), adminToken);

        mockMvc.perform(get("/api/tasks/{id}", created.getId()).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(created.getId().intValue())))
                .andExpect(jsonPath("$.title", is("Fix bug")));
    }

    @Test
    void findTaskById_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/tasks/99999").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void findTaskById_managerNotOwner_returns403() throws Exception {
        TaskResponse created = createTaskViaApi("Fix bug", "NEW", savedClient.getId(), adminToken);
        String otherManagerToken = registerManagerAndLogin();

        mockMvc.perform(get("/api/tasks/{id}", created.getId())
                        .header("Authorization", "Bearer " + otherManagerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateTask_returnsUpdatedData() throws Exception {
        TaskResponse created = createTaskViaApi("Fix bug", "NEW", savedClient.getId(), adminToken);

        String updateBody = objectMapper.writeValueAsString(Map.of(
                "title",    "Fix bug",
                "status",   "IN_PROGRESS",
                "priority", "HIGH",
                "clientId", savedClient.getId()));

        mockMvc.perform(put("/api/tasks/{id}", created.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("IN_PROGRESS")))
                .andExpect(jsonPath("$.priority", is("HIGH")));
    }

    @Test
    void deleteTask_returns204_thenGetReturns404() throws Exception {
        TaskResponse created = createTaskViaApi("Fix bug", "NEW", savedClient.getId(), adminToken);

        mockMvc.perform(delete("/api/tasks/{id}", created.getId()).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/tasks/{id}", created.getId()).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteTask_notFound_returns404() throws Exception {
        mockMvc.perform(delete("/api/tasks/99999").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteTask_managerNotOwner_returns403() throws Exception {
        TaskResponse created = createTaskViaApi("Fix bug", "NEW", savedClient.getId(), adminToken);
        String otherManagerToken = registerManagerAndLogin();

        mockMvc.perform(delete("/api/tasks/{id}", created.getId())
                        .header("Authorization", "Bearer " + otherManagerToken))
                .andExpect(status().isForbidden());
    }
}
