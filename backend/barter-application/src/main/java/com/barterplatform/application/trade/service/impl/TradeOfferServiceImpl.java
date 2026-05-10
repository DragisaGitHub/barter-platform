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
import com.barterplatform.domain.trade.enums.TradeOfferStatus;
import com.barterplatform.infrastructure.catalog.repository.CategoryRepository;
import com.barterplatform.infrastructure.catalog.repository.ItemRepository;
import com.barterplatform.infrastructure.identity.repository.UserRepository;
import com.barterplatform.infrastructure.trade.repository.TradeOfferRepository;
import java.time.OffsetDateTime;
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
    private final UserRepository userRepository;
    private final ItemRepository itemRepository;
    private final CategoryRepository categoryRepository;
    private final TradeOfferMapper tradeOfferMapper;
    private final PageRequestFactory pageRequestFactory;
    private final PageResponseMapper pageResponseMapper;

    public TradeOfferServiceImpl(TradeOfferRepository tradeOfferRepository,
                                 UserRepository userRepository,
                                 ItemRepository itemRepository,
                                 CategoryRepository categoryRepository,
                                 TradeOfferMapper tradeOfferMapper,
                                 PageRequestFactory pageRequestFactory,
                                 PageResponseMapper pageResponseMapper) {
        this.tradeOfferRepository = tradeOfferRepository;
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

        ItemEntity senderItem = resolveItem(request.getSenderItemUuid());
        ItemEntity receiverItem = resolveItem(request.getReceiverItemUuid());

        // Sender item must belong to sender
        if (!senderItem.getOwnerId().equals(sender.getId())) {
            throw forbidden("Sender item does not belong to the authenticated user.");
        }

        // Receiver item must NOT belong to sender (no self-offers)
        if (receiverItem.getOwnerId().equals(sender.getId())) {
            throw forbidden("Cannot create a trade offer for your own item.");
        }

        // Both items must be ACTIVE
        if (senderItem.getStatus() != ItemStatus.ACTIVE) {
            throw conflict("Sender item is not active.");
        }
        if (receiverItem.getStatus() != ItemStatus.ACTIVE) {
            throw conflict("Receiver item is not active.");
        }

        // Derive receiver user from receiver item owner
        UserEntity receiver = userRepository.findById(receiverItem.getOwnerId())
                .orElseThrow(() -> notFound("Owner of receiver item was not found."));

        // Build and save the offer
        TradeOfferEntity offer = new TradeOfferEntity();
        offer.setSenderUserId(sender.getId());
        offer.setReceiverUserId(receiver.getId());
        offer.setSenderItemId(senderItem.getId());
        offer.setReceiverItemId(receiverItem.getId());
        offer.setStatus(TradeOfferStatus.PENDING);
        offer.setMessage(request.getMessage());

        TradeOfferEntity saved = tradeOfferRepository.save(offer);

        return toResponse(saved, sender, receiver, senderItem, receiverItem);
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

        // Both items must still be ACTIVE
        ItemEntity senderItem = itemRepository.findById(offer.getSenderItemId())
                .orElseThrow(() -> notFound("Sender item was not found."));
        ItemEntity receiverItem = itemRepository.findById(offer.getReceiverItemId())
                .orElseThrow(() -> notFound("Receiver item was not found."));

        if (senderItem.getStatus() != ItemStatus.ACTIVE) {
            throw conflict("Sender item is no longer active.");
        }
        if (receiverItem.getStatus() != ItemStatus.ACTIVE) {
            throw conflict("Receiver item is no longer active.");
        }

        // Archive both items
        archiveItem(senderItem);
        archiveItem(receiverItem);

        TradeOfferEntity saved = tradeOfferRepository.save(offer);

        // Reject all competing PENDING offers involving either item
        List<TradeOfferEntity> competing = tradeOfferRepository.findCompetingPendingOffers(
                saved.getId(), saved.getSenderItemId(), saved.getReceiverItemId());
        for (TradeOfferEntity comp : competing) {
            comp.reject();
            tradeOfferRepository.save(comp);
        }

        UserEntity sender = userRepository.findById(saved.getSenderUserId())
                .orElseThrow(() -> notFound("Sender user was not found."));

        return toResponse(saved, sender, user, senderItem, receiverItem);
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
        ItemEntity senderItem = itemRepository.findById(offer.getSenderItemId())
                .orElseThrow(() -> notFound("Sender item was not found."));
        ItemEntity receiverItem = itemRepository.findById(offer.getReceiverItemId())
                .orElseThrow(() -> notFound("Receiver item was not found."));

        return toResponse(offer, sender, receiver, senderItem, receiverItem);
    }

    private TradeOfferResponse toResponse(TradeOfferEntity offer,
                                           UserEntity sender,
                                           UserEntity receiver,
                                           ItemEntity senderItem,
                                           ItemEntity receiverItem) {
        CategoryEntity senderCategory = resolveCategory(senderItem.getCategoryId());
        CategoryEntity receiverCategory = resolveCategory(receiverItem.getCategoryId());

        return tradeOfferMapper.toResponse(offer, sender, receiver,
                senderItem, senderCategory, receiverItem, receiverCategory);
    }

    /**
     * Build a TradeOfferSummaryResponse for list endpoints.
     */
    private TradeOfferSummaryResponse toSummaryResponse(TradeOfferEntity offer) {
        UserEntity sender = userRepository.findById(offer.getSenderUserId())
                .orElseThrow(() -> notFound("Sender user was not found."));
        UserEntity receiver = userRepository.findById(offer.getReceiverUserId())
                .orElseThrow(() -> notFound("Receiver user was not found."));
        ItemEntity senderItem = itemRepository.findById(offer.getSenderItemId())
                .orElseThrow(() -> notFound("Sender item was not found."));
        ItemEntity receiverItem = itemRepository.findById(offer.getReceiverItemId())
                .orElseThrow(() -> notFound("Receiver item was not found."));
        CategoryEntity senderCategory = resolveCategory(senderItem.getCategoryId());
        CategoryEntity receiverCategory = resolveCategory(receiverItem.getCategoryId());

        return tradeOfferMapper.toSummaryResponse(offer, sender, receiver,
                senderItem, senderCategory, receiverItem, receiverCategory);
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
}

