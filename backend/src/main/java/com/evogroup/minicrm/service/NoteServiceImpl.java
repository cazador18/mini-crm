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
    private final CurrentUserService currentUserService;
    private final OwnershipGuard ownershipGuard;

    public NoteServiceImpl(NoteRepository noteRepository,
                            ClientRepository clientRepository,
                            CurrentUserService currentUserService,
                            OwnershipGuard ownershipGuard) {
        this.noteRepository = noteRepository;
        this.clientRepository = clientRepository;
        this.currentUserService = currentUserService;
        this.ownershipGuard = ownershipGuard;
    }

    @Override
    public NoteResponse create(NoteRequest request) {
        Client client = findClientOrThrow(request.getClientId());
        ownershipGuard.check(currentUserService.getCurrentUser(), client);
        Note note = new Note();
        mapRequest(request, note, client);
        return toResponse(noteRepository.save(note));
    }

    @Override
    @Transactional(readOnly = true)
    public NoteResponse findById(Long id) {
        Note note = findOrThrow(id);
        ownershipGuard.check(currentUserService.getCurrentUser(), note.getClient());
        return toResponse(note);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NoteResponse> findAll(Long clientId) {
        User currentUser = currentUserService.getCurrentUser();
        List<Note> notes;
        if (currentUser.getRole() == UserRole.MANAGER) {
            notes = clientId != null
                    ? noteRepository.findByClientIdAndClientOwnerIdOrderByIdAsc(clientId, currentUser.getId())
                    : noteRepository.findByClientOwnerIdOrderByIdAsc(currentUser.getId());
        } else {
            notes = clientId != null
                    ? noteRepository.findByClientIdOrderByIdAsc(clientId)
                    : noteRepository.findAllByOrderByIdAsc();
        }
        return notes.stream().map(this::toResponse).toList();
    }

    @Override
    public NoteResponse update(Long id, NoteRequest request) {
        User currentUser = currentUserService.getCurrentUser();
        Note note = findOrThrow(id);
        ownershipGuard.check(currentUser, note.getClient());
        Client client = findClientOrThrow(request.getClientId());
        ownershipGuard.check(currentUser, client);
        mapRequest(request, note, client);
        return toResponse(noteRepository.save(note));
    }

    @Override
    public void delete(Long id) {
        Note note = findOrThrow(id);
        ownershipGuard.check(currentUserService.getCurrentUser(), note.getClient());
        noteRepository.delete(note);
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
