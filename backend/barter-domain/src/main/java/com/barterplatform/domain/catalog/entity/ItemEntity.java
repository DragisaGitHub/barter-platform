package com.barterplatform.domain.catalog.entity;

import com.barterplatform.common.persistence.AuditableEntity;
import com.barterplatform.domain.catalog.enums.ItemCondition;
import com.barterplatform.domain.catalog.enums.ItemStatus;
import com.barterplatform.domain.catalog.enums.ListingMode;
import com.barterplatform.domain.catalog.enums.ListingTemplateType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "items")
public class ItemEntity extends AuditableEntity {

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "exchange_location", length = 255)
    private String exchangeLocation;

    @Column(name = "exchange_city", length = 120)
    private String exchangeCity;

    @Column(name = "exchange_area", length = 120)
    private String exchangeArea;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private ItemStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "condition", nullable = false, length = 40)
    private ItemCondition condition;

    @Enumerated(EnumType.STRING)
    @Column(name = "listing_mode", nullable = false, length = 40)
    private ListingMode listingMode = ListingMode.SINGLE;

    @Enumerated(EnumType.STRING)
    @Column(name = "listing_template_type", length = 40)
    private ListingTemplateType listingTemplateType;

    @Column(name = "template_metadata_json", columnDefinition = "TEXT")
    private String templateMetadataJson;

    @Column(name = "archived_at")
    private OffsetDateTime archivedAt;

    @Column(name = "removed_at")
    private OffsetDateTime removedAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;
}

