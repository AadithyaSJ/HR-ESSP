package com.dotsolution.dot.payroll;

import com.dotsolution.dot.common.EntityNotFoundException;
import com.dotsolution.dot.employee.entity.Employee;
import com.dotsolution.dot.employee.repository.EmployeeRepository;
import com.dotsolution.dot.payroll.entity.Payslip;
import com.dotsolution.dot.payroll.repository.PayslipRepository;
import com.dotsolution.dot.leave.LeaveService;
import com.dotsolution.dot.leave.entity.LeaveBalance;
import com.dotsolution.dot.leave.entity.LeaveRequest;
import com.dotsolution.dot.leave.repository.LeaveBalanceRepository;
import com.dotsolution.dot.leave.repository.LeaveRequestRepository;
import com.dotsolution.dot.expense.entity.ExpenseClaim;
import com.dotsolution.dot.expense.repository.ExpenseClaimRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.StringReader;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class PayrollService {

    @Autowired
    private PayslipRepository payslipRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private LeaveRequestRepository leaveRequestRepository;

    @Autowired
    private LeaveBalanceRepository leaveBalanceRepository;

    @Autowired
    private ExpenseClaimRepository expenseClaimRepository;

    @Autowired
    private LeaveService leaveService;

    @Autowired
    private com.dotsolution.dot.payroll.repository.OvertimeRecordRepository overtimeRecordRepository;

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

    public List<Payslip> generatePayslips(String month, Integer year) {
        List<Employee> employees = employeeRepository.findAll();
        List<Payslip> generated = new ArrayList<>();

        for (Employee emp : employees) {
            if (!"ACTIVE".equalsIgnoreCase(emp.getStatus())) {
                continue;
            }

            // Check if published payslip already exists
            List<Payslip> existing = payslipRepository.findByEmployeeId(emp.getId());
            boolean hasPublished = false;
            Payslip draftToOverwrite = null;
            for (Payslip p : existing) {
                if (p.getMonth().equalsIgnoreCase(month) && p.getYear().equals(year)) {
                    if (p.getPublished()) {
                        hasPublished = true;
                    } else {
                        draftToOverwrite = p;
                    }
                }
            }

            if (hasPublished) {
                continue;
            }

            double baseMonthlySalary = emp.getSalary() != null ? emp.getSalary() / 12.0 : 80000.0;

            // Calculate unpaid leaves / leaves taken over limit
            double unpaidLeaveDays = calculateUnpaidLeaveDays(emp, month, year);
            double leaveDeduction = Math.round(unpaidLeaveDays * (baseMonthlySalary / 30.0));

            // Calculate approved expense claims
            double approvedExpenses = calculateApprovedExpenses(emp.getId(), month, year);

            // Calculate approved overtime pay
            double overtimePay = calculateApprovedOvertimePay(emp.getId(), month, year, baseMonthlySalary);

            // Gross Pay = baseMonthlySalary - leaveDeduction + approvedExpenses + overtimePay
            double gross = Math.max(0.0, Math.round(baseMonthlySalary - leaveDeduction + approvedExpenses + overtimePay));

            // Deduction = 12% of basic taxable salary (includes overtime, excludes expenses)
            double taxableSalary = Math.max(0.0, baseMonthlySalary - leaveDeduction + overtimePay);
            double deduction = Math.round(taxableSalary * 0.12);

            // Net Pay = Gross Pay - Deduction
            double net = Math.max(0.0, gross - deduction);

            String pdfUrl = "http://localhost:9000/payslips/" + emp.getEmployeeCode().toLowerCase() + "-" + month.toLowerCase() + "-" + year + ".pdf";

            Payslip payslip;
            if (draftToOverwrite != null) {
                payslip = draftToOverwrite;
                payslip.setGrossPay(gross);
                payslip.setDeduction(deduction);
                payslip.setNetPay(net);
                payslip.setPdfUrl(pdfUrl);
            } else {
                payslip = Payslip.builder()
                        .employeeId(emp.getId())
                        .month(month)
                        .year(year)
                        .grossPay(gross)
                        .deduction(deduction)
                        .netPay(net)
                        .pdfUrl(pdfUrl)
                        .published(false)
                        .build();
            }

            generated.add(payslipRepository.save(payslip));
        }
        return generated;
    }

    private double calculateUnpaidLeaveDays(Employee emp, String monthName, int year) {
        java.time.Month targetMonth;
        try {
            targetMonth = java.time.Month.valueOf(monthName.toUpperCase());
        } catch (IllegalArgumentException e) {
            return 0.0;
        }

        List<LeaveRequest> requests = leaveRequestRepository.findByEmployeeId(emp.getId());
        List<LeaveRequest> approvedRequests = requests.stream()
                .filter(r -> "APPROVED".equalsIgnoreCase(r.getStatus()))
                .filter(r -> r.getStartDate().getYear() == year || r.getEndDate().getYear() == year)
                .sorted(java.util.Comparator.comparing(LeaveRequest::getStartDate))
                .collect(Collectors.toList());

        Map<String, Integer> approvedDaysPerType = new HashMap<>();
        for (LeaveRequest r : approvedRequests) {
            LocalDate current = r.getStartDate();
            int daysInYear = 0;
            while (!current.isAfter(r.getEndDate())) {
                if (current.getYear() == year) {
                    long isWorkingDay = leaveService.calculateLeaveDays(emp.getId(), current, current);
                    if (isWorkingDay > 0) {
                        daysInYear++;
                    }
                }
                current = current.plusDays(1);
            }
            approvedDaysPerType.put(r.getLeaveType(), approvedDaysPerType.getOrDefault(r.getLeaveType(), 0) + daysInYear);
        }

        List<LeaveBalance> balances = leaveBalanceRepository.findByEmployeeId(emp.getId());
        Map<String, Integer> limits = new HashMap<>();
        for (LeaveBalance b : balances) {
            if (b.getYear() == year) {
                int approvedInYear = approvedDaysPerType.getOrDefault(b.getLeaveType(), 0);
                limits.put(b.getLeaveType(), b.getBalance() + approvedInYear);
            }
        }

        double totalUnpaidDaysInMonth = 0.0;
        Map<String, Integer> cumulativeDays = new HashMap<>();

        for (LeaveRequest r : approvedRequests) {
            String leaveType = r.getLeaveType();
            int limit = limits.getOrDefault(leaveType, 0);
            if ("UNPAID".equalsIgnoreCase(leaveType) || "LWP".equalsIgnoreCase(leaveType)) {
                limit = 0;
            }

            LocalDate current = r.getStartDate();
            while (!current.isAfter(r.getEndDate())) {
                long isWorkingDay = leaveService.calculateLeaveDays(emp.getId(), current, current);
                if (isWorkingDay > 0) {
                    cumulativeDays.put(leaveType, cumulativeDays.getOrDefault(leaveType, 0) + 1);
                    int currentAccum = cumulativeDays.get(leaveType);
                    if (current.getYear() == year && current.getMonth() == targetMonth) {
                        if (currentAccum > limit) {
                            totalUnpaidDaysInMonth += 1.0;
                        }
                    }
                }
                current = current.plusDays(1);
            }
        }

        return totalUnpaidDaysInMonth;
    }

    private double calculateApprovedExpenses(UUID employeeId, String monthName, int year) {
        java.time.Month targetMonth;
        try {
            targetMonth = java.time.Month.valueOf(monthName.toUpperCase());
        } catch (IllegalArgumentException e) {
            return 0.0;
        }

        List<ExpenseClaim> claims = expenseClaimRepository.findByEmployeeId(employeeId);
        double totalExpenses = 0.0;
        for (ExpenseClaim claim : claims) {
            if (claim.getCreatedAt() != null &&
                    claim.getCreatedAt().getYear() == year &&
                    claim.getCreatedAt().getMonth() == targetMonth) {
                if ("APPROVED".equalsIgnoreCase(claim.getStatus()) || "PAID".equalsIgnoreCase(claim.getStatus())) {
                    totalExpenses += claim.getAmount();
                }
            }
        }
        return totalExpenses;
    }

    private double calculateApprovedOvertimePay(UUID employeeId, String monthName, int year, double baseMonthlySalary) {
        java.time.Month targetMonth;
        try {
            targetMonth = java.time.Month.valueOf(monthName.toUpperCase());
        } catch (IllegalArgumentException e) {
            return 0.0;
        }

        List<com.dotsolution.dot.payroll.entity.OvertimeRecord> records = overtimeRecordRepository.findByEmployeeIdAndStatus(employeeId, "APPROVED");
        double totalHours = 0.0;
        for (com.dotsolution.dot.payroll.entity.OvertimeRecord r : records) {
            if (r.getDate() != null && r.getDate().getYear() == year && r.getDate().getMonth() == targetMonth) {
                totalHours += r.getHours();
            }
        }
        
        // Calculate overtime pay rate (1.5x standard hourly rate)
        // Assume standard 160 working hours a month
        double standardHourlyRate = baseMonthlySalary / 160.0;
        double overtimeHourlyRate = standardHourlyRate * 1.5;
        return Math.round(totalHours * overtimeHourlyRate);
    }

    public void rejectPayslips(String month, Integer year) {
        List<Payslip> payslips = payslipRepository.findByMonthAndYear(month, year);
        for (Payslip p : payslips) {
            if (!p.getPublished()) {
                payslipRepository.delete(p);
            }
        }
    }

    public Payslip updatePayslip(UUID id, Payslip details) {
        Payslip existing = payslipRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Payslip not found: " + id));
        if (existing.getPublished()) {
            throw new IllegalStateException("Cannot edit a published payslip");
        }
        existing.setGrossPay(details.getGrossPay());
        existing.setDeduction(details.getDeduction());
        existing.setNetPay(details.getGrossPay() - details.getDeduction());
        return payslipRepository.save(existing);
    }
}
