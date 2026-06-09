package com.dotsolution.dot.leave.repository;

import com.dotsolution.dot.leave.entity.LeaveBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LeaveBalanceRepository extends JpaRepository<LeaveBalance, UUID> {
    List<LeaveBalance> findByEmployeeId(UUID employeeId);
    Optional<LeaveBalance> findByEmployeeIdAndLeaveTypeAndYear(UUID employeeId, String leaveType, Integer year);
}
