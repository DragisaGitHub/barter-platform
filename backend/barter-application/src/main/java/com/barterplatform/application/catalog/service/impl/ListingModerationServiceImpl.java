package com.barterplatform.application.catalog.service.impl;

import com.barterplatform.api.model.AdminListingDetailResponse;
import com.barterplatform.api.model.AdminRemoveListingRequest;
import com.barterplatform.api.model.AdminRestoreListingRequest;
import com.barterplatform.application.catalog.service.AdminListingQueryService;
import com.barterplatform.application.catalog.service.ListingModerationService;
import com.barterplatform.application.catalog.service.TradeOfferInvalidationService;
import com.barterplatform.application.notification.service.NotificationService;
import com.barterplatform.common.exception.ApiException;
import com.barterplatform.common.exception.ErrorCode;
import com.barterplatform.domain.catalog.entity.ItemEntity;
import com.barterplatform.domain.catalog.enums.ItemStatus;
import com.barterplatform.domain.catalog.moderation.ListingModerationActionEntity;
import com.barterplatform.domain.catalog.moderation.ListingModerationActionType;
import com.barterplatform.domain.catalog.moderation.ListingModerationSourceType;
import com.barterplatform.domain.identity.entity.UserEntity;
import com.barterplatform.domain.notification.enums.NotificationType;
import com.barterplatform.infrastructure.catalog.repository.ItemRepository;
import com.barterplatform.infrastructure.catalog.repository.ListingModerationActionRepository;
import com.barterplatform.infrastructure.identity.repository.UserRepository;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ListingModerationServiceImpl implements ListingModerationService {

    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final ListingModerationActionRepository listingModerationActionRepository;
    private final TradeOfferInvalidationService tradeOfferInvalidationService;
    private final NotificationService notificationService;
    private final AdminListingQueryService adminListingQueryService;

    public ListingModerationServiceImpl(
            ItemRepository itemRepository,
            UserRepository userRepository,
            ListingModerationActionRepository listingModerationActionRepository,
            TradeOfferInvalidationService tradeOfferInvalidationService,
            NotificationService notificationService,
            AdminListingQueryService adminListingQueryService) {
        this.itemRepository = itemRepository;
        this.userRepository = userRepository;
        this.listingModerationActionRepository = listingModerationActionRepository;
        this.tradeOfferInvalidationService = tradeOfferInvalidationService;
        this.notificationService = notificationService;
        this.adminListingQueryService = adminListingQueryService;
    }

    @Override
    public AdminListingDetailResponse removeListing(UUID adminUserUuid, UUID listingUuid, AdminRemoveListingRequest request) {
        UserEntity admin = resolveUser(adminUserUuid);
        ItemEntity item = resolveItem(listingUuid);

        if (item.getStatus() == ItemStatus.REMOVED) {
            throw conflict("Listing is already removed.");
        }

        item.setStatus(ItemStatus.REMOVED);
        item.setRemovedAt(OffsetDateTime.now());
        itemRepository.save(item);

        createModerationAction(item, admin, request.getReasonCode().name(), request.getUserMessage(), request.getInternalNote(),
                ListingModerationActionType.REMOVE);
        tradeOfferInvalidationService.invalidatePendingOffersForListing(item.getId());
        notificationService.createNotification(
                item.getOwnerId(),
                NotificationType.LISTING_REMOVED,
                "Your listing was removed",
                request.getUserMessage() != null && !request.getUserMessage().isBlank()
                        ? request.getUserMessage()
                        : "Your listing \"" + item.getTitle() + "\" was removed by an administrator.",
                item.getUuid(),
                "ITEM");

        return adminListingQueryService.getListing(listingUuid);
    }

    @Override
    public AdminListingDetailResponse restoreListing(UUID adminUserUuid, UUID listingUuid, AdminRestoreListingRequest request) {
        UserEntity admin = resolveUser(adminUserUuid);
        ItemEntity item = resolveItem(listingUuid);

        if (item.getStatus() != ItemStatus.REMOVED) {
            throw conflict("Only removed listings can be restored.");
        }

        item.setStatus(ItemStatus.ACTIVE);
        item.setRemovedAt(null);
        item.setArchivedAt(null);
        itemRepository.save(item);

        createModerationAction(item, admin, request.getReasonCode().name(), request.getUserMessage(), request.getInternalNote(),
                ListingModerationActionType.RESTORE);
        notificationService.createNotification(
                item.getOwnerId(),
                NotificationType.LISTING_RESTORED,
                "Your listing was restored",
                request.getUserMessage() != null && !request.getUserMessage().isBlank()
                        ? request.getUserMessage()
                        : "Your listing \"" + item.getTitle() + "\" was restored by an administrator.",
                item.getUuid(),
                "ITEM");

        return adminListingQueryService.getListing(listingUuid);
    }

    private void createModerationAction(
            ItemEntity item,
            UserEntity admin,
            String reasonCode,
            String userMessage,
            String internalNote,
            ListingModerationActionType actionType) {
        ListingModerationActionEntity action = new ListingModerationActionEntity();
        action.setItemId(item.getId());
        action.setActionType(actionType);
        action.setReasonCode(com.barterplatform.domain.catalog.moderation.ListingModerationReasonCode.valueOf(reasonCode));
        action.setSourceType(ListingModerationSourceType.ADMIN);
        action.setPerformedByUserId(admin.getId());
        action.setUserMessage(userMessage);
        action.setInternalNote(internalNote);
        listingModerationActionRepository.save(action);
    }

    private UserEntity resolveUser(UUID userUuid) {
        return userRepository.findByUuid(userUuid)
                .orElseThrow(() -> notFound("User with uuid '%s' was not found.", userUuid));
    }

    private ItemEntity resolveItem(UUID listingUuid) {
        ItemEntity item = itemRepository.findByUuid(listingUuid)
                .orElseThrow(() -> notFound("Item with uuid '%s' was not found.", listingUuid));
        if (item.getDeletedAt() != null) {
            throw notFound("Item with uuid '%s' was not found.", listingUuid);
        }
        return item;
    }

    private ApiException notFound(String messageTemplate, Object... args) {
        return new ApiException(HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND, messageTemplate.formatted(args));
    }

    private ApiException conflict(String message) {
        return new ApiException(HttpStatus.CONFLICT, ErrorCode.CONFLICT, message);
    }
}

