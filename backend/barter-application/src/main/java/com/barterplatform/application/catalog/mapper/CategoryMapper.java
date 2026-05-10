package com.barterplatform.application.catalog.mapper;

import com.barterplatform.application.config.CentralMapperConfig;
import com.barterplatform.api.model.CategoryResponse;
import com.barterplatform.domain.catalog.entity.CategoryEntity;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(config = CentralMapperConfig.class)
public interface CategoryMapper {

    CategoryResponse toResponse(CategoryEntity entity);

    List<CategoryResponse> toResponseList(List<CategoryEntity> entities);
}

