package com.evogroup.minicrm.controller;

import com.evogroup.minicrm.AbstractIntegrationTest;
import com.evogroup.minicrm.dto.ClientResponse;
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

class ClientControllerIT extends AbstractIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired ClientRepository clientRepo;
    @Autowired TaskRepository taskRepo;

    @BeforeEach
    void cleanUp() {
        taskRepo.deleteAll();
        clientRepo.deleteAll();
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private ClientResponse createClientViaApi(String name, String email) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("name", name, "email", email));
        String content = mockMvc.perform(post("/api/clients")
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
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void findAllClients_returnsListSortedById() throws Exception {
        createClientViaApi("Alice", "alice@example.com");
        createClientViaApi("Bob", "bob@example.com");

        mockMvc.perform(get("/api/clients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name", is("Alice")))
                .andExpect(jsonPath("$[1].name", is("Bob")));
    }

    @Test
    void findClientById_returnsClient() throws Exception {
        ClientResponse created = createClientViaApi("Alice", "alice@example.com");

        mockMvc.perform(get("/api/clients/{id}", created.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(created.getId().intValue())))
                .andExpect(jsonPath("$.name", is("Alice")))
                .andExpect(jsonPath("$.email", is("alice@example.com")));
    }

    @Test
    void findClientById_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/clients/99999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateClient_returnsUpdatedData() throws Exception {
        ClientResponse created = createClientViaApi("Alice", "alice@example.com");

        String updateBody = objectMapper.writeValueAsString(
                Map.of("name", "Bob", "email", "bob@example.com"));

        mockMvc.perform(put("/api/clients/{id}", created.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(created.getId().intValue())))
                .andExpect(jsonPath("$.name", is("Bob")))
                .andExpect(jsonPath("$.email", is("bob@example.com")));
    }

    @Test
    void deleteClient_returns204_thenGetReturns404() throws Exception {
        ClientResponse created = createClientViaApi("Alice", "alice@example.com");

        mockMvc.perform(delete("/api/clients/{id}", created.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/clients/{id}", created.getId()))
                .andExpect(status().isNotFound());
    }
}
