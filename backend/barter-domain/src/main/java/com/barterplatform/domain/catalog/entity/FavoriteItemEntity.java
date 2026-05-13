package com.barterplatform.domain.catalog.entity;

import com.barterplatform.common.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "favorite_items",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_favorite_items_user_item", columnNames = {"user_id", "item_id"})
        },
        indexes = {
                @Index(name = "idx_favorite_items_user_id", columnList = "user_id"),
                @Index(name = "idx_favorite_items_item_id", columnList = "item_id")
        })
public class FavoriteItemEntity extends AuditableEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;
}

