package com.barterplatform.domain.catalog.entity;

import com.barterplatform.common.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "item_listing_entries")
public class ItemListingEntryEntity extends AuditableEntity {

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "image_id")
    private Long imageId;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;
}

