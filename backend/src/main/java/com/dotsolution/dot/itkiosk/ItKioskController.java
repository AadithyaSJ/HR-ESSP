package com.dotsolution.dot.itkiosk;

import com.dotsolution.dot.common.ApiResponse;
import com.dotsolution.dot.itkiosk.entity.ItTicket;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/it-kiosk")
public class ItKioskController {

    @Autowired
    private ItKioskService itKioskService;

    @PostMapping("/tickets")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'HR_ADMIN', 'FINANCE_ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<ItTicket>> createTicket(@RequestBody ItTicket ticket) {
        ItTicket created = itKioskService.createTicket(ticket);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "IT Support ticket raised successfully"));
    }

    @GetMapping("/tickets/my")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'HR_ADMIN', 'FINANCE_ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<List<ItTicket>>> getMyTickets(@RequestParam UUID employeeId) {
        return ResponseEntity.ok(ApiResponse.success(itKioskService.getTicketsByEmployee(employeeId)));
    }

    @GetMapping("/tickets/all")
    @PreAuthorize("hasAnyRole('HR_ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<List<ItTicket>>> getAllTickets() {
        return ResponseEntity.ok(ApiResponse.success(itKioskService.getAllTickets()));
    }

    @PutMapping("/tickets/{id}/status")
    @PreAuthorize("hasAnyRole('HR_ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<ItTicket>> updateTicketStatus(
            @PathVariable UUID id,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String assignedTo,
            @RequestParam(required = false) String comment) {
        ItTicket updated = itKioskService.updateTicketStatus(id, status, assignedTo, comment);
        return ResponseEntity.ok(ApiResponse.success(updated, "IT Support ticket updated successfully"));
    }
}
