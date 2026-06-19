package com.dotsolution.dot.travel.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "travel_requests")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TravelRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;

    @Column(nullable = false)
    private String purpose;

    @Column(nullable = false)
    private String destination;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "needs_ticket")
    @Builder.Default
    private Boolean needsTicket = false;

    @Column(name = "needs_accommodation")
    @Builder.Default
    private Boolean needsAccommodation = false;

    @Column(name = "accommodation_details", columnDefinition = "TEXT")
    private String accommodationDetails;

    @Column(name = "estimated_cost")
    @Builder.Default
    private Double estimatedCost = 0.0;

    @Column(length = 50)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "manager_id")
    private UUID managerId;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
