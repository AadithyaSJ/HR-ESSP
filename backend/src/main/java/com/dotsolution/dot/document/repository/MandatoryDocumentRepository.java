package com.dotsolution.dot.document.repository;

import com.dotsolution.dot.document.entity.MandatoryDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MandatoryDocumentRepository extends JpaRepository<MandatoryDocument, UUID> {
    Optional<MandatoryDocument> findByName(String name);
}
