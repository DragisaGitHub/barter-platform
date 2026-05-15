package com.barterplatform.application.catalog.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.barterplatform.api.model.AdminListingDetailResponse;
import com.barterplatform.api.model.AdminRemoveListingRequest;
import com.barterplatform.api.model.AdminRestoreListingRequest;
import com.barterplatform.application.catalog.service.AdminListingQueryService;
import com.barterplatform.application.catalog.service.TradeOfferInvalidationService;
import com.barterplatform.application.notification.service.NotificationService;
import com.barterplatform.common.exception.ApiException;
import com.barterplatform.domain.catalog.entity.ItemEntity;
import com.barterplatform.domain.catalog.enums.ItemCondition;
import com.barterplatform.domain.catalog.enums.ItemStatus;
import com.barterplatform.domain.catalog.moderation.ListingModerationActionEntity;
import com.barterplatform.domain.catalog.moderation.ListingModerationReasonCode;
import com.barterplatform.domain.identity.entity.UserEntity;
import com.barterplatform.domain.notification.enums.NotificationType;
import com.barterplatform.infrastructure.catalog.repository.ItemRepository;
import com.barterplatform.infrastructure.catalog.repository.ListingModerationActionRepository;
import com.barterplatform.infrastructure.identity.repository.UserRepository;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ListingModerationServiceImplTest {

    @Mock private ItemRepository itemRepository;
    @Mock private UserRepository userRepository;
    @Mock private ListingModerationActionRepository listingModerationActionRepository;
    @Mock private TradeOfferInvalidationService tradeOfferInvalidationService;
    @Mock private NotificationService notificationService;
    @Mock private AdminListingQueryService adminListingQueryService;

    private ListingModerationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ListingModerationServiceImpl(
                itemRepository,
                userRepository,
                listingModerationActionRepository,
                tradeOfferInvalidationService,
                notificationService,
                adminListingQueryService);
    }

    @Test
    void removeListingMarksListingRemovedCreatesHistoryInvalidatesOffersAndNotifiesOwner() {
        UUID adminUuid = UUID.randomUUID();
        UUID listingUuid = UUID.randomUUID();
        UserEntity admin = admin(adminUuid);
        ItemEntity listing = listing(listingUuid, ItemStatus.ACTIVE);
        AdminListingDetailResponse expected = new AdminListingDetailResponse().uuid(listingUuid);

        when(userRepository.findByUuid(adminUuid)).thenReturn(Optional.of(admin));
        when(itemRepository.findByUuid(listingUuid)).thenReturn(Optional.of(listing));
        when(itemRepository.save(any(ItemEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(adminListingQueryService.getListing(listingUuid)).thenReturn(expected);

        AdminRemoveListingRequest request = new AdminRemoveListingRequest()
                .reasonCode(com.barterplatform.api.model.ListingModerationReasonCode.POLICY_VIOLATION)
                .userMessage("Listing removed for policy review")
                .internalNote("internal note");

        AdminListingDetailResponse result = service.removeListing(adminUuid, listingUuid, request);

        assertEquals(expected, result);
        assertEquals(ItemStatus.REMOVED, listing.getStatus());
        assertNotNull(listing.getRemovedAt());
        verify(tradeOfferInvalidationService).invalidatePendingOffersForListing(20L);
        verify(notificationService).createNotification(
                eq(30L),
                eq(NotificationType.LISTING_REMOVED),
                any(String.class),
                eq("Listing removed for policy review"),
                eq(listingUuid),
                eq("ITEM"));

        ArgumentCaptor<ListingModerationActionEntity> actionCaptor = ArgumentCaptor.forClass(ListingModerationActionEntity.class);
        verify(listingModerationActionRepository).save(actionCaptor.capture());
        assertEquals(ListingModerationReasonCode.POLICY_VIOLATION, actionCaptor.getValue().getReasonCode());
    }

    @Test
    void restoreListingMovesRemovedListingBackToActiveCreatesHistoryAndNotifiesOwner() {
        UUID adminUuid = UUID.randomUUID();
        UUID listingUuid = UUID.randomUUID();
        UserEntity admin = admin(adminUuid);
        ItemEntity listing = listing(listingUuid, ItemStatus.REMOVED);
        listing.setRemovedAt(OffsetDateTime.now().minusDays(1));
        listing.setArchivedAt(OffsetDateTime.now().minusDays(2));
        AdminListingDetailResponse expected = new AdminListingDetailResponse().uuid(listingUuid);

        when(userRepository.findByUuid(adminUuid)).thenReturn(Optional.of(admin));
        when(itemRepository.findByUuid(listingUuid)).thenReturn(Optional.of(listing));
        when(itemRepository.save(any(ItemEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(adminListingQueryService.getListing(listingUuid)).thenReturn(expected);

        AdminRestoreListingRequest request = new AdminRestoreListingRequest()
                .reasonCode(com.barterplatform.api.model.ListingModerationReasonCode.OWNER_REQUEST)
                .userMessage("Listing restored")
                .internalNote("restored after review");

        AdminListingDetailResponse result = service.restoreListing(adminUuid, listingUuid, request);

        assertEquals(expected, result);
        assertEquals(ItemStatus.ACTIVE, listing.getStatus());
        assertNull(listing.getRemovedAt());
        assertNull(listing.getArchivedAt());
        verify(notificationService).createNotification(
                eq(30L),
                eq(NotificationType.LISTING_RESTORED),
                any(String.class),
                eq("Listing restored"),
                eq(listingUuid),
                eq("ITEM"));
        verify(tradeOfferInvalidationService, never()).invalidatePendingOffersForListing(any());
    }

    @Test
    void restoreListingRejectsNonRemovedListing() {
        UUID adminUuid = UUID.randomUUID();
        UUID listingUuid = UUID.randomUUID();
        when(userRepository.findByUuid(adminUuid)).thenReturn(Optional.of(admin(adminUuid)));
        when(itemRepository.findByUuid(listingUuid)).thenReturn(Optional.of(listing(listingUuid, ItemStatus.ACTIVE)));

        AdminRestoreListingRequest request = new AdminRestoreListingRequest()
                .reasonCode(com.barterplatform.api.model.ListingModerationReasonCode.OWNER_REQUEST);

        ApiException exception = assertThrows(ApiException.class, () -> service.restoreListing(adminUuid, listingUuid, request));

        assertEquals(409, exception.getStatus().value());
        verify(notificationService, never()).createNotification(any(), any(), any(), any(), any(), any());
    }

    private UserEntity admin(UUID uuid) {
        UserEntity user = new UserEntity();
        user.setId(10L);
        user.setUuid(uuid);
        user.setUsername("admin");
        user.setEmail("admin@test.local");
        user.setCreatedAt(OffsetDateTime.now());
        return user;
    }

    private ItemEntity listing(UUID uuid, ItemStatus status) {
        ItemEntity item = new ItemEntity();
        item.setId(20L);
        item.setUuid(uuid);
        item.setOwnerId(30L);
        item.setCategoryId(999L);
        item.setTitle("Listing");
        item.setCondition(ItemCondition.GOOD);
        item.setStatus(status);
        item.setCreatedAt(OffsetDateTime.now());
        return item;
    }
}

