package com.evogroup.minicrm.repository;

import com.evogroup.minicrm.model.Task;
import com.evogroup.minicrm.model.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findAllByOrderByIdAsc();
    List<Task> findByStatusOrderByIdAsc(TaskStatus status);
    List<Task> findByClientIdOrderByIdAsc(Long clientId);
    List<Task> findByStatusAndClientIdOrderByIdAsc(TaskStatus status, Long clientId);
    long countByStatus(TaskStatus status);
}
