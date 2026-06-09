package com.dotsolution.dot.payroll.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Entity
@Table(name = "payslips")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payslip {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;

    @Column(nullable = false, length = 20)
    private String month;

    @Column(nullable = false)
    private Integer year;

    @Column(name = "gross_pay", nullable = false)
    @Builder.Default
    private Double grossPay = 0.0;

    @Column(nullable = false)
    @Builder.Default
    private Double deduction = 0.0;

    @Column(name = "net_pay", nullable = false)
    @Builder.Default
    private Double netPay = 0.0;

    @Column(name = "pdf_url", length = 1000)
    private String pdfUrl;

    @Builder.Default
    private Boolean published = false;
}
