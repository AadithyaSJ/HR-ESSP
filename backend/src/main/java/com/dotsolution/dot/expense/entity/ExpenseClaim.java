package com.dotsolution.dot.expense.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "expense_claims")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseClaim {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;

    @Column(nullable = false, length = 100)
    private String category;

    @Column(nullable = false)
    @Builder.Default
    private Double amount = 0.0;

    @Column(nullable = false, length = 10)
    @Builder.Default
    private String currency = "INR";

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 50)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
