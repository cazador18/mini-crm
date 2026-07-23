package com.evogroup.minicrm.service;

import com.evogroup.minicrm.dto.PageResponse;
import com.evogroup.minicrm.dto.TaskRequest;
import com.evogroup.minicrm.dto.TaskResponse;
import com.evogroup.minicrm.exception.ClientNotFoundException;
import com.evogroup.minicrm.exception.TaskNotFoundException;
import com.evogroup.minicrm.model.Client;
import com.evogroup.minicrm.model.Task;
import com.evogroup.minicrm.model.TaskStatus;
import com.evogroup.minicrm.repository.ClientRepository;
import com.evogroup.minicrm.repository.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TaskServiceImpl implements TaskService {

    private static final Logger log = LoggerFactory.getLogger(TaskServiceImpl.class);

    private final TaskRepository taskRepository;
    private final ClientRepository clientRepository;

    public TaskServiceImpl(TaskRepository taskRepository, ClientRepository clientRepository) {
        this.taskRepository = taskRepository;
        this.clientRepository = clientRepository;
    }

    @Override
    public TaskResponse create(TaskRequest request) {
        Client client = findClientOrThrow(request.getClientId());
        Task task = new Task();
        mapRequestToTask(request, task, client);
        return toResponse(taskRepository.save(task));
    }

    @Override
    @Transactional(readOnly = true)
    public TaskResponse findById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<TaskResponse> findAll(TaskStatus status, Long clientId, Pageable pageable) {
        Page<Task> tasks;
        if (status != null && clientId != null) {
            tasks = taskRepository.findByStatusAndClientIdOrderByIdAsc(status, clientId, pageable);
        } else if (status != null) {
            tasks = taskRepository.findByStatusOrderByIdAsc(status, pageable);
        } else if (clientId != null) {
            tasks = taskRepository.findByClientIdOrderByIdAsc(clientId, pageable);
        } else {
            tasks = taskRepository.findAllByOrderByIdAsc(pageable);
        }
        return PageResponse.of(tasks.map(this::toResponse));
    }

    @Override
    public TaskResponse update(Long id, TaskRequest request) {
        Task task = findOrThrow(id);
        Client client = findClientOrThrow(request.getClientId());
        mapRequestToTask(request, task, client);
        return toResponse(taskRepository.save(task));
    }

    @Override
    public void delete(Long id) {
        taskRepository.deleteById(id);
    }

    private Task findOrThrow(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Task not found: {}", id);
                    return new TaskNotFoundException(id);
                });
    }

    private Client findClientOrThrow(Long clientId) {
        return clientRepository.findById(clientId)
                .orElseThrow(() -> {
                    log.warn("Client not found: {}", clientId);
                    return new ClientNotFoundException(clientId);
                });
    }

    private void mapRequestToTask(TaskRequest request, Task task, Client client) {
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setStatus(request.getStatus());
        task.setPriority(request.getPriority());
        task.setDeadline(request.getDeadline());
        task.setClient(client);
    }

    private TaskResponse toResponse(Task task) {
        TaskResponse response = new TaskResponse();
        response.setId(task.getId());
        response.setTitle(task.getTitle());
        response.setDescription(task.getDescription());
        response.setStatus(task.getStatus());
        response.setPriority(task.getPriority());
        response.setDeadline(task.getDeadline());
        response.setClientId(task.getClient().getId());
        return response;
    }
}
