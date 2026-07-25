package com.evogroup.minicrm.service;

import com.evogroup.minicrm.dto.AuditLogResponse;
import com.evogroup.minicrm.dto.PageResponse;
import org.springframework.data.domain.Pageable;

public interface AuditService {
    void log(String action, String entity, Long entityId);
    PageResponse<AuditLogResponse> findAll(Pageable pageable);
}
