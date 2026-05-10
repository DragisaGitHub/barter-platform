package com.barterplatform.domain.catalog.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Embeddable
public class ItemTagId implements Serializable {

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(name = "tag_id", nullable = false)
    private Long tagId;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ItemTagId that = (ItemTagId) o;
        return Objects.equals(itemId, that.itemId) && Objects.equals(tagId, that.tagId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(itemId, tagId);
    }
}

