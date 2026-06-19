package com.dotsolution.dot.employee;

import com.dotsolution.dot.common.ApiResponse;
import com.dotsolution.dot.employee.entity.Resignation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/resignations")
public class ResignationController {

    @Autowired
    private ResignationService resignationService;

    @PostMapping
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'HR_ADMIN', 'FINANCE_ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<Resignation>> submitResignation(@RequestBody Resignation resignation) {
        Resignation submitted = resignationService.submitResignation(resignation);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(submitted, "Resignation request submitted successfully"));
    }

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'HR_ADMIN', 'FINANCE_ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<List<Resignation>>> getMyResignations(@RequestParam UUID employeeId) {
        return ResponseEntity.ok(ApiResponse.success(resignationService.getResignationsByEmployee(employeeId)));
    }

    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('HR_ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<List<Resignation>>> getAllResignations() {
        return ResponseEntity.ok(ApiResponse.success(resignationService.getAllResignations()));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('HR_ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<Resignation>> approveResignation(
            @PathVariable UUID id,
            @RequestParam(required = false) String approvedLwd) {
        LocalDate lwd = null;
        if (approvedLwd != null && !approvedLwd.trim().isEmpty()) {
            lwd = LocalDate.parse(approvedLwd);
        }
        Resignation approved = resignationService.approveResignation(id, lwd);
        return ResponseEntity.ok(ApiResponse.success(approved, "Resignation approved. Employee is now serving notice period."));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('HR_ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<Resignation>> rejectResignation(
            @PathVariable UUID id,
            @RequestParam String reason) {
        Resignation rejected = resignationService.rejectResignation(id, reason);
        return ResponseEntity.ok(ApiResponse.success(rejected, "Resignation rejected successfully"));
    }
}
