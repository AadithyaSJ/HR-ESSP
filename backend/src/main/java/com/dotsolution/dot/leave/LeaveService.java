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

    public List<LeaveBalance> getLeaveBalances(UUID employeeId) {
        return leaveBalanceRepository.findByEmployeeId(employeeId);
    }

    public List<LeaveRequest> getLeaveRequests(UUID employeeId) {
        return leaveRequestRepository.findByEmployeeId(employeeId);
    }

    public List<LeaveRequest> getManagerLeaveRequests(UUID managerId) {
        List<Employee> reports = employeeRepository.findByManagerId(managerId);
        if (reports.isEmpty()) {
            return Collections.emptyList();
        }
        List<UUID> reportIds = reports.stream().map(Employee::getId).collect(Collectors.toList());
        return leaveRequestRepository.findByEmployeeIdIn(reportIds);
    }

    public LeaveRequest createLeaveRequest(LeaveRequest request) {
        if (request.getStartDate().isAfter(request.getEndDate())) {
            throw new IllegalArgumentException("Start date cannot be after end date");
        }

        long days = ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate()) + 1;
        int year = request.getStartDate().getYear();

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

        long days = ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate()) + 1;
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
            long days = ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate()) + 1;
            int year = request.getStartDate().getYear();

            Optional<LeaveBalance> balanceOpt = leaveBalanceRepository.findByEmployeeIdAndLeaveTypeAndYear(
                    request.getEmployeeId(), request.getLeaveType(), year);

            if (balanceOpt.isPresent()) {
                LeaveBalance balance = balanceOpt.get();
                balance.setBalance(balance.getBalance() + (int) days);
                leaveBalanceRepository.save(balance);
            }
        }

        request.setStatus("CANCELLED");
        return leaveRequestRepository.save(request);
    }
}
