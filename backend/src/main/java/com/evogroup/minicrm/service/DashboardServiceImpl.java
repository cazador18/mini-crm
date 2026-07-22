package com.evogroup.minicrm.service;

import com.evogroup.minicrm.dto.DashboardResponse;
import com.evogroup.minicrm.model.TaskStatus;
import com.evogroup.minicrm.repository.ClientRepository;
import com.evogroup.minicrm.repository.TaskRepository;
import com.evogroup.minicrm.repository.TaskStatusCount;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    private final ClientRepository clientRepository;
    private final TaskRepository taskRepository;

    public DashboardServiceImpl(ClientRepository clientRepository, TaskRepository taskRepository) {
        this.clientRepository = clientRepository;
        this.taskRepository = taskRepository;
    }

    @Override
    public DashboardResponse getStats() {
        DashboardResponse response = new DashboardResponse();
        response.setTotalClients(clientRepository.count());

        Map<TaskStatus, Long> counts = new EnumMap<>(TaskStatus.class);
        for (TaskStatus status : TaskStatus.values()) {
            counts.put(status, 0L);
        }
        for (TaskStatusCount row : taskRepository.countGroupedByStatus()) {
            counts.put(row.getStatus(), row.getCount());
        }
        response.setTasksByStatus(counts);

        return response;
    }
}
