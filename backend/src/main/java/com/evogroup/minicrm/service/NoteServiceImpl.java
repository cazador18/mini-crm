package com.evogroup.minicrm.service;

import com.evogroup.minicrm.dto.NoteRequest;
import com.evogroup.minicrm.dto.NoteResponse;
import com.evogroup.minicrm.exception.ClientNotFoundException;
import com.evogroup.minicrm.exception.NoteNotFoundException;
import com.evogroup.minicrm.model.Client;
import com.evogroup.minicrm.model.Note;
import com.evogroup.minicrm.repository.ClientRepository;
import com.evogroup.minicrm.repository.NoteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class NoteServiceImpl implements NoteService {

    private static final Logger log = LoggerFactory.getLogger(NoteServiceImpl.class);

    private final NoteRepository noteRepository;
    private final ClientRepository clientRepository;

    public NoteServiceImpl(NoteRepository noteRepository, ClientRepository clientRepository) {
        this.noteRepository = noteRepository;
        this.clientRepository = clientRepository;
    }

    @Override
    public NoteResponse create(NoteRequest request) {
        Client client = findClientOrThrow(request.getClientId());
        Note note = new Note();
        mapRequest(request, note, client);
        return toResponse(noteRepository.save(note));
    }

    @Override
    @Transactional(readOnly = true)
    public NoteResponse findById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<NoteResponse> findAll(Long clientId) {
        List<Note> notes = clientId != null
                ? noteRepository.findByClientIdOrderByIdAsc(clientId)
                : noteRepository.findAllByOrderByIdAsc();
        return notes.stream().map(this::toResponse).toList();
    }

    @Override
    public NoteResponse update(Long id, NoteRequest request) {
        Note note = findOrThrow(id);
        Client client = findClientOrThrow(request.getClientId());
        mapRequest(request, note, client);
        return toResponse(noteRepository.save(note));
    }

    @Override
    public void delete(Long id) {
        noteRepository.deleteById(id);
    }

    private Note findOrThrow(Long id) {
        return noteRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Note not found: {}", id);
                    return new NoteNotFoundException(id);
                });
    }

    private Client findClientOrThrow(Long clientId) {
        return clientRepository.findById(clientId)
                .orElseThrow(() -> {
                    log.warn("Client not found: {}", clientId);
                    return new ClientNotFoundException(clientId);
                });
    }

    private void mapRequest(NoteRequest request, Note note, Client client) {
        note.setContent(request.getContent());
        note.setClient(client);
    }

    private NoteResponse toResponse(Note note) {
        NoteResponse r = new NoteResponse();
        r.setId(note.getId());
        r.setContent(note.getContent());
        r.setCreatedAt(note.getCreatedAt());
        r.setClientId(note.getClient().getId());
        return r;
    }
}
