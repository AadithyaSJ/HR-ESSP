package com.dotsolution.dot.employee.repository;

import com.dotsolution.dot.employee.entity.Resignation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface ResignationRepository extends JpaRepository<Resignation, UUID> {
    List<Resignation> findByEmployeeId(UUID employeeId);
}
