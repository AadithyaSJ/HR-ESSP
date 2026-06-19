package com.dotsolution.dot.task;

import com.dotsolution.dot.common.ApiResponse;
import com.dotsolution.dot.task.entity.Task;
import com.dotsolution.dot.task.repository.TaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tasks")
public class TaskController {

    @Autowired
    private TaskRepository taskRepository;

    @PostMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'HR_ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<Task>> assignTask(@RequestBody Task task) {
        Task created = taskRepository.save(task);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "Task assigned successfully"));
    }

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'HR_ADMIN', 'FINANCE_ADMIN', 'SYSTEM_ADMIN', 'IT_SUPPORT')")
    public ResponseEntity<ApiResponse<List<Task>>> getMyTasks(@RequestParam UUID employeeId) {
        return ResponseEntity.ok(ApiResponse.success(taskRepository.findByEmployeeId(employeeId)));
    }

    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('HR_ADMIN', 'SYSTEM_ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<Task>>> getAllTasks() {
        return ResponseEntity.ok(ApiResponse.success(taskRepository.findAll()));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'HR_ADMIN', 'FINANCE_ADMIN', 'SYSTEM_ADMIN', 'IT_SUPPORT')")
    public ResponseEntity<ApiResponse<Task>> updateTaskStatus(
            @PathVariable UUID id,
            @RequestParam String status) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Task not found"));
        task.setStatus(status);
        Task updated = taskRepository.save(task);
        return ResponseEntity.ok(ApiResponse.success(updated, "Task status updated successfully"));
    }
}
