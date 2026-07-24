package com.evogroup.minicrm.service;

import com.evogroup.minicrm.dto.PageResponse;
import com.evogroup.minicrm.dto.TaskRequest;
import com.evogroup.minicrm.dto.TaskResponse;
import com.evogroup.minicrm.exception.ClientNotFoundException;
import com.evogroup.minicrm.exception.InvalidTaskStatusTransitionException;
import com.evogroup.minicrm.exception.TaskNotFoundException;
import com.evogroup.minicrm.model.Client;
import com.evogroup.minicrm.model.Task;
import com.evogroup.minicrm.model.TaskPriority;
import com.evogroup.minicrm.model.TaskStatus;
import com.evogroup.minicrm.model.User;
import com.evogroup.minicrm.model.UserRole;
import com.evogroup.minicrm.repository.ClientRepository;
import com.evogroup.minicrm.repository.TaskRepository;
import com.evogroup.minicrm.security.CurrentUserService;
import com.evogroup.minicrm.security.OwnershipGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceImplTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private AuditService auditService;

    private TaskServiceImpl service;

    private User admin;
    private User manager;
    private User otherManager;
    private Client client;
    private Task task;
    private TaskRequest request;

    @BeforeEach
    void setUp() {
        service = new TaskServiceImpl(taskRepository, clientRepository, currentUserService, new OwnershipGuard(), auditService);

        admin = new User();
        admin.setId(100L);
        admin.setRole(UserRole.ADMIN);

        manager = new User();
        manager.setId(1L);
        manager.setRole(UserRole.MANAGER);

        otherManager = new User();
        otherManager.setId(2L);
        otherManager.setRole(UserRole.MANAGER);

        client = new Client();
        client.setId(1L);
        client.setName("Alice");
        client.setEmail("alice@example.com");
        client.setOwner(manager);

        task = new Task();
        task.setId(10L);
        task.setTitle("Fix bug");
        task.setDescription("Critical issue");
        task.setStatus(TaskStatus.NEW);
        task.setPriority(TaskPriority.HIGH);
        task.setDeadline(LocalDate.of(2026, 12, 31));
        task.setClient(client);

        request = new TaskRequest();
        request.setTitle("Fix bug");
        request.setDescription("Critical issue");
        request.setStatus(TaskStatus.NEW);
        request.setPriority(TaskPriority.HIGH);
        request.setDeadline(LocalDate.of(2026, 12, 31));
        request.setClientId(1L);
    }

    @Test
    void create_mapsRequestAndReturnsResponse_whenOwner() {
        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        when(currentUserService.getCurrentUser()).thenReturn(manager);
        when(taskRepository.save(any(Task.class))).thenReturn(task);

        TaskResponse response = service.create(request);

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getTitle()).isEqualTo("Fix bug");
        assertThat(response.getStatus()).isEqualTo(TaskStatus.NEW);
        assertThat(response.getClientId()).isEqualTo(1L);
        verify(taskRepository).save(any(Task.class));
        verify(auditService).log("CREATE", "TASK", 10L);
    }

    @Test
    void create_throwsAccessDenied_whenManagerNotOwnerOfClient() {
        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        when(currentUserService.getCurrentUser()).thenReturn(otherManager);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(AccessDeniedException.class);

        verify(taskRepository, never()).save(any());
    }

    @Test
    void create_throwsClientNotFound_whenClientMissing() {
        when(clientRepository.findById(99L)).thenReturn(Optional.empty());
        request.setClientId(99L);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(ClientNotFoundException.class)
                .hasMessageContaining("99");

        verify(taskRepository, never()).save(any());
    }

    @Test
    void findById_returnsResponse_whenOwner() {
        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));
        when(currentUserService.getCurrentUser()).thenReturn(manager);

        TaskResponse response = service.findById(10L);

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getTitle()).isEqualTo("Fix bug");
    }

    @Test
    void findById_throwsAccessDenied_whenManagerNotOwner() {
        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));
        when(currentUserService.getCurrentUser()).thenReturn(otherManager);

        assertThatThrownBy(() -> service.findById(10L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void findById_throwsNotFound_whenTaskMissing() {
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(99L))
                .isInstanceOf(TaskNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void findAll_admin_noFilter_returnsAll() {
        Pageable pageable = PageRequest.of(0, 20);
        when(currentUserService.getCurrentUser()).thenReturn(admin);
        when(taskRepository.findAllByOrderByIdAsc(pageable))
                .thenReturn(new PageImpl<>(List.of(task), pageable, 1));

        PageResponse<TaskResponse> result = service.findAll(null, null, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Fix bug");
        verify(taskRepository).findAllByOrderByIdAsc(pageable);
    }

    @Test
    void findAll_admin_byStatus_filtersCorrectly() {
        Pageable pageable = PageRequest.of(0, 20);
        when(currentUserService.getCurrentUser()).thenReturn(admin);
        when(taskRepository.findByStatusOrderByIdAsc(TaskStatus.NEW, pageable))
                .thenReturn(new PageImpl<>(List.of(task), pageable, 1));

        PageResponse<TaskResponse> result = service.findAll(TaskStatus.NEW, null, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getStatus()).isEqualTo(TaskStatus.NEW);
        verify(taskRepository).findByStatusOrderByIdAsc(TaskStatus.NEW, pageable);
    }

    @Test
    void findAll_admin_byClientId_filtersCorrectly() {
        Pageable pageable = PageRequest.of(0, 20);
        when(currentUserService.getCurrentUser()).thenReturn(admin);
        when(taskRepository.findByClientIdOrderByIdAsc(1L, pageable))
                .thenReturn(new PageImpl<>(List.of(task), pageable, 1));

        PageResponse<TaskResponse> result = service.findAll(null, 1L, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getClientId()).isEqualTo(1L);
        verify(taskRepository).findByClientIdOrderByIdAsc(1L, pageable);
    }

    @Test
    void findAll_admin_byStatusAndClientId_filtersCorrectly() {
        Pageable pageable = PageRequest.of(0, 20);
        when(currentUserService.getCurrentUser()).thenReturn(admin);
        when(taskRepository.findByStatusAndClientIdOrderByIdAsc(TaskStatus.NEW, 1L, pageable))
                .thenReturn(new PageImpl<>(List.of(task), pageable, 1));

        PageResponse<TaskResponse> result = service.findAll(TaskStatus.NEW, 1L, pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(taskRepository).findByStatusAndClientIdOrderByIdAsc(TaskStatus.NEW, 1L, pageable);
    }

    @Test
    void findAll_manager_noFilter_scopedToOwnClients() {
        Pageable pageable = PageRequest.of(0, 20);
        when(currentUserService.getCurrentUser()).thenReturn(manager);
        when(taskRepository.findByClientOwnerIdOrderByIdAsc(1L, pageable))
                .thenReturn(new PageImpl<>(List.of(task), pageable, 1));

        PageResponse<TaskResponse> result = service.findAll(null, null, pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(taskRepository).findByClientOwnerIdOrderByIdAsc(1L, pageable);
        verify(taskRepository, never()).findAllByOrderByIdAsc(any());
    }

    @Test
    void findAll_manager_byStatus_scopedToOwnClients() {
        Pageable pageable = PageRequest.of(0, 20);
        when(currentUserService.getCurrentUser()).thenReturn(manager);
        when(taskRepository.findByStatusAndClientOwnerIdOrderByIdAsc(TaskStatus.NEW, 1L, pageable))
                .thenReturn(new PageImpl<>(List.of(task), pageable, 1));

        PageResponse<TaskResponse> result = service.findAll(TaskStatus.NEW, null, pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(taskRepository).findByStatusAndClientOwnerIdOrderByIdAsc(TaskStatus.NEW, 1L, pageable);
    }

    @Test
    void findAll_manager_byClientId_scopedToOwnClients() {
        Pageable pageable = PageRequest.of(0, 20);
        when(currentUserService.getCurrentUser()).thenReturn(manager);
        when(taskRepository.findByClientIdAndClientOwnerIdOrderByIdAsc(1L, 1L, pageable))
                .thenReturn(new PageImpl<>(List.of(task), pageable, 1));

        PageResponse<TaskResponse> result = service.findAll(null, 1L, pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(taskRepository).findByClientIdAndClientOwnerIdOrderByIdAsc(1L, 1L, pageable);
    }

    @Test
    void findAll_manager_byStatusAndClientId_scopedToOwnClients() {
        Pageable pageable = PageRequest.of(0, 20);
        when(currentUserService.getCurrentUser()).thenReturn(manager);
        when(taskRepository.findByStatusAndClientIdAndClientOwnerIdOrderByIdAsc(TaskStatus.NEW, 1L, 1L, pageable))
                .thenReturn(new PageImpl<>(List.of(task), pageable, 1));

        PageResponse<TaskResponse> result = service.findAll(TaskStatus.NEW, 1L, pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(taskRepository).findByStatusAndClientIdAndClientOwnerIdOrderByIdAsc(TaskStatus.NEW, 1L, 1L, pageable);
    }

    @Test
    void update_updatesFieldsAndReturnsResponse_whenOwner() {
        TaskRequest updateRequest = new TaskRequest();
        updateRequest.setTitle("Fix bug v2");
        updateRequest.setStatus(TaskStatus.IN_PROGRESS);
        updateRequest.setPriority(TaskPriority.MEDIUM);
        updateRequest.setClientId(1L);

        Task updated = new Task();
        updated.setId(10L);
        updated.setTitle("Fix bug v2");
        updated.setStatus(TaskStatus.IN_PROGRESS);
        updated.setPriority(TaskPriority.MEDIUM);
        updated.setClient(client);

        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));
        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        when(currentUserService.getCurrentUser()).thenReturn(manager);
        when(taskRepository.save(task)).thenReturn(updated);

        TaskResponse response = service.update(10L, updateRequest);

        assertThat(response.getTitle()).isEqualTo("Fix bug v2");
        assertThat(response.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        verify(auditService).log("STATUS_CHANGE", "TASK", 10L);
    }

    @Test
    void update_throwsAccessDenied_whenManagerNotOwnerOfExistingTask() {
        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));
        when(currentUserService.getCurrentUser()).thenReturn(otherManager);

        assertThatThrownBy(() -> service.update(10L, request))
                .isInstanceOf(AccessDeniedException.class);

        verify(taskRepository, never()).save(any());
    }

    @Test
    void delete_deletesEntity_whenOwner() {
        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));
        when(currentUserService.getCurrentUser()).thenReturn(manager);

        service.delete(10L);

        verify(taskRepository).delete(task);
        verify(auditService).log("DELETE", "TASK", 10L);
    }

    @Test
    void delete_throwsAccessDenied_whenManagerNotOwner() {
        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));
        when(currentUserService.getCurrentUser()).thenReturn(otherManager);

        assertThatThrownBy(() -> service.delete(10L))
                .isInstanceOf(AccessDeniedException.class);

        verify(taskRepository, never()).delete(any());
    }

    @Test
    void delete_throwsNotFound_whenTaskMissing() {
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(99L))
                .isInstanceOf(TaskNotFoundException.class)
                .hasMessageContaining("99");

        verify(taskRepository, never()).delete(any());
    }

    // ── status transition rules ─────────────────────────────────────────────

    private Task taskWithStatus(TaskStatus status) {
        Task t = new Task();
        t.setId(10L);
        t.setTitle("Fix bug");
        t.setStatus(status);
        t.setPriority(TaskPriority.HIGH);
        t.setClient(client);
        return t;
    }

    private TaskRequest requestWithStatus(TaskStatus status) {
        TaskRequest r = new TaskRequest();
        r.setTitle("Fix bug");
        r.setStatus(status);
        r.setPriority(TaskPriority.HIGH);
        r.setClientId(1L);
        return r;
    }

    private void stubForUpdate(Task existing, User actingUser) {
        when(taskRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        when(currentUserService.getCurrentUser()).thenReturn(actingUser);
    }

    @Test
    void update_newToInProgress_succeeds() {
        Task existing = taskWithStatus(TaskStatus.NEW);
        TaskRequest req = requestWithStatus(TaskStatus.IN_PROGRESS);
        stubForUpdate(existing, manager);
        when(taskRepository.save(existing)).thenReturn(existing);

        TaskResponse response = service.update(10L, req);

        assertThat(response.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        verify(auditService).log("STATUS_CHANGE", "TASK", 10L);
    }

    @Test
    void update_inProgressToDone_succeeds() {
        Task existing = taskWithStatus(TaskStatus.IN_PROGRESS);
        TaskRequest req = requestWithStatus(TaskStatus.DONE);
        stubForUpdate(existing, manager);
        when(taskRepository.save(existing)).thenReturn(existing);

        TaskResponse response = service.update(10L, req);

        assertThat(response.getStatus()).isEqualTo(TaskStatus.DONE);
        verify(auditService).log("STATUS_CHANGE", "TASK", 10L);
    }

    @Test
    void update_doneToInProgress_asAdmin_succeeds() {
        Task existing = taskWithStatus(TaskStatus.DONE);
        TaskRequest req = requestWithStatus(TaskStatus.IN_PROGRESS);
        stubForUpdate(existing, admin);
        when(taskRepository.save(existing)).thenReturn(existing);

        TaskResponse response = service.update(10L, req);

        assertThat(response.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
    }

    @Test
    void update_doneToInProgress_asManager_throwsConflict() {
        Task existing = taskWithStatus(TaskStatus.DONE);
        TaskRequest req = requestWithStatus(TaskStatus.IN_PROGRESS);
        stubForUpdate(existing, manager);

        assertThatThrownBy(() -> service.update(10L, req))
                .isInstanceOf(InvalidTaskStatusTransitionException.class);

        verify(taskRepository, never()).save(any());
        verify(auditService, never()).log(any(), any(), any());
    }

    @Test
    void update_newToDone_throwsConflict_evenForAdmin() {
        Task existing = taskWithStatus(TaskStatus.NEW);
        TaskRequest req = requestWithStatus(TaskStatus.DONE);
        stubForUpdate(existing, admin);

        assertThatThrownBy(() -> service.update(10L, req))
                .isInstanceOf(InvalidTaskStatusTransitionException.class);

        verify(taskRepository, never()).save(any());
    }

    @Test
    void update_inProgressToNew_throwsConflict() {
        Task existing = taskWithStatus(TaskStatus.IN_PROGRESS);
        TaskRequest req = requestWithStatus(TaskStatus.NEW);
        stubForUpdate(existing, admin);

        assertThatThrownBy(() -> service.update(10L, req))
                .isInstanceOf(InvalidTaskStatusTransitionException.class);
    }

    @Test
    void update_doneToNew_throwsConflict() {
        Task existing = taskWithStatus(TaskStatus.DONE);
        TaskRequest req = requestWithStatus(TaskStatus.NEW);
        stubForUpdate(existing, admin);

        assertThatThrownBy(() -> service.update(10L, req))
                .isInstanceOf(InvalidTaskStatusTransitionException.class);
    }

    @Test
    void update_sameStatus_isNoOp_succeeds() {
        Task existing = taskWithStatus(TaskStatus.NEW);
        TaskRequest req = requestWithStatus(TaskStatus.NEW);
        stubForUpdate(existing, manager);
        when(taskRepository.save(existing)).thenReturn(existing);

        TaskResponse response = service.update(10L, req);

        assertThat(response.getStatus()).isEqualTo(TaskStatus.NEW);
        verify(auditService).log("UPDATE", "TASK", 10L);
        verify(auditService, never()).log(eq("STATUS_CHANGE"), any(), any());
    }
}
