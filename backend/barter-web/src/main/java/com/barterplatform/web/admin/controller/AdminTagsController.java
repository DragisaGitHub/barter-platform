package com.barterplatform.web.admin.controller;

import com.barterplatform.api.controller.AdminTagsApi;
import com.barterplatform.api.model.AdminTagPagedResponse;
import com.barterplatform.api.model.AdminTagResponse;
import com.barterplatform.api.model.CreateTagRequest;
import com.barterplatform.api.model.UpdateTagRequest;
import com.barterplatform.application.catalog.service.AdminTagService;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasRole('ADMIN')")
public class AdminTagsController implements AdminTagsApi {

    private final AdminTagService adminTagService;

    public AdminTagsController(AdminTagService adminTagService) {
        this.adminTagService = adminTagService;
    }

    @Override
    public ResponseEntity<AdminTagPagedResponse> listAdminTags(
            Integer page,
            Integer size,
            String sort,
            String q,
            Boolean includeDeleted) {
        return ResponseEntity.ok(adminTagService.searchTags(page, size, sort, q, includeDeleted));
    }

    @Override
    public ResponseEntity<AdminTagResponse> createAdminTag(CreateTagRequest createTagRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(adminTagService.createTag(createTagRequest));
    }

    @Override
    public ResponseEntity<AdminTagResponse> getAdminTagByUuid(UUID tagUuid) {
        return ResponseEntity.ok(adminTagService.getTag(tagUuid));
    }

    @Override
    public ResponseEntity<AdminTagResponse> updateAdminTag(UUID tagUuid, UpdateTagRequest updateTagRequest) {
        return ResponseEntity.ok(adminTagService.updateTag(tagUuid, updateTagRequest));
    }

    @Override
    public ResponseEntity<Void> deleteAdminTag(UUID tagUuid) {
        adminTagService.deleteTag(tagUuid);
        return ResponseEntity.noContent().build();
    }
}

