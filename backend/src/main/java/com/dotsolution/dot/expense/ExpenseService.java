package com.dotsolution.dot.expense;

import com.dotsolution.dot.common.EntityNotFoundException;
import com.dotsolution.dot.employee.entity.Employee;
import com.dotsolution.dot.employee.repository.EmployeeRepository;
import com.dotsolution.dot.expense.entity.ExpenseClaim;
import com.dotsolution.dot.expense.entity.ExpenseReceipt;
import com.dotsolution.dot.expense.repository.ExpenseClaimRepository;
import com.dotsolution.dot.expense.repository.ExpenseReceiptRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class ExpenseService {

    @Autowired
    private ExpenseClaimRepository expenseClaimRepository;

    @Autowired
    private ExpenseReceiptRepository expenseReceiptRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    private static final Map<String, Double> CATEGORY_LIMITS = new HashMap<>();
    static {
        CATEGORY_LIMITS.put("TRAVEL", 25000.0);
        CATEGORY_LIMITS.put("MEALS", 5000.0);
        CATEGORY_LIMITS.put("EQUIPMENT", 50000.0);
        CATEGORY_LIMITS.put("OTHER", 10000.0);
    }

    public Map<String, Double> getCategoryLimits() {
        return CATEGORY_LIMITS;
    }

    public List<ExpenseClaim> getClaimsByEmployee(UUID employeeId) {
        return expenseClaimRepository.findByEmployeeId(employeeId);
    }

    public List<ExpenseClaim> getManagerClaims(UUID managerId) {
        List<Employee> reports = employeeRepository.findByManagerId(managerId);
        if (reports.isEmpty()) {
            return Collections.emptyList();
        }
        List<UUID> reportIds = reports.stream().map(Employee::getId).collect(Collectors.toList());
        return expenseClaimRepository.findByEmployeeIdIn(reportIds);
    }

    public List<ExpenseClaim> getFinanceQueue() {
        return expenseClaimRepository.findByStatusIn(Arrays.asList("APPROVED_BY_MANAGER", "PENDING_FINANCE"));
    }

    public ExpenseClaim createClaim(ExpenseClaim claim, List<ExpenseReceipt> receipts) {
        String categoryUpper = claim.getCategory().toUpperCase();
        Double limit = CATEGORY_LIMITS.getOrDefault(categoryUpper, CATEGORY_LIMITS.get("OTHER"));
        
        if (claim.getAmount() > limit) {
            throw new IllegalArgumentException("Expense amount " + claim.getAmount() + " exceeds the limit of " + limit + " for category: " + claim.getCategory());
        }

        claim.setStatus("PENDING");
        ExpenseClaim savedClaim = expenseClaimRepository.save(claim);

        if (receipts != null && !receipts.isEmpty()) {
            for (ExpenseReceipt receipt : receipts) {
                receipt.setExpenseId(savedClaim.getId());
                expenseReceiptRepository.save(receipt);
            }
        }

        return savedClaim;
    }

    public ExpenseClaim approveByManager(UUID id) {
        ExpenseClaim claim = expenseClaimRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Expense claim not found: " + id));

        if (!"PENDING".equalsIgnoreCase(claim.getStatus())) {
            throw new IllegalStateException("Expense claim must be in PENDING status for manager approval");
        }

        claim.setStatus("APPROVED_BY_MANAGER");
        return expenseClaimRepository.save(claim);
    }

    public ExpenseClaim rejectByManager(UUID id) {
        ExpenseClaim claim = expenseClaimRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Expense claim not found: " + id));

        if (!"PENDING".equalsIgnoreCase(claim.getStatus())) {
            throw new IllegalStateException("Expense claim must be in PENDING status to reject");
        }

        claim.setStatus("REJECTED");
        return expenseClaimRepository.save(claim);
    }

    public ExpenseClaim approveByFinance(UUID id) {
        ExpenseClaim claim = expenseClaimRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Expense claim not found: " + id));

        if (!"APPROVED_BY_MANAGER".equalsIgnoreCase(claim.getStatus()) && !"PENDING_FINANCE".equalsIgnoreCase(claim.getStatus())) {
            throw new IllegalStateException("Expense claim must be approved by manager before finance settling");
        }

        claim.setStatus("APPROVED");
        return expenseClaimRepository.save(claim);
    }

    public ExpenseClaim rejectByFinance(UUID id) {
        ExpenseClaim claim = expenseClaimRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Expense claim not found: " + id));

        if (!"APPROVED_BY_MANAGER".equalsIgnoreCase(claim.getStatus()) && !"PENDING_FINANCE".equalsIgnoreCase(claim.getStatus())) {
            throw new IllegalStateException("Expense claim must be approved by manager before finance review");
        }

        claim.setStatus("REJECTED_BY_FINANCE");
        return expenseClaimRepository.save(claim);
    }

    public List<ExpenseReceipt> getReceipts(UUID expenseId) {
        return expenseReceiptRepository.findByExpenseId(expenseId);
    }
}
