package com.evogroup.minicrm.service;

import com.evogroup.minicrm.dto.PageResponse;
import com.evogroup.minicrm.dto.TaskRequest;
import com.evogroup.minicrm.dto.TaskResponse;
import com.evogroup.minicrm.exception.ClientNotFoundException;
import com.evogroup.minicrm.exception.TaskNotFoundException;
import com.evogroup.minicrm.model.Client;
import com.evogroup.minicrm.model.Task;
import com.evogroup.minicrm.model.TaskPriority;
import com.evogroup.minicrm.model.TaskStatus;
import com.evogroup.minicrm.repository.ClientRepository;
import com.evogroup.minicrm.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceImplTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private ClientRepository clientRepository;

    @InjectMocks
    private TaskServiceImpl service;

    private Client client;
    private Task task;
    private TaskRequest request;

    @BeforeEach
    void setUp() {
        client = new Client();
        client.setId(1L);
        client.setName("Alice");
        client.setEmail("alice@example.com");

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
    void create_mapsRequestAndReturnsResponse() {
        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        when(taskRepository.save(any(Task.class))).thenReturn(task);

        TaskResponse response = service.create(request);

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getTitle()).isEqualTo("Fix bug");
        assertThat(response.getStatus()).isEqualTo(TaskStatus.NEW);
        assertThat(response.getClientId()).isEqualTo(1L);
        verify(taskRepository).save(any(Task.class));
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
    void findById_returnsResponse_whenTaskExists() {
        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));

        TaskResponse response = service.findById(10L);

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getTitle()).isEqualTo("Fix bug");
    }

    @Test
    void findById_throwsNotFound_whenTaskMissing() {
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(99L))
                .isInstanceOf(TaskNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void findAll_noFilter_returnsAll() {
        Pageable pageable = PageRequest.of(0, 20);
        when(taskRepository.findAllByOrderByIdAsc(pageable))
                .thenReturn(new PageImpl<>(List.of(task), pageable, 1));

        PageResponse<TaskResponse> result = service.findAll(null, null, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Fix bug");
        verify(taskRepository).findAllByOrderByIdAsc(pageable);
    }

    @Test
    void findAll_byStatus_filtersCorrectly() {
        Pageable pageable = PageRequest.of(0, 20);
        when(taskRepository.findByStatusOrderByIdAsc(TaskStatus.NEW, pageable))
                .thenReturn(new PageImpl<>(List.of(task), pageable, 1));

        PageResponse<TaskResponse> result = service.findAll(TaskStatus.NEW, null, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getStatus()).isEqualTo(TaskStatus.NEW);
        verify(taskRepository).findByStatusOrderByIdAsc(TaskStatus.NEW, pageable);
    }

    @Test
    void findAll_byClientId_filtersCorrectly() {
        Pageable pageable = PageRequest.of(0, 20);
        when(taskRepository.findByClientIdOrderByIdAsc(1L, pageable))
                .thenReturn(new PageImpl<>(List.of(task), pageable, 1));

        PageResponse<TaskResponse> result = service.findAll(null, 1L, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getClientId()).isEqualTo(1L);
        verify(taskRepository).findByClientIdOrderByIdAsc(1L, pageable);
    }

    @Test
    void findAll_byStatusAndClientId_filtersCorrectly() {
        Pageable pageable = PageRequest.of(0, 20);
        when(taskRepository.findByStatusAndClientIdOrderByIdAsc(TaskStatus.NEW, 1L, pageable))
                .thenReturn(new PageImpl<>(List.of(task), pageable, 1));

        PageResponse<TaskResponse> result = service.findAll(TaskStatus.NEW, 1L, pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(taskRepository).findByStatusAndClientIdOrderByIdAsc(TaskStatus.NEW, 1L, pageable);
    }

    @Test
    void update_updatesFieldsAndReturnsResponse() {
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
        when(taskRepository.save(task)).thenReturn(updated);

        TaskResponse response = service.update(10L, updateRequest);

        assertThat(response.getTitle()).isEqualTo("Fix bug v2");
        assertThat(response.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
    }

    @Test
    void delete_callsDeleteById_whenTaskExists() {
        service.delete(10L);

        verify(taskRepository).deleteById(10L);
    }

    @Test
    void delete_throwsEmptyResult_whenTaskMissing() {
        doThrow(new EmptyResultDataAccessException(1)).when(taskRepository).deleteById(99L);

        assertThatThrownBy(() -> service.delete(99L))
                .isInstanceOf(EmptyResultDataAccessException.class);
    }
}
