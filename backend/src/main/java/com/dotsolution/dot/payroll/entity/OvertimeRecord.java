package com.dotsolution.dot.payroll.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "overtime_records")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OvertimeRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private Double hours;

    @Column(columnDefinition = "TEXT")
    private String purpose;

    @Column(length = 50)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "approved_by")
    private UUID approvedBy;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
