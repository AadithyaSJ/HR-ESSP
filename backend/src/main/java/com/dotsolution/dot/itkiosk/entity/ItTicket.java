package com.dotsolution.dot.itkiosk.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "it_tickets")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 100)
    private String category;

    @Column(length = 50)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "assigned_to")
    private String assignedTo;

    @Column(columnDefinition = "TEXT")
    private String comments;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
