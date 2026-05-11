package com.barterplatform.web.notification.controller;

import com.barterplatform.api.controller.NotificationsApi;
import com.barterplatform.api.model.NotificationPagedResponse;
import com.barterplatform.api.model.NotificationResponse;
import com.barterplatform.api.model.NotificationUnreadCountResponse;
import com.barterplatform.application.notification.service.NotificationService;
import com.barterplatform.web.security.jwt.AuthenticatedUser;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class NotificationsController implements NotificationsApi {

    private final NotificationService notificationService;

    public NotificationsController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Override
    public ResponseEntity<NotificationPagedResponse> listNotifications(
            Integer page, Integer size, @Nullable String sort) {
        UUID currentUserUuid = currentUserUuid();
        return ResponseEntity.ok(
                notificationService.listNotifications(currentUserUuid, page, size, sort));
    }

    @Override
    public ResponseEntity<NotificationUnreadCountResponse> getUnreadNotificationCount() {
        UUID currentUserUuid = currentUserUuid();
        return ResponseEntity.ok(notificationService.getUnreadCount(currentUserUuid));
    }

    @Override
    public ResponseEntity<NotificationResponse> markNotificationAsRead(UUID notificationUuid) {
        UUID currentUserUuid = currentUserUuid();
        return ResponseEntity.ok(
                notificationService.markAsRead(currentUserUuid, notificationUuid));
    }

    @Override
    public ResponseEntity<Void> markAllNotificationsAsRead() {
        UUID currentUserUuid = currentUserUuid();
        notificationService.markAllAsRead(currentUserUuid);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    // ── Private helpers ──────────────────────────────────────────

    private UUID currentUserUuid() {
        AuthenticatedUser principal = (AuthenticatedUser) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        return principal.getUserUuid();
    }
}

