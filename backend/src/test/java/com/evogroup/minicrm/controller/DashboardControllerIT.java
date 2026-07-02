package com.evogroup.minicrm.controller;

import com.evogroup.minicrm.AbstractIntegrationTest;
import com.evogroup.minicrm.model.Client;
import com.evogroup.minicrm.model.Task;
import com.evogroup.minicrm.model.TaskPriority;
import com.evogroup.minicrm.model.TaskStatus;
import com.evogroup.minicrm.repository.ClientRepository;
import com.evogroup.minicrm.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class DashboardControllerIT extends AbstractIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ClientRepository clientRepo;
    @Autowired TaskRepository   taskRepo;

    @BeforeEach
    void setUp() {
        taskRepo.deleteAll();
        clientRepo.deleteAll();
    }

    private Client saveClient(String name, String email) {
        Client c = new Client();
        c.setName(name);
        c.setEmail(email);
        return clientRepo.save(c);
    }

    private void saveTask(String title, TaskStatus status, Client client) {
        Task t = new Task();
        t.setTitle(title);
        t.setStatus(status);
        t.setPriority(TaskPriority.MEDIUM);
        t.setClient(client);
        taskRepo.save(t);
    }

    @Test
    void getStats_returnsCorrectCounts() throws Exception {
        Client alice = saveClient("Alice", "alice@example.com");
        Client bob   = saveClient("Bob",   "bob@example.com");

        saveTask("T1", TaskStatus.NEW,         alice);
        saveTask("T2", TaskStatus.NEW,         alice);
        saveTask("T3", TaskStatus.IN_PROGRESS, bob);
        saveTask("T4", TaskStatus.DONE,        bob);

        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalClients",              is(2)))
                .andExpect(jsonPath("$.tasksByStatus.NEW",         is(2)))
                .andExpect(jsonPath("$.tasksByStatus.IN_PROGRESS", is(1)))
                .andExpect(jsonPath("$.tasksByStatus.DONE",        is(1)));
    }

    @Test
    void getStats_withNoData_returnsZeroes() throws Exception {
        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalClients",              is(0)))
                .andExpect(jsonPath("$.tasksByStatus.NEW",         is(0)))
                .andExpect(jsonPath("$.tasksByStatus.IN_PROGRESS", is(0)))
                .andExpect(jsonPath("$.tasksByStatus.DONE",        is(0)));
    }
}
