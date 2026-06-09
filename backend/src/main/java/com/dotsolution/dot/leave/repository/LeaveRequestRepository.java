package com.dotsolution.dot.leave.repository;

import com.dotsolution.dot.leave.entity.LeaveRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, UUID> {
    List<LeaveRequest> findByEmployeeId(UUID employeeId);
    List<LeaveRequest> findByEmployeeIdIn(List<UUID> employeeIds);
}
