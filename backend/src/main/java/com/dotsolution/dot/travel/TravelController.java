package com.dotsolution.dot.travel;

import com.dotsolution.dot.common.ApiResponse;
import com.dotsolution.dot.travel.entity.TravelRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/travel")
public class TravelController {

    @Autowired
    private TravelService travelService;

    @PostMapping("/requests")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'HR_ADMIN', 'FINANCE_ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<TravelRequest>> createRequest(@RequestBody TravelRequest request) {
        TravelRequest created = travelService.createTravelRequest(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "Travel request submitted successfully"));
    }

    @GetMapping("/requests/my")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'HR_ADMIN', 'FINANCE_ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<List<TravelRequest>>> getMyRequests(@RequestParam UUID employeeId) {
        return ResponseEntity.ok(ApiResponse.success(travelService.getTravelRequestsByEmployee(employeeId)));
    }

    @GetMapping("/requests/pending")
    @PreAuthorize("hasAnyRole('MANAGER', 'HR_ADMIN')")
    public ResponseEntity<ApiResponse<List<TravelRequest>>> getPendingRequests(@RequestParam UUID managerId) {
        return ResponseEntity.ok(ApiResponse.success(travelService.getTravelRequestsByManager(managerId)));
    }

    @GetMapping("/requests/all")
    @PreAuthorize("hasAnyRole('HR_ADMIN', 'FINANCE_ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<List<TravelRequest>>> getAllRequests() {
        return ResponseEntity.ok(ApiResponse.success(travelService.getAllTravelRequests()));
    }

    @PostMapping("/requests/{id}/approve")
    @PreAuthorize("hasAnyRole('MANAGER', 'HR_ADMIN')")
    public ResponseEntity<ApiResponse<TravelRequest>> approveRequest(@PathVariable UUID id) {
        TravelRequest approved = travelService.approveTravelRequest(id);
        return ResponseEntity.ok(ApiResponse.success(approved, "Travel request approved successfully"));
    }

    @PostMapping("/requests/{id}/reject")
    @PreAuthorize("hasAnyRole('MANAGER', 'HR_ADMIN')")
    public ResponseEntity<ApiResponse<TravelRequest>> rejectRequest(
            @PathVariable UUID id,
            @RequestParam String reason) {
        TravelRequest rejected = travelService.rejectTravelRequest(id, reason);
        return ResponseEntity.ok(ApiResponse.success(rejected, "Travel request rejected successfully"));
    }

    @PostMapping("/requests/{id}/book")
    @PreAuthorize("hasAnyRole('HR_ADMIN', 'FINANCE_ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<TravelRequest>> bookRequest(@PathVariable UUID id) {
        TravelRequest booked = travelService.bookTravelRequest(id);
        return ResponseEntity.ok(ApiResponse.success(booked, "Travel booking confirmed and ticket generated successfully"));
    }
}
