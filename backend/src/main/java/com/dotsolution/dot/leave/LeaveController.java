package com.dotsolution.dot.leave;

import com.dotsolution.dot.common.ApiResponse;
import com.dotsolution.dot.leave.entity.LeaveBalance;
import com.dotsolution.dot.leave.entity.LeaveRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/leaves")
public class LeaveController {

    @Autowired
    private LeaveService leaveService;

    @GetMapping("/balances")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'HR_ADMIN', 'FINANCE_ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<List<LeaveBalance>>> getBalances(@RequestParam UUID employeeId) {
        return ResponseEntity.ok(ApiResponse.success(leaveService.getLeaveBalances(employeeId)));
    }

    @GetMapping("/requests")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'HR_ADMIN', 'FINANCE_ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<List<LeaveRequest>>> getRequests(@RequestParam UUID employeeId) {
        return ResponseEntity.ok(ApiResponse.success(leaveService.getLeaveRequests(employeeId)));
    }

    @GetMapping("/requests/manager")
    @PreAuthorize("hasAnyRole('MANAGER', 'HR_ADMIN')")
    public ResponseEntity<ApiResponse<List<LeaveRequest>>> getManagerRequests(@RequestParam UUID managerId) {
        return ResponseEntity.ok(ApiResponse.success(leaveService.getManagerLeaveRequests(managerId)));
    }

    @PostMapping("/requests")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'HR_ADMIN', 'FINANCE_ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<LeaveRequest>> createRequest(@RequestBody LeaveRequest request) {
        LeaveRequest created = leaveService.createLeaveRequest(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "Leave request submitted successfully"));
    }

    @PostMapping("/requests/{id}/approve")
    @PreAuthorize("hasAnyRole('MANAGER', 'HR_ADMIN')")
    public ResponseEntity<ApiResponse<LeaveRequest>> approveRequest(
            @PathVariable UUID id, 
            @RequestParam(required = false) String comment) {
        LeaveRequest approved = leaveService.approveLeaveRequest(id, comment);
        return ResponseEntity.ok(ApiResponse.success(approved, "Leave request approved successfully"));
    }

    @PostMapping("/requests/{id}/reject")
    @PreAuthorize("hasAnyRole('MANAGER', 'HR_ADMIN')")
    public ResponseEntity<ApiResponse<LeaveRequest>> rejectRequest(
            @PathVariable UUID id, 
            @RequestParam(required = false) String comment) {
        LeaveRequest rejected = leaveService.rejectLeaveRequest(id, comment);
        return ResponseEntity.ok(ApiResponse.success(rejected, "Leave request rejected successfully"));
    }

    @PostMapping("/requests/{id}/cancel")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'HR_ADMIN', 'FINANCE_ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<LeaveRequest>> cancelRequest(@PathVariable UUID id) {
        LeaveRequest cancelled = leaveService.cancelLeaveRequest(id);
        return ResponseEntity.ok(ApiResponse.success(cancelled, "Leave request cancelled successfully"));
    }

    @GetMapping("/holidays")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'HR_ADMIN', 'FINANCE_ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getHolidays() {
        List<Map<String, Object>> holidays = new ArrayList<>();
        
        holidays.add(createHoliday("New Year's Day", "2026-01-01", "National"));
        holidays.add(createHoliday("Good Friday", "2026-04-03", "National"));
        holidays.add(createHoliday("Labour Day", "2026-05-01", "Regional"));
        holidays.add(createHoliday("Independence Day", "2026-08-15", "National"));
        holidays.add(createHoliday("Gandhi Jayanti", "2026-10-02", "National"));
        holidays.add(createHoliday("Christmas Day", "2026-12-25", "National"));
        
        return ResponseEntity.ok(ApiResponse.success(holidays, "Holiday list retrieved"));
    }

    private Map<String, Object> createHoliday(String name, String date, String type) {
        Map<String, Object> h = new HashMap<>();
        h.put("name", name);
        h.put("date", date);
        h.put("type", type);
        return h;
    }
}
