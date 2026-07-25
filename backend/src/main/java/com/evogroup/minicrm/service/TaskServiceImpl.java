package com.evogroup.minicrm.service;

import com.evogroup.minicrm.dto.PageResponse;
import com.evogroup.minicrm.dto.TaskRequest;
import com.evogroup.minicrm.dto.TaskResponse;
import com.evogroup.minicrm.exception.ClientNotFoundException;
import com.evogroup.minicrm.exception.InvalidTaskStatusTransitionException;
import com.evogroup.minicrm.exception.TaskNotFoundException;
import com.evogroup.minicrm.model.Client;
import com.evogroup.minicrm.model.Task;
import com.evogroup.minicrm.model.TaskStatus;
import com.evogroup.minicrm.model.User;
import com.evogroup.minicrm.model.UserRole;
import com.evogroup.minicrm.repository.ClientRepository;
import com.evogroup.minicrm.repository.TaskRepository;
import com.evogroup.minicrm.security.CurrentUserService;
import com.evogroup.minicrm.security.OwnershipGuard;
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
    private final CurrentUserService currentUserService;
    private final OwnershipGuard ownershipGuard;
    private final AuditService auditService;

    public TaskServiceImpl(TaskRepository taskRepository,
                            ClientRepository clientRepository,
                            CurrentUserService currentUserService,
                            OwnershipGuard ownershipGuard,
                            AuditService auditService) {
        this.taskRepository = taskRepository;
        this.clientRepository = clientRepository;
        this.currentUserService = currentUserService;
        this.ownershipGuard = ownershipGuard;
        this.auditService = auditService;
    }

    @Override
    public TaskResponse create(TaskRequest request) {
        Client client = findClientOrThrow(request.getClientId());
        ownershipGuard.check(currentUserService.getCurrentUser(), client);
        Task task = new Task();
        mapRequestToTask(request, task, client);
        Task saved = taskRepository.save(task);
        auditService.log("CREATE", "TASK", saved.getId());
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public TaskResponse findById(Long id) {
        Task task = findOrThrow(id);
        ownershipGuard.check(currentUserService.getCurrentUser(), task.getClient());
        return toResponse(task);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<TaskResponse> findAll(TaskStatus status, Long clientId, Pageable pageable) {
        User currentUser = currentUserService.getCurrentUser();
        Page<Task> tasks;
        if (currentUser.getRole() == UserRole.MANAGER) {
            Long ownerId = currentUser.getId();
            if (status != null && clientId != null) {
                tasks = taskRepository.findByStatusAndClientIdAndClientOwnerIdOrderByIdAsc(
                        status, clientId, ownerId, pageable);
            } else if (status != null) {
                tasks = taskRepository.findByStatusAndClientOwnerIdOrderByIdAsc(status, ownerId, pageable);
            } else if (clientId != null) {
                tasks = taskRepository.findByClientIdAndClientOwnerIdOrderByIdAsc(clientId, ownerId, pageable);
            } else {
                tasks = taskRepository.findByClientOwnerIdOrderByIdAsc(ownerId, pageable);
            }
        } else {
            if (status != null && clientId != null) {
                tasks = taskRepository.findByStatusAndClientIdOrderByIdAsc(status, clientId, pageable);
            } else if (status != null) {
                tasks = taskRepository.findByStatusOrderByIdAsc(status, pageable);
            } else if (clientId != null) {
                tasks = taskRepository.findByClientIdOrderByIdAsc(clientId, pageable);
            } else {
                tasks = taskRepository.findAllByOrderByIdAsc(pageable);
            }
        }
        return PageResponse.of(tasks.map(this::toResponse));
    }

    @Override
    public TaskResponse update(Long id, TaskRequest request) {
        User currentUser = currentUserService.getCurrentUser();
        Task task = findOrThrow(id);
        ownershipGuard.check(currentUser, task.getClient());
        Client client = findClientOrThrow(request.getClientId());
        ownershipGuard.check(currentUser, client);

        TaskStatus previousStatus = task.getStatus();
        validateStatusTransition(previousStatus, request.getStatus(), currentUser);

        mapRequestToTask(request, task, client);
        Task saved = taskRepository.save(task);

        if (previousStatus != saved.getStatus()) {
            auditService.log("STATUS_CHANGE", "TASK", saved.getId());
        } else {
            auditService.log("UPDATE", "TASK", saved.getId());
        }

        return toResponse(saved);
    }

    @Override
    public void delete(Long id) {
        Task task = findOrThrow(id);
        ownershipGuard.check(currentUserService.getCurrentUser(), task.getClient());
        taskRepository.delete(task);
        auditService.log("DELETE", "TASK", id);
    }

    private void validateStatusTransition(TaskStatus from, TaskStatus to, User currentUser) {
        if (from == to) {
            return;
        }
        boolean forward = (from == TaskStatus.NEW && to == TaskStatus.IN_PROGRESS)
                || (from == TaskStatus.IN_PROGRESS && to == TaskStatus.DONE);
        boolean adminReopen = from == TaskStatus.DONE && to == TaskStatus.IN_PROGRESS
                && currentUser.getRole() == UserRole.ADMIN;
        if (!forward && !adminReopen) {
            throw new InvalidTaskStatusTransitionException(from, to);
        }
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
