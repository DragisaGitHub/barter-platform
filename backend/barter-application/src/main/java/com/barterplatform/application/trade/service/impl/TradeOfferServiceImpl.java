package com.barterplatform.application.trade.service.impl;

import com.barterplatform.api.model.CreateTradeOfferRequest;
import com.barterplatform.api.model.TradeOfferPagedResponse;
import com.barterplatform.api.model.TradeOfferResponse;
import com.barterplatform.api.model.TradeOfferSummaryResponse;
import com.barterplatform.application.common.pagination.PageRequestFactory;
import com.barterplatform.application.common.pagination.PageResponseMapper;
import com.barterplatform.application.trade.mapper.TradeOfferMapper;
import com.barterplatform.application.trade.service.TradeOfferService;
import com.barterplatform.common.exception.ApiException;
import com.barterplatform.common.exception.ErrorCode;
import com.barterplatform.domain.catalog.entity.CategoryEntity;
import com.barterplatform.domain.catalog.entity.ItemEntity;
import com.barterplatform.domain.catalog.enums.ItemStatus;
import com.barterplatform.domain.identity.entity.UserEntity;
import com.barterplatform.domain.trade.entity.TradeOfferEntity;
import com.barterplatform.domain.trade.entity.TradeOfferItemEntity;
import com.barterplatform.domain.trade.enums.TradeOfferItemSide;
import com.barterplatform.domain.trade.enums.TradeOfferMode;
import com.barterplatform.domain.trade.enums.TradeOfferStatus;
import com.barterplatform.infrastructure.catalog.repository.CategoryRepository;
import com.barterplatform.infrastructure.catalog.repository.ItemRepository;
import com.barterplatform.infrastructure.identity.repository.UserRepository;
import com.barterplatform.infrastructure.trade.repository.TradeOfferItemRepository;
import com.barterplatform.infrastructure.trade.repository.TradeOfferRepository;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TradeOfferServiceImpl implements TradeOfferService {

    private static final String DEFAULT_SORT_FIELD = "createdAt";
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("createdAt", "status", "respondedAt");

    private final TradeOfferRepository tradeOfferRepository;
    private final TradeOfferItemRepository tradeOfferItemRepository;
    private final UserRepository userRepository;
    private final ItemRepository itemRepository;
    private final CategoryRepository categoryRepository;
    private final TradeOfferMapper tradeOfferMapper;
    private final PageRequestFactory pageRequestFactory;
    private final PageResponseMapper pageResponseMapper;

    public TradeOfferServiceImpl(TradeOfferRepository tradeOfferRepository,
                                 TradeOfferItemRepository tradeOfferItemRepository,
                                 UserRepository userRepository,
                                 ItemRepository itemRepository,
                                 CategoryRepository categoryRepository,
                                 TradeOfferMapper tradeOfferMapper,
                                 PageRequestFactory pageRequestFactory,
                                 PageResponseMapper pageResponseMapper) {
        this.tradeOfferRepository = tradeOfferRepository;
        this.tradeOfferItemRepository = tradeOfferItemRepository;
        this.userRepository = userRepository;
        this.itemRepository = itemRepository;
        this.categoryRepository = categoryRepository;
        this.tradeOfferMapper = tradeOfferMapper;
        this.pageRequestFactory = pageRequestFactory;
        this.pageResponseMapper = pageResponseMapper;
    }

    // ── Create ───────────────────────────────────────────────────

    @Override
    public TradeOfferResponse createOffer(UUID currentUserUuid, CreateTradeOfferRequest request) {
        UserEntity sender = resolveUser(currentUserUuid);

        // Resolve mode
        TradeOfferMode mode = TradeOfferMode.valueOf(request.getMode().name());
        List<UUID> senderItemUuids = request.getSenderItemUuids();

        // Validate mode-specific rules
        validateModeConstraints(mode, senderItemUuids, request.getMessage());

        // Resolve receiver item
        ItemEntity receiverItem = resolveItem(request.getReceiverItemUuid());

        // Receiver item must NOT belong to sender (no self-offers)
        if (receiverItem.getOwnerId().equals(sender.getId())) {
            throw forbidden("Cannot create a trade offer for your own item.");
        }

        // Receiver item must be ACTIVE
        if (receiverItem.getStatus() != ItemStatus.ACTIVE) {
            throw conflict("Receiver item is not active.");
        }

        // Resolve and validate sender items
        List<ItemEntity> senderItems = resolveSenderItems(senderItemUuids, sender);

        // Derive receiver user from receiver item owner
        UserEntity receiver = userRepository.findById(receiverItem.getOwnerId())
                .orElseThrow(() -> notFound("Owner of receiver item was not found."));

        // Build and save the offer
        TradeOfferEntity offer = new TradeOfferEntity();
        offer.setSenderUserId(sender.getId());
        offer.setReceiverUserId(receiver.getId());
        offer.setSenderItemId(senderItems.isEmpty() ? null : senderItems.getFirst().getId());
        offer.setReceiverItemId(receiverItem.getId());
        offer.setMode(mode);
        offer.setStatus(TradeOfferStatus.PENDING);
        offer.setMessage(request.getMessage());

        TradeOfferEntity saved = tradeOfferRepository.save(offer);

        // Create trade_offer_items entries
        for (ItemEntity senderItem : senderItems) {
            TradeOfferItemEntity toi = new TradeOfferItemEntity();
            toi.setTradeOffer(saved);
            toi.setItemId(senderItem.getId());
            toi.setSide(TradeOfferItemSide.OFFERED);
            saved.getItems().add(toi);
        }

        TradeOfferItemEntity requestedToi = new TradeOfferItemEntity();
        requestedToi.setTradeOffer(saved);
        requestedToi.setItemId(receiverItem.getId());
        requestedToi.setSide(TradeOfferItemSide.REQUESTED);
        saved.getItems().add(requestedToi);

        tradeOfferRepository.save(saved);

        return toResponse(saved, sender, receiver, senderItems, receiverItem);
    }

    // ── List Incoming ────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public TradeOfferPagedResponse listIncoming(UUID currentUserUuid, Integer page, Integer size,
                                                 String sort, TradeOfferStatus status) {
        UserEntity user = resolveUser(currentUserUuid);
        PageRequestFactory.ResolvedPageRequest resolved =
                pageRequestFactory.create(page, size, sort, DEFAULT_SORT_FIELD, ALLOWED_SORT_FIELDS);

        Page<TradeOfferEntity> offers = (status != null)
                ? tradeOfferRepository.findByReceiverUserIdAndStatus(user.getId(), status, resolved.pageable())
                : tradeOfferRepository.findByReceiverUserId(user.getId(), resolved.pageable());

        List<TradeOfferSummaryResponse> content = offers.getContent().stream()
                .map(this::toSummaryResponse)
                .toList();

        return pageResponseMapper.toTradeOfferPagedResponse(offers, content, resolved.sort());
    }

    // ── List Sent ────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public TradeOfferPagedResponse listSent(UUID currentUserUuid, Integer page, Integer size,
                                             String sort, TradeOfferStatus status) {
        UserEntity user = resolveUser(currentUserUuid);
        PageRequestFactory.ResolvedPageRequest resolved =
                pageRequestFactory.create(page, size, sort, DEFAULT_SORT_FIELD, ALLOWED_SORT_FIELDS);

        Page<TradeOfferEntity> offers = (status != null)
                ? tradeOfferRepository.findBySenderUserIdAndStatus(user.getId(), status, resolved.pageable())
                : tradeOfferRepository.findBySenderUserId(user.getId(), resolved.pageable());

        List<TradeOfferSummaryResponse> content = offers.getContent().stream()
                .map(this::toSummaryResponse)
                .toList();

        return pageResponseMapper.toTradeOfferPagedResponse(offers, content, resolved.sort());
    }

    // ── Get ──────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public TradeOfferResponse getOffer(UUID currentUserUuid, UUID offerUuid) {
        UserEntity user = resolveUser(currentUserUuid);
        TradeOfferEntity offer = resolveOffer(offerUuid);

        // Only sender or receiver can view
        if (!isParticipant(offer, user)) {
            throw forbidden("You are not a participant of this trade offer.");
        }

        return toFullResponse(offer);
    }

    // ── Accept ───────────────────────────────────────────────────

    @Override
    public TradeOfferResponse acceptOffer(UUID currentUserUuid, UUID offerUuid) {
        UserEntity user = resolveUser(currentUserUuid);
        TradeOfferEntity offer = resolveOffer(offerUuid);

        // Only receiver can accept
        if (!offer.getReceiverUserId().equals(user.getId())) {
            throw forbidden("Only the receiver can accept a trade offer.");
        }

        // Only PENDING can be accepted (domain method handles assertion)
        try {
            offer.accept();
        } catch (IllegalStateException e) {
            throw conflict(e.getMessage());
        }

        // Receiver item must still be ACTIVE
        ItemEntity receiverItem = itemRepository.findById(offer.getReceiverItemId())
                .orElseThrow(() -> notFound("Receiver item was not found."));
        if (receiverItem.getStatus() != ItemStatus.ACTIVE) {
            throw conflict("Receiver item is no longer active.");
        }

        // Resolve offered items from trade_offer_items
        List<Long> offeredItemIds = tradeOfferItemRepository.findItemIdsByTradeOfferIdAndSide(
                offer.getId(), TradeOfferItemSide.OFFERED);
        List<ItemEntity> offeredItems = new ArrayList<>();

        for (Long itemId : offeredItemIds) {
            ItemEntity item = itemRepository.findById(itemId)
                    .orElseThrow(() -> notFound("Offered item was not found."));
            if (item.getStatus() != ItemStatus.ACTIVE) {
                throw conflict("Offered item '" + item.getTitle() + "' is no longer active.");
            }
            offeredItems.add(item);
        }

        // Archive receiver item
        archiveItem(receiverItem);

        // Archive all offered items
        for (ItemEntity item : offeredItems) {
            archiveItem(item);
        }

        TradeOfferEntity saved = tradeOfferRepository.save(offer);

        // Collect all item IDs involved in this trade for competing offer detection
        Set<Long> involvedItemIds = new HashSet<>();
        involvedItemIds.add(saved.getReceiverItemId());
        involvedItemIds.addAll(offeredItemIds);

        // Reject all competing PENDING offers involving any accepted item
        List<TradeOfferEntity> competing = tradeOfferRepository.findCompetingPendingOffers(
                saved.getId(), involvedItemIds);
        for (TradeOfferEntity comp : competing) {
            comp.reject();
            tradeOfferRepository.save(comp);
        }

        UserEntity sender = userRepository.findById(saved.getSenderUserId())
                .orElseThrow(() -> notFound("Sender user was not found."));

        return toResponse(saved, sender, user, offeredItems, receiverItem);
    }

    // ── Reject ───────────────────────────────────────────────────

    @Override
    public TradeOfferResponse rejectOffer(UUID currentUserUuid, UUID offerUuid) {
        UserEntity user = resolveUser(currentUserUuid);
        TradeOfferEntity offer = resolveOffer(offerUuid);

        // Only receiver can reject
        if (!offer.getReceiverUserId().equals(user.getId())) {
            throw forbidden("Only the receiver can reject a trade offer.");
        }

        try {
            offer.reject();
        } catch (IllegalStateException e) {
            throw conflict(e.getMessage());
        }

        TradeOfferEntity saved = tradeOfferRepository.save(offer);
        return toFullResponse(saved);
    }

    // ── Cancel ───────────────────────────────────────────────────

    @Override
    public TradeOfferResponse cancelOffer(UUID currentUserUuid, UUID offerUuid) {
        UserEntity user = resolveUser(currentUserUuid);
        TradeOfferEntity offer = resolveOffer(offerUuid);

        // Only sender can cancel
        if (!offer.getSenderUserId().equals(user.getId())) {
            throw forbidden("Only the sender can cancel a trade offer.");
        }

        try {
            offer.cancel();
        } catch (IllegalStateException e) {
            throw conflict(e.getMessage());
        }

        TradeOfferEntity saved = tradeOfferRepository.save(offer);
        return toFullResponse(saved);
    }

    // ── Private helpers ──────────────────────────────────────────

    private void validateModeConstraints(TradeOfferMode mode, List<UUID> senderItemUuids, String message) {
        boolean hasSenderItems = senderItemUuids != null && !senderItemUuids.isEmpty();

        switch (mode) {
            case ITEM_EXCHANGE -> {
                if (!hasSenderItems) {
                    throw badRequest("ITEM_EXCHANGE mode requires at least one sender item.");
                }
            }
            case GIFT -> {
                if (hasSenderItems) {
                    throw badRequest("GIFT mode does not allow sender items.");
                }
                if (message == null || message.isBlank()) {
                    throw badRequest("Message is required for GIFT mode.");
                }
            }
            case NEGOTIABLE -> {
                if (message == null || message.isBlank()) {
                    throw badRequest("Message is required for NEGOTIABLE mode.");
                }
            }
        }

        // Check for duplicates
        if (hasSenderItems) {
            Set<UUID> unique = new HashSet<>(senderItemUuids);
            if (unique.size() != senderItemUuids.size()) {
                throw badRequest("Duplicate sender item UUIDs are not allowed.");
            }
        }
    }

    private List<ItemEntity> resolveSenderItems(List<UUID> senderItemUuids, UserEntity sender) {
        if (senderItemUuids == null || senderItemUuids.isEmpty()) {
            return List.of();
        }

        List<ItemEntity> items = new ArrayList<>();
        for (UUID uuid : senderItemUuids) {
            ItemEntity item = resolveItem(uuid);
            if (!item.getOwnerId().equals(sender.getId())) {
                throw forbidden("Sender item '%s' does not belong to the authenticated user.".formatted(uuid));
            }
            if (item.getStatus() != ItemStatus.ACTIVE) {
                throw conflict("Sender item '%s' is not active.".formatted(uuid));
            }
            items.add(item);
        }
        return items;
    }

    private UserEntity resolveUser(UUID userUuid) {
        return userRepository.findByUuid(userUuid)
                .orElseThrow(() -> notFound("User with uuid '%s' was not found.", userUuid));
    }

    private ItemEntity resolveItem(UUID itemUuid) {
        return itemRepository.findByUuid(itemUuid)
                .orElseThrow(() -> notFound("Item with uuid '%s' was not found.", itemUuid));
    }

    private TradeOfferEntity resolveOffer(UUID offerUuid) {
        return tradeOfferRepository.findByUuid(offerUuid)
                .orElseThrow(() -> notFound("Trade offer with uuid '%s' was not found.", offerUuid));
    }

    private boolean isParticipant(TradeOfferEntity offer, UserEntity user) {
        return offer.getSenderUserId().equals(user.getId())
                || offer.getReceiverUserId().equals(user.getId());
    }

    private void archiveItem(ItemEntity item) {
        item.setStatus(ItemStatus.ARCHIVED);
        item.setArchivedAt(OffsetDateTime.now());
        itemRepository.save(item);
    }

    private CategoryEntity resolveCategory(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> notFound("Category was not found."));
    }

    /**
     * Build a full TradeOfferResponse with all embedded summaries.
     */
    private TradeOfferResponse toFullResponse(TradeOfferEntity offer) {
        UserEntity sender = userRepository.findById(offer.getSenderUserId())
                .orElseThrow(() -> notFound("Sender user was not found."));
        UserEntity receiver = userRepository.findById(offer.getReceiverUserId())
                .orElseThrow(() -> notFound("Receiver user was not found."));
        ItemEntity receiverItem = itemRepository.findById(offer.getReceiverItemId())
                .orElseThrow(() -> notFound("Receiver item was not found."));

        // Resolve offered items from trade_offer_items
        List<Long> offeredItemIds = tradeOfferItemRepository.findItemIdsByTradeOfferIdAndSide(
                offer.getId(), TradeOfferItemSide.OFFERED);
        List<ItemEntity> offeredItems = offeredItemIds.stream()
                .map(id -> itemRepository.findById(id)
                        .orElseThrow(() -> notFound("Offered item was not found.")))
                .toList();

        return toResponse(offer, sender, receiver, offeredItems, receiverItem);
    }

    private TradeOfferResponse toResponse(TradeOfferEntity offer,
                                           UserEntity sender,
                                           UserEntity receiver,
                                           List<ItemEntity> offeredItems,
                                           ItemEntity receiverItem) {
        CategoryEntity receiverCategory = resolveCategory(receiverItem.getCategoryId());
        List<CategoryEntity> offeredCategories = offeredItems.stream()
                .map(item -> resolveCategory(item.getCategoryId()))
                .toList();

        return tradeOfferMapper.toResponse(offer, sender, receiver,
                receiverItem, receiverCategory, offeredItems, offeredCategories);
    }

    /**
     * Build a TradeOfferSummaryResponse for list endpoints.
     */
    private TradeOfferSummaryResponse toSummaryResponse(TradeOfferEntity offer) {
        UserEntity sender = userRepository.findById(offer.getSenderUserId())
                .orElseThrow(() -> notFound("Sender user was not found."));
        UserEntity receiver = userRepository.findById(offer.getReceiverUserId())
                .orElseThrow(() -> notFound("Receiver user was not found."));
        ItemEntity receiverItem = itemRepository.findById(offer.getReceiverItemId())
                .orElseThrow(() -> notFound("Receiver item was not found."));
        CategoryEntity receiverCategory = resolveCategory(receiverItem.getCategoryId());

        // Resolve offered items from trade_offer_items
        List<Long> offeredItemIds = tradeOfferItemRepository.findItemIdsByTradeOfferIdAndSide(
                offer.getId(), TradeOfferItemSide.OFFERED);
        List<ItemEntity> offeredItems = offeredItemIds.stream()
                .map(id -> itemRepository.findById(id)
                        .orElseThrow(() -> notFound("Offered item was not found.")))
                .toList();
        List<CategoryEntity> offeredCategories = offeredItems.stream()
                .map(item -> resolveCategory(item.getCategoryId()))
                .toList();

        return tradeOfferMapper.toSummaryResponse(offer, sender, receiver,
                receiverItem, receiverCategory, offeredItems, offeredCategories);
    }

    private ApiException notFound(String messageTemplate, Object... args) {
        return new ApiException(
                HttpStatus.NOT_FOUND,
                ErrorCode.NOT_FOUND,
                messageTemplate.formatted(args));
    }

    private ApiException forbidden(String message) {
        return new ApiException(
                HttpStatus.FORBIDDEN,
                ErrorCode.FORBIDDEN,
                message);
    }

    private ApiException conflict(String message) {
        return new ApiException(
                HttpStatus.CONFLICT,
                ErrorCode.CONFLICT,
                message);
    }

    private ApiException badRequest(String message) {
        return new ApiException(
                HttpStatus.BAD_REQUEST,
                ErrorCode.BAD_REQUEST,
                message);
    }
}

