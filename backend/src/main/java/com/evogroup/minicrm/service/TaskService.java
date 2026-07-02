package com.evogroup.minicrm.service;

import com.evogroup.minicrm.dto.TaskRequest;
import com.evogroup.minicrm.dto.TaskResponse;
import com.evogroup.minicrm.model.TaskStatus;

import java.util.List;

public interface TaskService {
    TaskResponse create(TaskRequest request);
    TaskResponse findById(Long id);
    List<TaskResponse> findAll(TaskStatus status, Long clientId);
    TaskResponse update(Long id, TaskRequest request);
    void delete(Long id);
}
