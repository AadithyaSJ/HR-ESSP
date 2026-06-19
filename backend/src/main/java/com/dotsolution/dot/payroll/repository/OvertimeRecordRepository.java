package com.dotsolution.dot.payroll.repository;

import com.dotsolution.dot.payroll.entity.OvertimeRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface OvertimeRecordRepository extends JpaRepository<OvertimeRecord, UUID> {
    List<OvertimeRecord> findByEmployeeId(UUID employeeId);
    List<OvertimeRecord> findByStatus(String status);
    List<OvertimeRecord> findByEmployeeIdAndStatus(UUID employeeId, String status);
}
