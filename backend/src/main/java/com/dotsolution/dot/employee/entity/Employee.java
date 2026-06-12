package com.dotsolution.dot.employee.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "employees")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "employee_code", nullable = false, unique = true)
    private String employeeCode;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    private String phone;
    private String address;
    private String department;
    private String designation;

    @Column(name = "bank_account_no")
    private String bankAccountNo;

    @Column(name = "bank_ifsc")
    private String bankIfsc;

    @Column(name = "bank_name")
    private String bankName;

    @Column(name = "emergency_name")
    private String emergencyName;

    @Column(name = "emergency_relation")
    private String emergencyRelation;

    @Column(name = "emergency_phone")
    private String emergencyPhone;

    @Column(name = "onboarding_percent")
    @Builder.Default
    private Integer onboardingPercent = 0;

    @Column(name = "manager_id")
    private UUID managerId;

    @Column(name = "joining_date", nullable = false)
    private LocalDate joiningDate;

    @Column(length = 50)
    @Builder.Default
    private String status = "ACTIVE";

    private Double salary;

    @Column(name = "salary_band")
    private String salaryBand;
}
