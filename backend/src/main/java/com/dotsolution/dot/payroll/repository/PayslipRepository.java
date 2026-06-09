package com.dotsolution.dot.payroll.repository;

import com.dotsolution.dot.payroll.entity.Payslip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface PayslipRepository extends JpaRepository<Payslip, UUID> {
    List<Payslip> findByEmployeeId(UUID employeeId);
    List<Payslip> findByEmployeeIdAndPublished(UUID employeeId, Boolean published);
    List<Payslip> findByMonthAndYear(String month, Integer year);
}
