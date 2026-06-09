package com.dotsolution.dot.payroll;

import com.dotsolution.dot.common.ApiResponse;
import com.dotsolution.dot.payroll.entity.Payslip;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payroll")
public class PayrollController {

    @Autowired
    private PayrollService payrollService;

    @GetMapping("/employee/{employeeId}")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'HR_ADMIN', 'FINANCE_ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<List<Payslip>>> getEmployeePayslips(
            @PathVariable UUID employeeId,
            @RequestParam(defaultValue = "false") boolean includeUnpublished) {
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean hasAdminRole = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_HR_ADMIN") || a.getAuthority().equals("ROLE_FINANCE_ADMIN"));
        
        boolean actualInclude = includeUnpublished && hasAdminRole;
        
        return ResponseEntity.ok(ApiResponse.success(payrollService.getPayslipsForEmployee(employeeId, actualInclude)));
    }

    @PostMapping("/upload")
    @PreAuthorize("hasAnyRole('FINANCE_ADMIN', 'HR_ADMIN')")
    public ResponseEntity<ApiResponse<List<Payslip>>> uploadPayslips(@RequestBody String csvContent) {
        try {
            List<Payslip> imported = payrollService.importPayslipsFromCsv(csvContent);
            return ResponseEntity.ok(ApiResponse.success(imported, "Payslips parsed and imported successfully. Status: UNPUBLISHED"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to parse CSV: " + e.getMessage()));
        }
    }

    @PostMapping("/publish")
    @PreAuthorize("hasAnyRole('FINANCE_ADMIN', 'HR_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> publishPayslips(
            @RequestParam String month,
            @RequestParam Integer year) {
        payrollService.publishPayslips(month, year);
        return ResponseEntity.ok(ApiResponse.success(null, "Payslips published successfully for " + month + " " + year));
    }
}
