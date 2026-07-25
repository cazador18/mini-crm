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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ClientServiceImpl implements ClientService {

    private static final Logger log = LoggerFactory.getLogger(ClientServiceImpl.class);

    private final ClientRepository repository;
    private final CurrentUserService currentUserService;
    private final OwnershipGuard ownershipGuard;
    private final AuditService auditService;

    public ClientServiceImpl(ClientRepository repository,
                              CurrentUserService currentUserService,
                              OwnershipGuard ownershipGuard,
                              AuditService auditService) {
        this.repository = repository;
        this.currentUserService = currentUserService;
        this.ownershipGuard = ownershipGuard;
        this.auditService = auditService;
    }

    @Override
    public ClientResponse create(ClientRequest request) {
        Client client = new Client();
        client.setName(request.getName());
        client.setEmail(request.getEmail());
        client.setPhone(request.getPhone());
        client.setOwner(currentUserService.getCurrentUser());
        Client saved = repository.save(client);
        auditService.log("CREATE", "CLIENT", saved.getId());
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ClientResponse findById(Long id) {
        Client client = findOrThrow(id);
        ownershipGuard.check(currentUserService.getCurrentUser(), client);
        return toResponse(client);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ClientResponse> findAll(Pageable pageable) {
        User currentUser = currentUserService.getCurrentUser();
        if (currentUser.getRole() == UserRole.MANAGER) {
            return PageResponse.of(
                    repository.findByOwnerIdOrderByIdAsc(currentUser.getId(), pageable).map(this::toResponse));
        }
        return PageResponse.of(repository.findAllByOrderByIdAsc(pageable).map(this::toResponse));
    }

    @Override
    public ClientResponse update(Long id, ClientRequest request) {
        Client client = findOrThrow(id);
        ownershipGuard.check(currentUserService.getCurrentUser(), client);
        client.setName(request.getName());
        client.setEmail(request.getEmail());
        client.setPhone(request.getPhone());
        Client saved = repository.save(client);
        auditService.log("UPDATE", "CLIENT", saved.getId());
        return toResponse(saved);
    }

    @Override
    public void delete(Long id) {
        Client client = findOrThrow(id);
        ownershipGuard.check(currentUserService.getCurrentUser(), client);
        repository.delete(client);
        auditService.log("DELETE", "CLIENT", id);
    }

    private Client findOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Client not found: {}", id);
                    return new ClientNotFoundException(id);
                });
    }

    private ClientResponse toResponse(Client client) {
        ClientResponse response = new ClientResponse();
        response.setId(client.getId());
        response.setName(client.getName());
        response.setEmail(client.getEmail());
        response.setPhone(client.getPhone());
        response.setCreatedAt(client.getCreatedAt());
        response.setOwnerId(client.getOwner().getId());
        return response;
    }
}
