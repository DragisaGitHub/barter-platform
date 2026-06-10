package com.barterplatform.application.notification.service.impl;

import com.barterplatform.api.model.NotificationPagedResponse;
import com.barterplatform.api.model.NotificationResponse;
import com.barterplatform.api.model.NotificationUnreadCountResponse;
import com.barterplatform.application.common.pagination.PageRequestFactory;
import com.barterplatform.application.common.pagination.PageResponseMapper;
import com.barterplatform.application.notification.mapper.NotificationMapper;
import com.barterplatform.application.notification.service.NotificationService;
import com.barterplatform.common.exception.ApiException;
import com.barterplatform.common.exception.ErrorCode;
import com.barterplatform.domain.identity.entity.UserEntity;
import com.barterplatform.domain.notification.entity.NotificationEntity;
import com.barterplatform.domain.notification.enums.NotificationType;
import com.barterplatform.infrastructure.identity.repository.UserRepository;
import com.barterplatform.infrastructure.notification.repository.NotificationRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private static final String DEFAULT_SORT_FIELD = "createdAt";
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("createdAt");

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final NotificationMapper notificationMapper;
    private final PageRequestFactory pageRequestFactory;
    private final PageResponseMapper pageResponseMapper;

    public NotificationServiceImpl(NotificationRepository notificationRepository,
                                   UserRepository userRepository,
                                   NotificationMapper notificationMapper,
                                   PageRequestFactory pageRequestFactory,
                                   PageResponseMapper pageResponseMapper) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.notificationMapper = notificationMapper;
        this.pageRequestFactory = pageRequestFactory;
        this.pageResponseMapper = pageResponseMapper;
    }

    // ── Create ───────────────────────────────────────────────────

    @Override
    public void createNotification(Long recipientUserId, NotificationType type,
                                   Map<String, Object> metadata,
                                   String title, String message,
                                   UUID referenceUuid, String referenceType) {
        NotificationEntity notification = new NotificationEntity();
        notification.setRecipientUserId(recipientUserId);
        notification.setType(type);
        notification.setMetadata(normalizeMetadata(metadata));
        notification.setTitle(StringUtils.hasText(title) ? title : type.name());
        notification.setMessage(StringUtils.hasText(message) ? message : null);
        notification.setReferenceUuid(referenceUuid);
        notification.setReferenceType(referenceType);
        notificationRepository.save(notification);
    }

    // ── List ─────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public NotificationPagedResponse listNotifications(UUID currentUserUuid,
                                                       Integer page, Integer size, String sort) {
        UserEntity user = resolveUser(currentUserUuid);

        // Default to newest-first; allow caller to override.
        String effectiveSort = (sort == null || sort.isBlank()) ? "createdAt,desc" : sort;
        PageRequestFactory.ResolvedPageRequest resolved =
                pageRequestFactory.create(page, size, effectiveSort, DEFAULT_SORT_FIELD, ALLOWED_SORT_FIELDS);

        Page<NotificationEntity> pageResult =
                notificationRepository.findByRecipientUserId(user.getId(), resolved.pageable());

        List<NotificationResponse> content = pageResult.getContent().stream()
                .map(notificationMapper::toResponse)
                .toList();

        return pageResponseMapper.toNotificationPagedResponse(pageResult, content, resolved.sort());
    }

    // ── Unread count ─────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public NotificationUnreadCountResponse getUnreadCount(UUID currentUserUuid) {
        UserEntity user = resolveUser(currentUserUuid);
        long count = notificationRepository.countByRecipientUserIdAndIsReadFalse(user.getId());
        return new NotificationUnreadCountResponse().count(count);
    }

    // ── Mark single as read ──────────────────────────────────────

    @Override
    public NotificationResponse markAsRead(UUID currentUserUuid, UUID notificationUuid) {
        UserEntity user = resolveUser(currentUserUuid);
        NotificationEntity notification = notificationRepository
                .findByUuidAndRecipientUserId(notificationUuid, user.getId())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        ErrorCode.NOT_FOUND,
                        "Notification with uuid '%s' was not found.".formatted(notificationUuid)));

        notification.markAsRead();
        NotificationEntity saved = notificationRepository.save(notification);
        return notificationMapper.toResponse(saved);
    }

    // ── Mark all as read ─────────────────────────────────────────

    @Override
    public void markAllAsRead(UUID currentUserUuid) {
        UserEntity user = resolveUser(currentUserUuid);
        notificationRepository.markAllReadByRecipientUserId(user.getId());
    }

    // ── Private helpers ──────────────────────────────────────────

    private UserEntity resolveUser(UUID userUuid) {
        return userRepository.findByUuid(userUuid)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        ErrorCode.NOT_FOUND,
                        "User with uuid '%s' was not found.".formatted(userUuid)));
    }

    private Map<String, Object> normalizeMetadata(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }
        return new LinkedHashMap<>(metadata);
    }
}

