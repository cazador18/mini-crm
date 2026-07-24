package com.evogroup.minicrm.controller;

import com.evogroup.minicrm.AbstractIntegrationTest;
import com.evogroup.minicrm.repository.AuditLogRepository;
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

class AuditControllerIT extends AbstractIntegrationTest {

    @Autowired ClientRepository clientRepo;
    @Autowired TaskRepository taskRepo;
    @Autowired AuditLogRepository auditLogRepo;

    private String adminToken;

    @BeforeEach
    void cleanUp() throws Exception {
        taskRepo.deleteAll();
        clientRepo.deleteAll();
        auditLogRepo.deleteAll();
        adminToken = adminToken();
    }

    @Test
    void getAudit_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/audit"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getAudit_asManager_returns403() throws Exception {
        String managerToken = registerManagerAndLogin();

        mockMvc.perform(get("/api/audit").header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAudit_asViewer_returns403() throws Exception {
        String viewerToken = createViewerAndLogin();

        mockMvc.perform(get("/api/audit").header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAudit_asAdmin_returns200() throws Exception {
        mockMvc.perform(get("/api/audit").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", notNullValue()));
    }

    @Test
    void getAudit_afterClientCreate_containsCreateEntry() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("name", "Alice", "email", "alice@example.com"));
        mockMvc.perform(post("/api/clients")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/audit").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].action", is("CREATE")))
                .andExpect(jsonPath("$.content[0].entity", is("CLIENT")))
                .andExpect(jsonPath("$.content[0].who", is("admin")));
    }
}
