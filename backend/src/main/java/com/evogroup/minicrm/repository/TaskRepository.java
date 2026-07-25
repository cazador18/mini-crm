package com.evogroup.minicrm.repository;

import com.evogroup.minicrm.model.Task;
import com.evogroup.minicrm.model.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {
    Page<Task> findAllByOrderByIdAsc(Pageable pageable);
    Page<Task> findByStatusOrderByIdAsc(TaskStatus status, Pageable pageable);
    Page<Task> findByClientIdOrderByIdAsc(Long clientId, Pageable pageable);
    Page<Task> findByStatusAndClientIdOrderByIdAsc(TaskStatus status, Long clientId, Pageable pageable);

    Page<Task> findByClientOwnerIdOrderByIdAsc(Long ownerId, Pageable pageable);
    Page<Task> findByStatusAndClientOwnerIdOrderByIdAsc(TaskStatus status, Long ownerId, Pageable pageable);
    Page<Task> findByClientIdAndClientOwnerIdOrderByIdAsc(Long clientId, Long ownerId, Pageable pageable);
    Page<Task> findByStatusAndClientIdAndClientOwnerIdOrderByIdAsc(
            TaskStatus status, Long clientId, Long ownerId, Pageable pageable);

    @Query("select t.status as status, count(t) as count from Task t group by t.status")
    List<TaskStatusCount> countGroupedByStatus();
}
