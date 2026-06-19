package com.dotsolution.dot.employee.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "resignations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Resignation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;

    @Column(name = "submission_date", nullable = false)
    private LocalDate submissionDate;

    @Column(name = "requested_last_working_day", nullable = false)
    private LocalDate requestedLastWorkingDay;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(length = 50)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "approved_last_working_day")
    private LocalDate approvedLastWorkingDay;
}
