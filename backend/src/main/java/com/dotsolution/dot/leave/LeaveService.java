package com.dotsolution.dot.leave;

import com.dotsolution.dot.common.EntityNotFoundException;
import com.dotsolution.dot.employee.entity.Employee;
import com.dotsolution.dot.employee.repository.EmployeeRepository;
import com.dotsolution.dot.leave.entity.LeaveBalance;
import com.dotsolution.dot.leave.entity.LeaveRequest;
import com.dotsolution.dot.leave.repository.LeaveBalanceRepository;
import com.dotsolution.dot.leave.repository.LeaveRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class LeaveService {

    @Autowired
    private LeaveRequestRepository leaveRequestRepository;

    @Autowired
    private LeaveBalanceRepository leaveBalanceRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private com.dotsolution.dot.leave.repository.PublicHolidayRepository publicHolidayRepository;

    public List<LeaveBalance> getLeaveBalances(UUID employeeId) {
        return leaveBalanceRepository.findByEmployeeId(employeeId);
    }

    public List<LeaveRequest> getLeaveRequests(UUID employeeId) {
        return leaveRequestRepository.findByEmployeeId(employeeId);
    }

    public List<LeaveRequest> getAllLeaveRequests() {
        return leaveRequestRepository.findAll();
    }

    public List<LeaveRequest> getManagerLeaveRequests(UUID managerId) {
        List<Employee> reports = employeeRepository.findByManagerId(managerId);
        if (reports.isEmpty()) {
            return Collections.emptyList();
        }
        List<UUID> reportIds = reports.stream().map(Employee::getId).collect(Collectors.toList());
        return leaveRequestRepository.findByEmployeeIdIn(reportIds);
    }

    public long calculateLeaveDays(UUID employeeId, LocalDate startDate, LocalDate endDate) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new EntityNotFoundException("Employee not found with id: " + employeeId));

        String country = "India";
        if (employee.getAddress() != null) {
            String addrUpper = employee.getAddress().toUpperCase();
            if (addrUpper.contains("USA") || addrUpper.contains("UNITED STATES")) {
                country = "USA";
            } else if (addrUpper.contains("UK") || addrUpper.contains("UNITED KINGDOM")) {
                country = "UK";
            }
        }

        List<LocalDate> holidays = publicHolidayRepository.findByCountry(country).stream()
                .map(com.dotsolution.dot.leave.entity.PublicHoliday::getHolidayDate)
                .collect(Collectors.toList());

        long count = 0;
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            java.time.DayOfWeek dow = current.getDayOfWeek();
            boolean isWeekend = (dow == java.time.DayOfWeek.SATURDAY || dow == java.time.DayOfWeek.SUNDAY);
            boolean isHoliday = holidays.contains(current);

            if (!isWeekend && !isHoliday) {
                count++;
            }
            current = current.plusDays(1);
        }
        return count;
    }

    public LeaveRequest createLeaveRequest(LeaveRequest request) {
        if (request.getStartDate().isAfter(request.getEndDate())) {
            throw new IllegalArgumentException("Start date cannot be after end date");
        }

        long days = calculateLeaveDays(request.getEmployeeId(), request.getStartDate(), request.getEndDate());
        if (days == 0) {
            throw new IllegalArgumentException("Leave request contains only weekends and/or public holidays");
        }
        int year = request.getStartDate().getYear();

        if ("UNPAID".equalsIgnoreCase(request.getLeaveType()) || "LWP".equalsIgnoreCase(request.getLeaveType())) {
            request.setStatus("PENDING");
            return leaveRequestRepository.save(request);
        }

        Optional<LeaveBalance> balanceOpt = leaveBalanceRepository.findByEmployeeIdAndLeaveTypeAndYear(
                request.getEmployeeId(), request.getLeaveType(), year);

        if (balanceOpt.isEmpty()) {
            throw new IllegalArgumentException("No leave balance configured for employee for leave type: " 
                    + request.getLeaveType() + " and year: " + year);
        }

        if (balanceOpt.get().getBalance() < days) {
            throw new IllegalArgumentException("Insufficient leave balance. Available: " 
                    + balanceOpt.get().getBalance() + ", Requested: " + days);
        }

        request.setStatus("PENDING");
        return leaveRequestRepository.save(request);
    }

    public LeaveRequest approveLeaveRequest(UUID id, String managerComment) {
        LeaveRequest request = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Leave request not found with id: " + id));

        if (!"PENDING".equalsIgnoreCase(request.getStatus())) {
            throw new IllegalStateException("Leave request must be in PENDING status to approve");
        }

        if ("UNPAID".equalsIgnoreCase(request.getLeaveType()) || "LWP".equalsIgnoreCase(request.getLeaveType())) {
            request.setStatus("APPROVED");
            request.setManagerComment(managerComment);
            return leaveRequestRepository.save(request);
        }

        long days = calculateLeaveDays(request.getEmployeeId(), request.getStartDate(), request.getEndDate());
        int year = request.getStartDate().getYear();

        LeaveBalance balance = leaveBalanceRepository.findByEmployeeIdAndLeaveTypeAndYear(
                request.getEmployeeId(), request.getLeaveType(), year)
                .orElseThrow(() -> new EntityNotFoundException("Leave balance not found for approval validation"));

        if (balance.getBalance() < days) {
            throw new IllegalArgumentException("Insufficient leave balance to approve. Available: " 
                    + balance.getBalance() + ", Requested: " + days);
        }

        // Deduct balance
        balance.setBalance(balance.getBalance() - (int) days);
        leaveBalanceRepository.save(balance);

        request.setStatus("APPROVED");
        request.setManagerComment(managerComment);
        return leaveRequestRepository.save(request);
    }

    public LeaveRequest rejectLeaveRequest(UUID id, String managerComment) {
        LeaveRequest request = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Leave request not found with id: " + id));

        if (!"PENDING".equalsIgnoreCase(request.getStatus())) {
            throw new IllegalStateException("Leave request must be in PENDING status to reject");
        }

        request.setStatus("REJECTED");
        request.setManagerComment(managerComment);
        return leaveRequestRepository.save(request);
    }

    public LeaveRequest cancelLeaveRequest(UUID id) {
        LeaveRequest request = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Leave request not found with id: " + id));

        if ("CANCELLED".equalsIgnoreCase(request.getStatus())) {
            throw new IllegalStateException("Leave request is already cancelled");
        }

        // Refund if it was already approved
        if ("APPROVED".equalsIgnoreCase(request.getStatus())) {
            if (!"UNPAID".equalsIgnoreCase(request.getLeaveType()) && !"LWP".equalsIgnoreCase(request.getLeaveType())) {
                long days = calculateLeaveDays(request.getEmployeeId(), request.getStartDate(), request.getEndDate());
                int year = request.getStartDate().getYear();

                Optional<LeaveBalance> balanceOpt = leaveBalanceRepository.findByEmployeeIdAndLeaveTypeAndYear(
                        request.getEmployeeId(), request.getLeaveType(), year);

                if (balanceOpt.isPresent()) {
                    LeaveBalance balance = balanceOpt.get();
                    balance.setBalance(balance.getBalance() + (int) days);
                    leaveBalanceRepository.save(balance);
                }
            }
        }

        request.setStatus("CANCELLED");
        return leaveRequestRepository.save(request);
    }
}
