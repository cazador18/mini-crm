package com.evogroup.minicrm.service;

import com.evogroup.minicrm.dto.NoteRequest;
import com.evogroup.minicrm.dto.NoteResponse;
import com.evogroup.minicrm.exception.ClientNotFoundException;
import com.evogroup.minicrm.exception.NoteNotFoundException;
import com.evogroup.minicrm.model.Client;
import com.evogroup.minicrm.model.Note;
import com.evogroup.minicrm.model.User;
import com.evogroup.minicrm.model.UserRole;
import com.evogroup.minicrm.repository.ClientRepository;
import com.evogroup.minicrm.repository.NoteRepository;
import com.evogroup.minicrm.security.CurrentUserService;
import com.evogroup.minicrm.security.OwnershipGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NoteServiceImplTest {

    @Mock NoteRepository   noteRepository;
    @Mock ClientRepository clientRepository;
    @Mock CurrentUserService currentUserService;

    private NoteServiceImpl service;

    private User admin;
    private User manager;
    private User otherManager;
    private Client client;
    private Note   note;
    private NoteRequest request;

    @BeforeEach
    void setUp() {
        service = new NoteServiceImpl(noteRepository, clientRepository, currentUserService, new OwnershipGuard());

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
        client.setOwner(manager);

        note = new Note();
        note.setId(10L);
        note.setContent("Meeting at 3pm");
        note.setCreatedAt(Instant.parse("2026-07-01T10:00:00Z"));
        note.setClient(client);

        request = new NoteRequest();
        request.setContent("Meeting at 3pm");
        request.setClientId(1L);
    }

    @Test
    void create_mapsRequestAndReturnsResponse_whenOwner() {
        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        when(currentUserService.getCurrentUser()).thenReturn(manager);
        when(noteRepository.save(any(Note.class))).thenReturn(note);

        NoteResponse response = service.create(request);

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getContent()).isEqualTo("Meeting at 3pm");
        assertThat(response.getClientId()).isEqualTo(1L);
        verify(noteRepository).save(any(Note.class));
    }

    @Test
    void create_throwsAccessDenied_whenManagerNotOwnerOfClient() {
        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        when(currentUserService.getCurrentUser()).thenReturn(otherManager);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(AccessDeniedException.class);

        verify(noteRepository, never()).save(any());
    }

    @Test
    void create_throwsClientNotFound_whenClientMissing() {
        when(clientRepository.findById(99L)).thenReturn(Optional.empty());
        request.setClientId(99L);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(ClientNotFoundException.class)
                .hasMessageContaining("99");

        verify(noteRepository, never()).save(any());
    }

    @Test
    void findById_returnsResponse_whenOwner() {
        when(noteRepository.findById(10L)).thenReturn(Optional.of(note));
        when(currentUserService.getCurrentUser()).thenReturn(manager);

        NoteResponse response = service.findById(10L);

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getContent()).isEqualTo("Meeting at 3pm");
    }

    @Test
    void findById_throwsAccessDenied_whenManagerNotOwner() {
        when(noteRepository.findById(10L)).thenReturn(Optional.of(note));
        when(currentUserService.getCurrentUser()).thenReturn(otherManager);

        assertThatThrownBy(() -> service.findById(10L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void findById_throwsNotFound_whenMissing() {
        when(noteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(99L))
                .isInstanceOf(NoteNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void findAll_admin_noFilter_returnsAll() {
        when(currentUserService.getCurrentUser()).thenReturn(admin);
        when(noteRepository.findAllByOrderByIdAsc()).thenReturn(List.of(note));

        List<NoteResponse> result = service.findAll(null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getContent()).isEqualTo("Meeting at 3pm");
        verify(noteRepository).findAllByOrderByIdAsc();
    }

    @Test
    void findAll_admin_byClientId_filtersCorrectly() {
        when(currentUserService.getCurrentUser()).thenReturn(admin);
        when(noteRepository.findByClientIdOrderByIdAsc(1L)).thenReturn(List.of(note));

        List<NoteResponse> result = service.findAll(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getClientId()).isEqualTo(1L);
        verify(noteRepository).findByClientIdOrderByIdAsc(1L);
    }

    @Test
    void findAll_manager_noFilter_scopedToOwnClients() {
        when(currentUserService.getCurrentUser()).thenReturn(manager);
        when(noteRepository.findByClientOwnerIdOrderByIdAsc(1L)).thenReturn(List.of(note));

        List<NoteResponse> result = service.findAll(null);

        assertThat(result).hasSize(1);
        verify(noteRepository).findByClientOwnerIdOrderByIdAsc(1L);
        verify(noteRepository, never()).findAllByOrderByIdAsc();
    }

    @Test
    void findAll_manager_byClientId_scopedToOwnClients() {
        when(currentUserService.getCurrentUser()).thenReturn(manager);
        when(noteRepository.findByClientIdAndClientOwnerIdOrderByIdAsc(1L, 1L)).thenReturn(List.of(note));

        List<NoteResponse> result = service.findAll(1L);

        assertThat(result).hasSize(1);
        verify(noteRepository).findByClientIdAndClientOwnerIdOrderByIdAsc(1L, 1L);
    }

    @Test
    void update_updatesContentAndReturnsResponse_whenOwner() {
        NoteRequest updateRequest = new NoteRequest();
        updateRequest.setContent("Updated note");
        updateRequest.setClientId(1L);

        Note updated = new Note();
        updated.setId(10L);
        updated.setContent("Updated note");
        updated.setCreatedAt(note.getCreatedAt());
        updated.setClient(client);

        when(noteRepository.findById(10L)).thenReturn(Optional.of(note));
        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        when(currentUserService.getCurrentUser()).thenReturn(manager);
        when(noteRepository.save(note)).thenReturn(updated);

        NoteResponse response = service.update(10L, updateRequest);

        assertThat(response.getContent()).isEqualTo("Updated note");
    }

    @Test
    void update_throwsAccessDenied_whenManagerNotOwnerOfExistingNote() {
        when(noteRepository.findById(10L)).thenReturn(Optional.of(note));
        when(currentUserService.getCurrentUser()).thenReturn(otherManager);

        assertThatThrownBy(() -> service.update(10L, request))
                .isInstanceOf(AccessDeniedException.class);

        verify(noteRepository, never()).save(any());
    }

    @Test
    void delete_deletesEntity_whenOwner() {
        when(noteRepository.findById(10L)).thenReturn(Optional.of(note));
        when(currentUserService.getCurrentUser()).thenReturn(manager);

        service.delete(10L);

        verify(noteRepository).delete(note);
    }

    @Test
    void delete_throwsAccessDenied_whenManagerNotOwner() {
        when(noteRepository.findById(10L)).thenReturn(Optional.of(note));
        when(currentUserService.getCurrentUser()).thenReturn(otherManager);

        assertThatThrownBy(() -> service.delete(10L))
                .isInstanceOf(AccessDeniedException.class);

        verify(noteRepository, never()).delete(any());
    }

    @Test
    void delete_throwsNotFound_whenMissing() {
        when(noteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(99L))
                .isInstanceOf(NoteNotFoundException.class)
                .hasMessageContaining("99");

        verify(noteRepository, never()).delete(any());
    }
}
