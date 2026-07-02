package com.evogroup.minicrm.service;

import com.evogroup.minicrm.dto.NoteRequest;
import com.evogroup.minicrm.dto.NoteResponse;

import java.util.List;

public interface NoteService {
    NoteResponse create(NoteRequest request);
    NoteResponse findById(Long id);
    List<NoteResponse> findAll(Long clientId);
    NoteResponse update(Long id, NoteRequest request);
    void delete(Long id);
}
