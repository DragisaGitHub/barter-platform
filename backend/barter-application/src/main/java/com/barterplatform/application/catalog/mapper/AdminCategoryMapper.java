package com.barterplatform.application.catalog.mapper;

import com.barterplatform.api.model.AdminCategoryResponse;
import com.barterplatform.domain.catalog.entity.CategoryEntity;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class AdminCategoryMapper {

    public AdminCategoryResponse toResponse(CategoryEntity entity, Map<Long, CategoryEntity> parentsById) {
        CategoryEntity parent = entity.getParentId() == null ? null : parentsById.get(entity.getParentId());

        return new AdminCategoryResponse()
                .uuid(entity.getUuid())
                .name(entity.getName())
                .slug(entity.getSlug())
                .description(entity.getDescription())
                .parentUuid(parent != null ? parent.getUuid() : null)
                .parentName(parent != null ? parent.getName() : null)
                .sortOrder(entity.getSortOrder())
                .deleted(entity.getDeletedAt() != null)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .deletedAt(entity.getDeletedAt());
    }

    public List<AdminCategoryResponse> toResponseList(List<CategoryEntity> entities, Map<Long, CategoryEntity> parentsById) {
        return entities.stream()
                .map(entity -> toResponse(entity, parentsById))
                .toList();
    }
}

