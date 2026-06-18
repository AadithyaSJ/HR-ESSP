package com.dotsolution.dot.document.repository;

import com.dotsolution.dot.document.entity.EmployeeDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmployeeDocumentRepository extends JpaRepository<EmployeeDocument, UUID> {
    List<EmployeeDocument> findByEmployeeId(UUID employeeId);
    Optional<EmployeeDocument> findByEmployeeIdAndDocumentType(UUID employeeId, String documentType);
}
