package com.evogroup.minicrm.service;

import com.evogroup.minicrm.dto.PageResponse;
import com.evogroup.minicrm.dto.TaskRequest;
import com.evogroup.minicrm.dto.TaskResponse;
import com.evogroup.minicrm.model.TaskStatus;
import org.springframework.data.domain.Pageable;

public interface TaskService {
    TaskResponse create(TaskRequest request);
    TaskResponse findById(Long id);
    PageResponse<TaskResponse> findAll(TaskStatus status, Long clientId, Pageable pageable);
    TaskResponse update(Long id, TaskRequest request);
    void delete(Long id);
}
