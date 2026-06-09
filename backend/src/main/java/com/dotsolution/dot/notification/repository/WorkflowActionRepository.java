package com.dotsolution.dot.notification.repository;

import com.dotsolution.dot.notification.entity.WorkflowAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface WorkflowActionRepository extends JpaRepository<WorkflowAction, UUID> {
    List<WorkflowAction> findByEntityTypeAndEntityId(String entityType, UUID entityId);
}
