package com.evogroup.minicrm.service;

import com.evogroup.minicrm.dto.DashboardResponse;
import com.evogroup.minicrm.model.TaskStatus;
import com.evogroup.minicrm.repository.ClientRepository;
import com.evogroup.minicrm.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTest {

    @Mock ClientRepository clientRepository;
    @Mock TaskRepository   taskRepository;

    @InjectMocks DashboardServiceImpl service;

    @Test
    void getStats_returnsCorrectCounters() {
        when(clientRepository.count()).thenReturn(3L);
        when(taskRepository.countByStatus(TaskStatus.NEW)).thenReturn(2L);
        when(taskRepository.countByStatus(TaskStatus.IN_PROGRESS)).thenReturn(1L);
        when(taskRepository.countByStatus(TaskStatus.DONE)).thenReturn(4L);

        DashboardResponse stats = service.getStats();

        assertThat(stats.getTotalClients()).isEqualTo(3L);
        assertThat(stats.getTasksByStatus())
                .containsEntry(TaskStatus.NEW, 2L)
                .containsEntry(TaskStatus.IN_PROGRESS, 1L)
                .containsEntry(TaskStatus.DONE, 4L);
    }

    @Test
    void getStats_withNoData_returnsZeroes() {
        when(clientRepository.count()).thenReturn(0L);
        when(taskRepository.countByStatus(TaskStatus.NEW)).thenReturn(0L);
        when(taskRepository.countByStatus(TaskStatus.IN_PROGRESS)).thenReturn(0L);
        when(taskRepository.countByStatus(TaskStatus.DONE)).thenReturn(0L);

        DashboardResponse stats = service.getStats();

        assertThat(stats.getTotalClients()).isZero();
        assertThat(stats.getTasksByStatus().values()).containsOnly(0L);
    }
}
