package com.evogroup.minicrm.service;

import com.evogroup.minicrm.dto.NoteRequest;
import com.evogroup.minicrm.dto.NoteResponse;
import com.evogroup.minicrm.exception.ClientNotFoundException;
import com.evogroup.minicrm.exception.NoteNotFoundException;
import com.evogroup.minicrm.model.Client;
import com.evogroup.minicrm.model.Note;
import com.evogroup.minicrm.repository.ClientRepository;
import com.evogroup.minicrm.repository.NoteRepository;
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
class NoteServiceImplTest {

    @Mock NoteRepository   noteRepository;
    @Mock ClientRepository clientRepository;

    @InjectMocks NoteServiceImpl service;

    private Client client;
    private Note   note;
    private NoteRequest request;

    @BeforeEach
    void setUp() {
        client = new Client();
        client.setId(1L);
        client.setName("Alice");
        client.setEmail("alice@example.com");

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
    void create_mapsRequestAndReturnsResponse() {
        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        when(noteRepository.save(any(Note.class))).thenReturn(note);

        NoteResponse response = service.create(request);

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getContent()).isEqualTo("Meeting at 3pm");
        assertThat(response.getClientId()).isEqualTo(1L);
        verify(noteRepository).save(any(Note.class));
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
    void findById_returnsResponse_whenExists() {
        when(noteRepository.findById(10L)).thenReturn(Optional.of(note));

        NoteResponse response = service.findById(10L);

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getContent()).isEqualTo("Meeting at 3pm");
    }

    @Test
    void findById_throwsNotFound_whenMissing() {
        when(noteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(99L))
                .isInstanceOf(NoteNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void findAll_noFilter_returnsAll() {
        when(noteRepository.findAllByOrderByIdAsc()).thenReturn(List.of(note));

        List<NoteResponse> result = service.findAll(null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getContent()).isEqualTo("Meeting at 3pm");
        verify(noteRepository).findAllByOrderByIdAsc();
    }

    @Test
    void findAll_byClientId_filtersCorrectly() {
        when(noteRepository.findByClientIdOrderByIdAsc(1L)).thenReturn(List.of(note));

        List<NoteResponse> result = service.findAll(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getClientId()).isEqualTo(1L);
        verify(noteRepository).findByClientIdOrderByIdAsc(1L);
    }

    @Test
    void update_updatesContentAndReturnsResponse() {
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
        when(noteRepository.save(note)).thenReturn(updated);

        NoteResponse response = service.update(10L, updateRequest);

        assertThat(response.getContent()).isEqualTo("Updated note");
    }

    @Test
    void delete_callsDeleteById_whenExists() {
        when(noteRepository.findById(10L)).thenReturn(Optional.of(note));

        service.delete(10L);

        verify(noteRepository).deleteById(10L);
    }
}
