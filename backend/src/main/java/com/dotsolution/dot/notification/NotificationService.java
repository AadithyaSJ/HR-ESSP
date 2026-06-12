package com.dotsolution.dot.notification;

import com.dotsolution.dot.common.EntityNotFoundException;
import com.dotsolution.dot.notification.entity.Notification;
import com.dotsolution.dot.notification.entity.WorkflowAction;
import com.dotsolution.dot.notification.repository.NotificationRepository;
import com.dotsolution.dot.notification.repository.WorkflowActionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private WorkflowActionRepository workflowActionRepository;

    public List<Notification> getUserNotifications(UUID userId) {
        return notificationRepository.findByUserId(userId);
    }

    public void markAsRead(UUID id) {
        Notification notif = notificationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Notification not found: " + id));
        notif.setIsRead(true);
        notificationRepository.save(notif);
    }

    public void markAllAsRead(UUID userId) {
        List<Notification> unread = notificationRepository.findByUserIdAndIsRead(userId, false);
        for (Notification n : unread) {
            n.setIsRead(true);
            notificationRepository.save(n);
        }
    }

    public Notification sendNotification(UUID userId, String title, String message) {
        Notification notification = Notification.builder()
                .userId(userId)
                .title(title)
                .message(message)
                .isRead(false)
                .build();
        return notificationRepository.save(notification);
    }

    public WorkflowAction logWorkflowAction(String entityType, UUID entityId, UUID actionBy, String action, String comments) {
        WorkflowAction workflowAction = WorkflowAction.builder()
                .entityType(entityType)
                .entityId(entityId)
                .actionBy(actionBy)
                .action(action)
                .comments(comments)
                .build();
        return workflowActionRepository.save(workflowAction);
    }

    public List<WorkflowAction> getWorkflowHistory(String entityType, UUID entityId) {
        return workflowActionRepository.findByEntityTypeAndEntityId(entityType, entityId);
    }
}
