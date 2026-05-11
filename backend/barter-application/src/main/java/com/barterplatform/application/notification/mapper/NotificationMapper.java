package com.barterplatform.application.notification.mapper;

import com.barterplatform.api.model.NotificationResponse;
import com.barterplatform.application.config.CentralMapperConfig;
import com.barterplatform.domain.notification.entity.NotificationEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = CentralMapperConfig.class)
public interface NotificationMapper {

    // ── Enum mapping (domain → API) ──────────────────────────────

    default com.barterplatform.api.model.NotificationType map(
            com.barterplatform.domain.notification.enums.NotificationType type) {
        return type == null ? null
                : com.barterplatform.api.model.NotificationType.valueOf(type.name());
    }

    // ── Entity → Response ────────────────────────────────────────
    //
    // Lombok generates isRead() getter for the boolean field "isRead", which
    // MapStruct resolves as property "read". The generated DTO property is "isRead"
    // (from getIsRead() getter), so an explicit mapping is required.

    @Mapping(target = "isRead", source = "read")
    NotificationResponse toResponse(NotificationEntity entity);
}

