package com.barterplatform.infrastructure.notification.repository;

import com.barterplatform.domain.notification.entity.NotificationEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {

    /**
     * List all notifications for a user, ordered by the provided pageable.
     */
    Page<NotificationEntity> findByRecipientUserId(Long recipientUserId, Pageable pageable);

    /**
     * Count unread notifications for a user.
     */
    long countByRecipientUserIdAndIsReadFalse(Long recipientUserId);

    /**
     * Find a single notification by UUID, scoped to a specific recipient to prevent cross-user access.
     */
    Optional<NotificationEntity> findByUuidAndRecipientUserId(UUID uuid, Long recipientUserId);

    /**
     * Bulk-mark all unread notifications as read for a given user.
     * Uses a native query to set both is_read and read_at / updated_at consistently.
     */
    @Modifying(clearAutomatically = true)
    @Query(value = """
            UPDATE notifications
               SET is_read    = true,
                   read_at    = NOW(),
                   updated_at = NOW()
             WHERE recipient_user_id = :userId
               AND is_read = false
            """, nativeQuery = true)
    int markAllReadByRecipientUserId(@Param("userId") Long userId);
}

