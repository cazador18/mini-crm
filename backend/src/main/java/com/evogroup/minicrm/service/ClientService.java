package com.evogroup.minicrm.service;

import com.evogroup.minicrm.dto.ClientRequest;
import com.evogroup.minicrm.dto.ClientResponse;
import com.evogroup.minicrm.dto.PageResponse;
import org.springframework.data.domain.Pageable;

public interface ClientService {
    ClientResponse create(ClientRequest request);
    ClientResponse findById(Long id);
    PageResponse<ClientResponse> findAll(Pageable pageable);
    ClientResponse update(Long id, ClientRequest request);
    void delete(Long id);
}
