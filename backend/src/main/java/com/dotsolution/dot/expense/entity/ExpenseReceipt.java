package com.dotsolution.dot.expense.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Entity
@Table(name = "expense_receipts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseReceipt {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "expense_id", nullable = false)
    private UUID expenseId;

    @Column(name = "s3_url", nullable = false, length = 1000)
    private String s3Url;

    @Column(name = "file_name", nullable = false)
    private String fileName;
}
