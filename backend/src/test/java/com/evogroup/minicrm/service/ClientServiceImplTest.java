package com.evogroup.minicrm.service;

import com.evogroup.minicrm.dto.ClientRequest;
import com.evogroup.minicrm.dto.ClientResponse;
import com.evogroup.minicrm.dto.PageResponse;
import com.evogroup.minicrm.exception.ClientNotFoundException;
import com.evogroup.minicrm.model.Client;
import com.evogroup.minicrm.model.User;
import com.evogroup.minicrm.model.UserRole;
import com.evogroup.minicrm.repository.ClientRepository;
import com.evogroup.minicrm.security.CurrentUserService;
import com.evogroup.minicrm.security.OwnershipGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

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

    @Mock
    private CurrentUserService currentUserService;

    private ClientServiceImpl service;

    private User admin;
    private User manager;
    private User otherManager;
    private Client client;
    private ClientRequest request;

    @BeforeEach
    void setUp() {
        service = new ClientServiceImpl(repository, currentUserService, new OwnershipGuard());

        admin = new User();
        admin.setId(100L);
        admin.setRole(UserRole.ADMIN);

        manager = new User();
        manager.setId(1L);
        manager.setRole(UserRole.MANAGER);

        otherManager = new User();
        otherManager.setId(2L);
        otherManager.setRole(UserRole.MANAGER);

        client = new Client();
        client.setId(1L);
        client.setName("Alice");
        client.setEmail("alice@example.com");
        client.setPhone("555-1234");
        client.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));
        client.setOwner(manager);

        request = new ClientRequest();
        request.setName("Alice");
        request.setEmail("alice@example.com");
        request.setPhone("555-1234");
    }

    @Test
    void create_setsOwnerToCurrentUser_returnsResponse() {
        when(currentUserService.getCurrentUser()).thenReturn(manager);
        when(repository.save(any(Client.class))).thenReturn(client);

        ClientResponse response = service.create(request);

        ArgumentCaptor<Client> captor = ArgumentCaptor.forClass(Client.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getOwner()).isEqualTo(manager);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Alice");
        assertThat(response.getOwnerId()).isEqualTo(1L);
    }

    @Test
    void findById_returnsResponse_whenOwner() {
        when(repository.findById(1L)).thenReturn(Optional.of(client));
        when(currentUserService.getCurrentUser()).thenReturn(manager);

        ClientResponse response = service.findById(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getEmail()).isEqualTo("alice@example.com");
    }

    @Test
    void findById_returnsResponse_forAdmin_evenWhenNotOwner() {
        when(repository.findById(1L)).thenReturn(Optional.of(client));
        when(currentUserService.getCurrentUser()).thenReturn(admin);

        ClientResponse response = service.findById(1L);

        assertThat(response.getId()).isEqualTo(1L);
    }

    @Test
    void findById_throwsAccessDenied_whenManagerNotOwner() {
        when(repository.findById(1L)).thenReturn(Optional.of(client));
        when(currentUserService.getCurrentUser()).thenReturn(otherManager);

        assertThatThrownBy(() -> service.findById(1L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void findById_throwsNotFound_whenClientMissing() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(99L))
                .isInstanceOf(ClientNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void findAll_admin_returnsAllClients() {
        Client second = new Client();
        second.setId(2L);
        second.setName("Bob");
        second.setEmail("bob@example.com");
        second.setCreatedAt(Instant.now());
        second.setOwner(otherManager);

        Pageable pageable = PageRequest.of(0, 20);
        when(currentUserService.getCurrentUser()).thenReturn(admin);
        when(repository.findAllByOrderByIdAsc(pageable))
                .thenReturn(new PageImpl<>(List.of(client, second), pageable, 2));

        PageResponse<ClientResponse> result = service.findAll(pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent()).extracting(ClientResponse::getName)
                .containsExactly("Alice", "Bob");
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    void findAll_manager_returnsOnlyOwnClients() {
        Pageable pageable = PageRequest.of(0, 20);
        when(currentUserService.getCurrentUser()).thenReturn(manager);
        when(repository.findByOwnerIdOrderByIdAsc(1L, pageable))
                .thenReturn(new PageImpl<>(List.of(client), pageable, 1));

        PageResponse<ClientResponse> result = service.findAll(pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(repository).findByOwnerIdOrderByIdAsc(1L, pageable);
        verify(repository, never()).findAllByOrderByIdAsc(any());
    }

    @Test
    void update_updatesFieldsAndReturnsResponse_whenOwner() {
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
        saved.setOwner(manager);

        when(repository.findById(1L)).thenReturn(Optional.of(client));
        when(currentUserService.getCurrentUser()).thenReturn(manager);
        when(repository.save(client)).thenReturn(saved);

        ClientResponse response = service.update(1L, updateRequest);

        assertThat(response.getName()).isEqualTo("Alice Updated");
        assertThat(response.getEmail()).isEqualTo("new@example.com");
    }

    @Test
    void update_throwsAccessDenied_whenManagerNotOwner() {
        when(repository.findById(1L)).thenReturn(Optional.of(client));
        when(currentUserService.getCurrentUser()).thenReturn(otherManager);

        assertThatThrownBy(() -> service.update(1L, request))
                .isInstanceOf(AccessDeniedException.class);

        verify(repository, never()).save(any());
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
    void delete_deletesEntity_whenOwner() {
        when(repository.findById(1L)).thenReturn(Optional.of(client));
        when(currentUserService.getCurrentUser()).thenReturn(manager);

        service.delete(1L);

        verify(repository).delete(client);
    }

    @Test
    void delete_throwsAccessDenied_whenManagerNotOwner() {
        when(repository.findById(1L)).thenReturn(Optional.of(client));
        when(currentUserService.getCurrentUser()).thenReturn(otherManager);

        assertThatThrownBy(() -> service.delete(1L))
                .isInstanceOf(AccessDeniedException.class);

        verify(repository, never()).delete(any());
    }

    @Test
    void delete_throwsNotFound_whenClientMissing() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(99L))
                .isInstanceOf(ClientNotFoundException.class)
                .hasMessageContaining("99");

        verify(repository, never()).delete(any());
    }
}
