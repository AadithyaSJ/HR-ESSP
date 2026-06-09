package com.dotsolution.dot.expense.repository;

import com.dotsolution.dot.expense.entity.ExpenseClaim;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface ExpenseClaimRepository extends JpaRepository<ExpenseClaim, UUID> {
    List<ExpenseClaim> findByEmployeeId(UUID employeeId);
    List<ExpenseClaim> findByEmployeeIdIn(List<UUID> employeeIds);
    List<ExpenseClaim> findByStatus(String status);
    List<ExpenseClaim> findByStatusIn(List<String> statuses);
}
