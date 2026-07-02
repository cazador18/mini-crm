package com.evogroup.minicrm.service;

import com.evogroup.minicrm.dto.ClientRequest;
import com.evogroup.minicrm.dto.ClientResponse;
import com.evogroup.minicrm.exception.ClientNotFoundException;
import com.evogroup.minicrm.model.Client;
import com.evogroup.minicrm.repository.ClientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClientServiceImplTest {

    @Mock
    private ClientRepository repository;

    @InjectMocks
    private ClientServiceImpl service;

    private Client client;
    private ClientRequest request;

    @BeforeEach
    void setUp() {
        client = new Client();
        client.setId(1L);
        client.setName("Alice");
        client.setEmail("alice@example.com");
        client.setPhone("555-1234");
        client.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));

        request = new ClientRequest();
        request.setName("Alice");
        request.setEmail("alice@example.com");
        request.setPhone("555-1234");
    }

    @Test
    void create_mapsRequestAndReturnsResponse() {
        when(repository.save(any(Client.class))).thenReturn(client);

        ClientResponse response = service.create(request);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Alice");
        assertThat(response.getEmail()).isEqualTo("alice@example.com");
        assertThat(response.getPhone()).isEqualTo("555-1234");
        verify(repository).save(any(Client.class));
    }

    @Test
    void findById_returnsResponse_whenClientExists() {
        when(repository.findById(1L)).thenReturn(Optional.of(client));

        ClientResponse response = service.findById(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getEmail()).isEqualTo("alice@example.com");
    }

    @Test
    void findById_throwsNotFound_whenClientMissing() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(99L))
                .isInstanceOf(ClientNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void findAll_returnsAllClients() {
        Client second = new Client();
        second.setId(2L);
        second.setName("Bob");
        second.setEmail("bob@example.com");
        second.setCreatedAt(Instant.now());

        when(repository.findAllByOrderByIdAsc()).thenReturn(List.of(client, second));

        List<ClientResponse> result = service.findAll();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(ClientResponse::getName)
                .containsExactly("Alice", "Bob");
    }

    @Test
    void update_updatesFieldsAndReturnsResponse() {
        ClientRequest updateRequest = new ClientRequest();
        updateRequest.setName("Alice Updated");
        updateRequest.setEmail("new@example.com");
        updateRequest.setPhone("999-0000");

        Client saved = new Client();
        saved.setId(1L);
        saved.setName("Alice Updated");
        saved.setEmail("new@example.com");
        saved.setPhone("999-0000");
        saved.setCreatedAt(client.getCreatedAt());

        when(repository.findById(1L)).thenReturn(Optional.of(client));
        when(repository.save(client)).thenReturn(saved);

        ClientResponse response = service.update(1L, updateRequest);

        assertThat(response.getName()).isEqualTo("Alice Updated");
        assertThat(response.getEmail()).isEqualTo("new@example.com");
    }

    @Test
    void update_throwsNotFound_whenClientMissing() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(99L, request))
                .isInstanceOf(ClientNotFoundException.class)
                .hasMessageContaining("99");

        verify(repository, never()).save(any());
    }

    @Test
    void delete_callsDeleteById_whenClientExists() {
        when(repository.findById(1L)).thenReturn(Optional.of(client));

        service.delete(1L);

        verify(repository).deleteById(1L);
    }

    @Test
    void delete_throwsNotFound_whenClientMissing() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(99L))
                .isInstanceOf(ClientNotFoundException.class)
                .hasMessageContaining("99");

        verify(repository, never()).deleteById(any());
    }
}
