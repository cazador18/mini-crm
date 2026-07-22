package com.evogroup.minicrm.repository;

import com.evogroup.minicrm.model.Task;
import com.evogroup.minicrm.model.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<Task, Long> {
    Page<Task> findAllByOrderByIdAsc(Pageable pageable);
    Page<Task> findByStatusOrderByIdAsc(TaskStatus status, Pageable pageable);
    Page<Task> findByClientIdOrderByIdAsc(Long clientId, Pageable pageable);
    Page<Task> findByStatusAndClientIdOrderByIdAsc(TaskStatus status, Long clientId, Pageable pageable);
    long countByStatus(TaskStatus status);
}
