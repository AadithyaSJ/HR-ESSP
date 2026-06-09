package com.dotsolution.dot.expense.repository;

import com.dotsolution.dot.expense.entity.ExpenseReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface ExpenseReceiptRepository extends JpaRepository<ExpenseReceipt, UUID> {
    List<ExpenseReceipt> findByExpenseId(UUID expenseId);
    void deleteByExpenseId(UUID expenseId);
}
