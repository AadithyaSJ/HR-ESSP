package com.dotsolution.dot.task.repository;

import com.dotsolution.dot.task.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID> {
    List<Task> findByEmployeeId(UUID employeeId);
    List<Task> findByAssignedBy(UUID assignedBy);
}
