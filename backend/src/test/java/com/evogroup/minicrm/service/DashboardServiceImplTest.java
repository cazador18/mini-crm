package com.evogroup.minicrm.service;

import com.evogroup.minicrm.dto.DashboardResponse;
import com.evogroup.minicrm.model.TaskStatus;
import com.evogroup.minicrm.repository.ClientRepository;
import com.evogroup.minicrm.repository.TaskRepository;
import com.evogroup.minicrm.repository.TaskStatusCount;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTest {

    @Mock ClientRepository clientRepository;
    @Mock TaskRepository   taskRepository;

    @InjectMocks DashboardServiceImpl service;

    private record Row(TaskStatus status, Long count) implements TaskStatusCount {
        @Override public TaskStatus getStatus() { return status; }
        @Override public Long getCount() { return count; }
    }

    @Test
    void getStats_returnsCorrectCounters() {
        when(clientRepository.count()).thenReturn(3L);
        when(taskRepository.countGroupedByStatus()).thenReturn(List.of(
                new Row(TaskStatus.NEW, 2L),
                new Row(TaskStatus.IN_PROGRESS, 1L),
                new Row(TaskStatus.DONE, 4L)
        ));

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
        when(taskRepository.countGroupedByStatus()).thenReturn(List.of());

        DashboardResponse stats = service.getStats();

        assertThat(stats.getTotalClients()).isZero();
        assertThat(stats.getTasksByStatus().values()).containsOnly(0L);
    }
}
