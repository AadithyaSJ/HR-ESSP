package com.dotsolution.dot.travel.repository;

import com.dotsolution.dot.travel.entity.TravelRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface TravelRequestRepository extends JpaRepository<TravelRequest, UUID> {
    List<TravelRequest> findByEmployeeId(UUID employeeId);
    List<TravelRequest> findByManagerId(UUID managerId);
}
