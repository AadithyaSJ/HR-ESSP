package com.dotsolution.dot.payroll;

import com.dotsolution.dot.common.EntityNotFoundException;
import com.dotsolution.dot.payroll.entity.OvertimeRecord;
import com.dotsolution.dot.payroll.repository.OvertimeRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class OvertimeService {

    @Autowired
    private OvertimeRecordRepository overtimeRecordRepository;

    public OvertimeRecord logOvertime(OvertimeRecord record) {
        record.setStatus("PENDING");
        return overtimeRecordRepository.save(record);
    }

    public List<OvertimeRecord> getOvertimeByEmployee(UUID employeeId) {
        return overtimeRecordRepository.findByEmployeeId(employeeId);
    }

    public List<OvertimeRecord> getPendingOvertime() {
        return overtimeRecordRepository.findByStatus("PENDING");
    }

    public OvertimeRecord approveOvertime(UUID id, UUID managerId) {
        OvertimeRecord record = overtimeRecordRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Overtime record not found with id: " + id));
        if (!"PENDING".equalsIgnoreCase(record.getStatus())) {
            throw new IllegalStateException("Overtime record must be PENDING to approve");
        }
        record.setStatus("APPROVED");
        record.setApprovedBy(managerId);
        return overtimeRecordRepository.save(record);
    }

    public OvertimeRecord rejectOvertime(UUID id, UUID managerId) {
        OvertimeRecord record = overtimeRecordRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Overtime record not found with id: " + id));
        if (!"PENDING".equalsIgnoreCase(record.getStatus())) {
            throw new IllegalStateException("Overtime record must be PENDING to reject");
        }
        record.setStatus("REJECTED");
        record.setApprovedBy(managerId);
        return overtimeRecordRepository.save(record);
    }
}
