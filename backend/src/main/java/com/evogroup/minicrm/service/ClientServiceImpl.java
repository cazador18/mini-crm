package com.evogroup.minicrm.service;

import com.evogroup.minicrm.dto.ClientRequest;
import com.evogroup.minicrm.dto.ClientResponse;
import com.evogroup.minicrm.dto.PageResponse;
import com.evogroup.minicrm.exception.ClientNotFoundException;
import com.evogroup.minicrm.model.Client;
import com.evogroup.minicrm.repository.ClientRepository;
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

    public ClientServiceImpl(ClientRepository repository) {
        this.repository = repository;
    }

    @Override
    public ClientResponse create(ClientRequest request) {
        Client client = new Client();
        client.setName(request.getName());
        client.setEmail(request.getEmail());
        client.setPhone(request.getPhone());
        return toResponse(repository.save(client));
    }

    @Override
    @Transactional(readOnly = true)
    public ClientResponse findById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ClientResponse> findAll(Pageable pageable) {
        return PageResponse.of(repository.findAllByOrderByIdAsc(pageable).map(this::toResponse));
    }

    @Override
    public ClientResponse update(Long id, ClientRequest request) {
        Client client = findOrThrow(id);
        client.setName(request.getName());
        client.setEmail(request.getEmail());
        client.setPhone(request.getPhone());
        return toResponse(repository.save(client));
    }

    @Override
    public void delete(Long id) {
        repository.deleteById(id);
    }

    private Client findOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> {
                    log.error("Client not found: {}", id);
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
        return response;
    }
}
