package com.dotsolution.dot.itkiosk.repository;

import com.dotsolution.dot.itkiosk.entity.ItTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface ItTicketRepository extends JpaRepository<ItTicket, UUID> {
    List<ItTicket> findByEmployeeId(UUID employeeId);
}
