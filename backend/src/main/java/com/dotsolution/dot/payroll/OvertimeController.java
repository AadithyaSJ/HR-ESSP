package com.dotsolution.dot.payroll;

import com.dotsolution.dot.common.ApiResponse;
import com.dotsolution.dot.payroll.entity.OvertimeRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/overtime")
public class OvertimeController {

    @Autowired
    private OvertimeService overtimeService;

    @PostMapping("/records")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'HR_ADMIN', 'FINANCE_ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<OvertimeRecord>> logOvertime(@RequestBody OvertimeRecord record) {
        OvertimeRecord created = overtimeService.logOvertime(record);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "Overtime logged successfully"));
    }

    @GetMapping("/records/my")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'HR_ADMIN', 'FINANCE_ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<List<OvertimeRecord>>> getMyOvertime(@RequestParam UUID employeeId) {
        return ResponseEntity.ok(ApiResponse.success(overtimeService.getOvertimeByEmployee(employeeId)));
    }

    @GetMapping("/records/pending")
    @PreAuthorize("hasAnyRole('MANAGER', 'HR_ADMIN', 'FINANCE_ADMIN')")
    public ResponseEntity<ApiResponse<List<OvertimeRecord>>> getPendingOvertime() {
        return ResponseEntity.ok(ApiResponse.success(overtimeService.getPendingOvertime()));
    }

    @PostMapping("/records/{id}/approve")
    @PreAuthorize("hasAnyRole('MANAGER', 'HR_ADMIN')")
    public ResponseEntity<ApiResponse<OvertimeRecord>> approveOvertime(
            @PathVariable UUID id,
            @RequestParam UUID managerId) {
        OvertimeRecord approved = overtimeService.approveOvertime(id, managerId);
        return ResponseEntity.ok(ApiResponse.success(approved, "Overtime record approved successfully"));
    }

    @PostMapping("/records/{id}/reject")
    @PreAuthorize("hasAnyRole('MANAGER', 'HR_ADMIN')")
    public ResponseEntity<ApiResponse<OvertimeRecord>> rejectOvertime(
            @PathVariable UUID id,
            @RequestParam UUID managerId) {
        OvertimeRecord rejected = overtimeService.rejectOvertime(id, managerId);
        return ResponseEntity.ok(ApiResponse.success(rejected, "Overtime record rejected successfully"));
    }
}
