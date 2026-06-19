package com.dotsolution.dot.employee;

import com.dotsolution.dot.common.EntityNotFoundException;
import com.dotsolution.dot.employee.entity.Employee;
import com.dotsolution.dot.employee.entity.Resignation;
import com.dotsolution.dot.employee.repository.EmployeeRepository;
import com.dotsolution.dot.employee.repository.ResignationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ResignationService {

    @Autowired
    private ResignationRepository resignationRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    public Resignation submitResignation(Resignation resignation) {
        resignation.setSubmissionDate(LocalDate.now());
        resignation.setStatus("PENDING");
        return resignationRepository.save(resignation);
    }

    public List<Resignation> getResignationsByEmployee(UUID employeeId) {
        return resignationRepository.findByEmployeeId(employeeId);
    }

    public List<Resignation> getAllResignations() {
        return resignationRepository.findAll();
    }

    public Resignation approveResignation(UUID id, LocalDate approvedLwd) {
        Resignation resignation = resignationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Resignation request not found with id: " + id));

        if (!"PENDING".equalsIgnoreCase(resignation.getStatus())) {
            throw new IllegalStateException("Resignation request must be PENDING to approve");
        }

        resignation.setStatus("APPROVED");
        resignation.setApprovedLastWorkingDay(approvedLwd != null ? approvedLwd : resignation.getRequestedLastWorkingDay());
        resignationRepository.save(resignation);

        // Update employee status to NOTICE_PERIOD
        Employee employee = employeeRepository.findById(resignation.getEmployeeId())
                .orElseThrow(() -> new EntityNotFoundException("Employee profile not found with id: " + resignation.getEmployeeId()));
        employee.setStatus("NOTICE_PERIOD");
        employeeRepository.save(employee);

        return resignation;
    }

    public Resignation rejectResignation(UUID id, String rejectionReason) {
        Resignation resignation = resignationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Resignation request not found with id: " + id));

        if (!"PENDING".equalsIgnoreCase(resignation.getStatus())) {
            throw new IllegalStateException("Resignation request must be PENDING to reject");
        }

        resignation.setStatus("REJECTED");
        resignation.setRejectionReason(rejectionReason);
        return resignationRepository.save(resignation);
    }
}
