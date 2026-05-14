package com.barterplatform.application.catalog.service;

import com.barterplatform.api.model.AdminTagPagedResponse;
import com.barterplatform.api.model.AdminTagResponse;
import com.barterplatform.api.model.CreateTagRequest;
import com.barterplatform.api.model.UpdateTagRequest;
import java.util.UUID;

public interface AdminTagService {

    AdminTagPagedResponse searchTags(Integer page, Integer size, String sort, String q, Boolean includeDeleted);

    AdminTagResponse createTag(CreateTagRequest request);

    AdminTagResponse getTag(UUID tagUuid);

    AdminTagResponse updateTag(UUID tagUuid, UpdateTagRequest request);

    void deleteTag(UUID tagUuid);
}

