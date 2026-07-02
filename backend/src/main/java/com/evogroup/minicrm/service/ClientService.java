package com.evogroup.minicrm.service;

import com.evogroup.minicrm.dto.ClientRequest;
import com.evogroup.minicrm.dto.ClientResponse;

import java.util.List;

public interface ClientService {
    ClientResponse create(ClientRequest request);
    ClientResponse findById(Long id);
    List<ClientResponse> findAll();
    ClientResponse update(Long id, ClientRequest request);
    void delete(Long id);
}
