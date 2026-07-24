package com.evogroup.minicrm.service;

import com.evogroup.minicrm.dto.AuditLogResponse;
import com.evogroup.minicrm.dto.PageResponse;
import com.evogroup.minicrm.model.AuditLog;
import com.evogroup.minicrm.repository.AuditLogRepository;
import com.evogroup.minicrm.security.CurrentUserService;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@Transactional
public class AuditServiceImpl implements AuditService {

    private final AuditLogRepository repository;
    private final CurrentUserService currentUserService;

    public AuditServiceImpl(AuditLogRepository repository, CurrentUserService currentUserService) {
        this.repository = repository;
        this.currentUserService = currentUserService;
    }

    @Override
    public void log(String action, String entity, Long entityId) {
        AuditLog entry = new AuditLog();
        entry.setWho(currentUserService.getCurrentUser().getUsername());
        entry.setAction(action);
        entry.setEntity(entity);
        entry.setEntityId(entityId);
        entry.setTimestamp(Instant.now());
        repository.save(entry);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> findAll(Pageable pageable) {
        return PageResponse.of(repository.findAllByOrderByTimestampDesc(pageable).map(this::toResponse));
    }

    private AuditLogResponse toResponse(AuditLog log) {
        AuditLogResponse response = new AuditLogResponse();
        response.setId(log.getId());
        response.setWho(log.getWho());
        response.setAction(log.getAction());
        response.setEntity(log.getEntity());
        response.setEntityId(log.getEntityId());
        response.setTimestamp(log.getTimestamp());
        return response;
    }
}
