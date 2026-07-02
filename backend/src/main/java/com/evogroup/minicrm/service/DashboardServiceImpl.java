package com.evogroup.minicrm.service;

import com.evogroup.minicrm.dto.DashboardResponse;
import com.evogroup.minicrm.model.TaskStatus;
import com.evogroup.minicrm.repository.ClientRepository;
import com.evogroup.minicrm.repository.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        response.setTasksByStatus(Map.of(
                TaskStatus.NEW,         taskRepository.countByStatus(TaskStatus.NEW),
                TaskStatus.IN_PROGRESS, taskRepository.countByStatus(TaskStatus.IN_PROGRESS),
                TaskStatus.DONE,        taskRepository.countByStatus(TaskStatus.DONE)
        ));
        return response;
    }
}
