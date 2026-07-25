package com.evogroup.minicrm.controller;

import com.evogroup.minicrm.AbstractIntegrationTest;
import com.evogroup.minicrm.dto.ClientResponse;
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

class ClientControllerIT extends AbstractIntegrationTest {

    @Autowired ClientRepository clientRepo;
    @Autowired TaskRepository taskRepo;

    private String adminToken;

    @BeforeEach
    void cleanUp() throws Exception {
        taskRepo.deleteAll();
        clientRepo.deleteAll();
        adminToken = adminToken();
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private ClientResponse createClientViaApi(String name, String email, String token) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("name", name, "email", email));
        String content = mockMvc.perform(post("/api/clients")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readValue(content, ClientResponse.class);
    }

    // ── tests ─────────────────────────────────────────────────────────────────

    @Test
    void createClient_returns201WithBody() throws Exception {
        String body = objectMapper.writeValueAsString(
                Map.of("name", "Alice", "email", "alice@example.com"));

        mockMvc.perform(post("/api/clients")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.name", is("Alice")))
                .andExpect(jsonPath("$.email", is("alice@example.com")));
    }

    @Test
    void createClient_missingName_returns400() throws Exception {
        // name is @NotBlank — missing field deserializes as null → 400
        String body = objectMapper.writeValueAsString(Map.of("email", "alice@example.com"));

        mockMvc.perform(post("/api/clients")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createClient_withoutToken_returns401() throws Exception {
        String body = objectMapper.writeValueAsString(
                Map.of("name", "Alice", "email", "alice@example.com"));

        mockMvc.perform(post("/api/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createClient_asViewer_returns403() throws Exception {
        String viewerToken = createViewerAndLogin();
        String body = objectMapper.writeValueAsString(
                Map.of("name", "Alice", "email", "alice@example.com"));

        mockMvc.perform(post("/api/clients")
                        .header("Authorization", "Bearer " + viewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void findAllClients_returnsListSortedById() throws Exception {
        createClientViaApi("Alice", "alice@example.com", adminToken);
        createClientViaApi("Bob", "bob@example.com", adminToken);

        mockMvc.perform(get("/api/clients").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].name", is("Alice")))
                .andExpect(jsonPath("$.content[1].name", is("Bob")));
    }

    @Test
    void findAllClients_manager_seesOnlyOwnClients() throws Exception {
        String managerAToken = registerManagerAndLogin();
        String managerBToken = registerManagerAndLogin();

        createClientViaApi("Alice", "alice@example.com", managerAToken);
        createClientViaApi("Bob", "bob@example.com", managerBToken);

        mockMvc.perform(get("/api/clients").header("Authorization", "Bearer " + managerAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name", is("Alice")));
    }

    @Test
    void findClientById_returnsClient() throws Exception {
        ClientResponse created = createClientViaApi("Alice", "alice@example.com", adminToken);

        mockMvc.perform(get("/api/clients/{id}", created.getId()).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(created.getId().intValue())))
                .andExpect(jsonPath("$.name", is("Alice")))
                .andExpect(jsonPath("$.email", is("alice@example.com")));
    }

    @Test
    void findClientById_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/clients/99999").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void findClientById_managerNotOwner_returns403() throws Exception {
        String ownerToken = registerManagerAndLogin();
        String otherManagerToken = registerManagerAndLogin();
        ClientResponse created = createClientViaApi("Alice", "alice@example.com", ownerToken);

        mockMvc.perform(get("/api/clients/{id}", created.getId())
                        .header("Authorization", "Bearer " + otherManagerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateClient_returnsUpdatedData() throws Exception {
        ClientResponse created = createClientViaApi("Alice", "alice@example.com", adminToken);

        String updateBody = objectMapper.writeValueAsString(
                Map.of("name", "Bob", "email", "bob@example.com"));

        mockMvc.perform(put("/api/clients/{id}", created.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(created.getId().intValue())))
                .andExpect(jsonPath("$.name", is("Bob")))
                .andExpect(jsonPath("$.email", is("bob@example.com")));
    }

    @Test
    void deleteClient_returns204_thenGetReturns404() throws Exception {
        ClientResponse created = createClientViaApi("Alice", "alice@example.com", adminToken);

        mockMvc.perform(delete("/api/clients/{id}", created.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/clients/{id}", created.getId()).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteClient_notFound_returns404() throws Exception {
        mockMvc.perform(delete("/api/clients/99999").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteClient_managerNotOwner_returns403() throws Exception {
        String ownerToken = registerManagerAndLogin();
        String otherManagerToken = registerManagerAndLogin();
        ClientResponse created = createClientViaApi("Alice", "alice@example.com", ownerToken);

        mockMvc.perform(delete("/api/clients/{id}", created.getId())
                        .header("Authorization", "Bearer " + otherManagerToken))
                .andExpect(status().isForbidden());
    }
}
