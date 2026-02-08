package com.rentpro.backend.notification.dto;

import com.rentpro.backend.notification.Notification;
import java.time.Instant;

public record NotificationResponse(
        Long id, String type, String title, String message,
        String entityType, Long entityId,
        boolean isRead, Instant createdAt
) {
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(
                n.getId(), n.getType(), n.getTitle(), n.getMessage(),
                n.getEntityType(), n.getEntityId(),
                n.isRead(), n.getCreatedAt()
        );
    }
}
