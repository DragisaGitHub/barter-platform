package com.barterplatform.application.catalog.mapper;

import com.barterplatform.api.model.AdminTagResponse;
import com.barterplatform.domain.catalog.entity.TagEntity;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class AdminTagMapper {

    public AdminTagResponse toResponse(TagEntity entity) {
        return new AdminTagResponse()
                .uuid(entity.getUuid())
                .name(entity.getName())
                .slug(entity.getSlug())
                .deleted(entity.getDeletedAt() != null)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .deletedAt(entity.getDeletedAt());
    }

    public List<AdminTagResponse> toResponseList(List<TagEntity> entities) {
        return entities.stream()
                .map(this::toResponse)
                .toList();
    }
}

