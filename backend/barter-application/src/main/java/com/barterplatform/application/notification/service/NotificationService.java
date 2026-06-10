package com.barterplatform.application.notification.service;

import com.barterplatform.api.model.NotificationPagedResponse;
import com.barterplatform.api.model.NotificationResponse;
import com.barterplatform.api.model.NotificationUnreadCountResponse;
import com.barterplatform.domain.notification.enums.NotificationType;
import java.util.Map;
import java.util.UUID;

public interface NotificationService {

    /**
     * Create a new notification for a recipient. Called internally from other services
     * within the same transaction boundary.
     *
     * @param recipientUserId internal user ID of the recipient
     * @param type            notification type
     * @param metadata        structured language-neutral rendering data for the frontend
     * @param title           legacy fallback title for pre-rendered notifications
     * @param message         legacy fallback body text (may be null)
     * @param referenceUuid   UUID of the referenced entity (e.g. trade offer UUID)
     * @param referenceType   discriminator for the referenced entity (e.g. "TRADE_OFFER")
     */
    void createNotification(Long recipientUserId, NotificationType type,
                            Map<String, Object> metadata,
                            String title, String message,
                            UUID referenceUuid, String referenceType);

    default void createNotification(Long recipientUserId, NotificationType type,
                                    String title, String message,
                                    UUID referenceUuid, String referenceType) {
        createNotification(recipientUserId, type, null, title, message, referenceUuid, referenceType);
    }

    /**
     * List notifications for the current user, newest first by default.
     */
    NotificationPagedResponse listNotifications(UUID currentUserUuid,
                                                Integer page, Integer size, String sort);

    /**
     * Get the unread notification count for the current user.
     */
    NotificationUnreadCountResponse getUnreadCount(UUID currentUserUuid);

    /**
     * Mark a single notification as read. Only the owning user may mark their notification.
     * Returns NOT_FOUND if the notification does not belong to the current user.
     */
    NotificationResponse markAsRead(UUID currentUserUuid, UUID notificationUuid);

    /**
     * Mark all unread notifications as read for the current user.
     */
    void markAllAsRead(UUID currentUserUuid);
}

