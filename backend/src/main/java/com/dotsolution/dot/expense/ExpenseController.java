package com.dotsolution.dot.expense;

import com.dotsolution.dot.common.ApiResponse;
import com.dotsolution.dot.expense.entity.ExpenseClaim;
import com.dotsolution.dot.expense.entity.ExpenseReceipt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/expenses")
public class ExpenseController {

    @Autowired
    private ExpenseService expenseService;

    @GetMapping("/limits")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'HR_ADMIN', 'FINANCE_ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Double>>> getLimits() {
        return ResponseEntity.ok(ApiResponse.success(expenseService.getCategoryLimits(), "Category limits retrieved"));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'HR_ADMIN', 'FINANCE_ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<List<ExpenseClaim>>> getClaims(@RequestParam UUID employeeId) {
        return ResponseEntity.ok(ApiResponse.success(expenseService.getClaimsByEmployee(employeeId)));
    }

    @GetMapping("/manager")
    @PreAuthorize("hasAnyRole('MANAGER', 'HR_ADMIN')")
    public ResponseEntity<ApiResponse<List<ExpenseClaim>>> getManagerClaims(@RequestParam UUID managerId) {
        return ResponseEntity.ok(ApiResponse.success(expenseService.getManagerClaims(managerId)));
    }

    @GetMapping("/finance")
    @PreAuthorize("hasAnyRole('FINANCE_ADMIN', 'HR_ADMIN')")
    public ResponseEntity<ApiResponse<List<ExpenseClaim>>> getFinanceQueue() {
        return ResponseEntity.ok(ApiResponse.success(expenseService.getFinanceQueue()));
    }

    public static class ExpenseClaimDto {
        private ExpenseClaim claim;
        private List<ExpenseReceipt> receipts;

        public ExpenseClaim getClaim() {
            return claim;
        }

        public void setClaim(ExpenseClaim claim) {
            this.claim = claim;
        }

        public List<ExpenseReceipt> getReceipts() {
            return receipts;
        }

        public void setReceipts(List<ExpenseReceipt> receipts) {
            this.receipts = receipts;
        }
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'HR_ADMIN', 'FINANCE_ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<ExpenseClaim>> createClaim(@RequestBody ExpenseClaimDto dto) {
        ExpenseClaim created = expenseService.createClaim(dto.getClaim(), dto.getReceipts());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "Expense claim submitted successfully"));
    }

    @PostMapping("/{id}/approve-manager")
    @PreAuthorize("hasAnyRole('MANAGER', 'HR_ADMIN')")
    public ResponseEntity<ApiResponse<ExpenseClaim>> approveByManager(@PathVariable UUID id) {
        ExpenseClaim claim = expenseService.approveByManager(id);
        return ResponseEntity.ok(ApiResponse.success(claim, "Expense approved by manager"));
    }

    @PostMapping("/{id}/reject-manager")
    @PreAuthorize("hasAnyRole('MANAGER', 'HR_ADMIN')")
    public ResponseEntity<ApiResponse<ExpenseClaim>> rejectByManager(@PathVariable UUID id) {
        ExpenseClaim claim = expenseService.rejectByManager(id);
        return ResponseEntity.ok(ApiResponse.success(claim, "Expense rejected by manager"));
    }

    @PostMapping("/{id}/approve-finance")
    @PreAuthorize("hasRole('FINANCE_ADMIN')")
    public ResponseEntity<ApiResponse<ExpenseClaim>> approveByFinance(@PathVariable UUID id) {
        ExpenseClaim claim = expenseService.approveByFinance(id);
        return ResponseEntity.ok(ApiResponse.success(claim, "Expense approved and settled by finance"));
    }

    @PostMapping("/{id}/reject-finance")
    @PreAuthorize("hasRole('FINANCE_ADMIN')")
    public ResponseEntity<ApiResponse<ExpenseClaim>> rejectByFinance(@PathVariable UUID id) {
        ExpenseClaim claim = expenseService.rejectByFinance(id);
        return ResponseEntity.ok(ApiResponse.success(claim, "Expense rejected by finance"));
    }

    @GetMapping("/{id}/receipts")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'HR_ADMIN', 'FINANCE_ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<List<ExpenseReceipt>>> getReceipts(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(expenseService.getReceipts(id)));
    }
}
