package com.barterplatform.application.catalog.mapper;

import com.barterplatform.application.config.CentralMapperConfig;
import com.barterplatform.api.model.TagResponse;
import com.barterplatform.domain.catalog.entity.TagEntity;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(config = CentralMapperConfig.class)
public interface TagMapper {

    TagResponse toResponse(TagEntity entity);

    List<TagResponse> toResponseList(List<TagEntity> entities);
}

