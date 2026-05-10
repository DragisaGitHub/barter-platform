package com.barterplatform.domain.catalog.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "item_tags")
public class ItemTagEntity {

    @EmbeddedId
    private ItemTagId id;

    @Column(name = "assigned_at", nullable = false)
    private OffsetDateTime assignedAt;
}

