package com.dotsolution.dot.payroll;

import com.dotsolution.dot.common.EntityNotFoundException;
import com.dotsolution.dot.employee.entity.Employee;
import com.dotsolution.dot.employee.repository.EmployeeRepository;
import com.dotsolution.dot.payroll.entity.Payslip;
import com.dotsolution.dot.payroll.repository.PayslipRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class PayrollService {

    @Autowired
    private PayslipRepository payslipRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    public List<Payslip> getPayslipsForEmployee(UUID employeeId, boolean includeUnpublished) {
        if (includeUnpublished) {
            return payslipRepository.findByEmployeeId(employeeId);
        } else {
            return payslipRepository.findByEmployeeIdAndPublished(employeeId, true);
        }
    }

    public List<Payslip> getAllPayslips() {
        return payslipRepository.findAll();
    }

    public List<Payslip> importPayslipsFromCsv(String csvContent) throws Exception {
        List<Payslip> createdPayslips = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new StringReader(csvContent))) {
            String line;
            boolean isHeader = true;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                if (isHeader) {
                    isHeader = false;
                    continue;
                }
                String[] tokens = line.split(",");
                if (tokens.length < 6) {
                    throw new IllegalArgumentException("Invalid CSV row format: " + line);
                }
                String empCode = tokens[0].trim();
                String month = tokens[1].trim();
                int year = Integer.parseInt(tokens[2].trim());
                double gross = Double.parseDouble(tokens[3].trim());
                double deduction = Double.parseDouble(tokens[4].trim());
                String pdfUrl = tokens[5].trim();

                Employee emp = employeeRepository.findByEmployeeCode(empCode)
                        .orElseThrow(() -> new EntityNotFoundException("Employee code not found during CSV import: " + empCode));

                double net = gross - deduction;

                Payslip payslip = Payslip.builder()
                        .employeeId(emp.getId())
                        .month(month)
                        .year(year)
                        .grossPay(gross)
                        .deduction(deduction)
                        .netPay(net)
                        .pdfUrl(pdfUrl)
                        .published(false)
                        .build();

                createdPayslips.add(payslipRepository.save(payslip));
            }
        }
        return createdPayslips;
    }

    public void publishPayslips(String month, Integer year) {
        List<Payslip> payslips = payslipRepository.findByMonthAndYear(month, year);
        for (Payslip p : payslips) {
            p.setPublished(true);
            payslipRepository.save(p);
        }
    }
}
