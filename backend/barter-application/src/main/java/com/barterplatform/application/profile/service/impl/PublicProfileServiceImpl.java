package com.barterplatform.application.profile.service.impl;

import com.barterplatform.api.model.ItemPagedResponse;
import com.barterplatform.api.model.ItemSummaryResponse;
import com.barterplatform.api.model.PublicProfileResponse;
import com.barterplatform.application.catalog.mapper.ItemImageMapper;
import com.barterplatform.application.catalog.mapper.ItemMapper;
import com.barterplatform.application.common.pagination.PageRequestFactory;
import com.barterplatform.application.common.pagination.PageResponseMapper;
import com.barterplatform.application.profile.service.PublicProfileService;
import com.barterplatform.common.exception.ApiException;
import com.barterplatform.common.exception.ErrorCode;
import com.barterplatform.domain.catalog.entity.CategoryEntity;
import com.barterplatform.domain.catalog.entity.ItemEntity;
import com.barterplatform.domain.catalog.enums.ItemStatus;
import com.barterplatform.domain.identity.entity.UserEntity;
import com.barterplatform.domain.identity.enums.UserStatus;
import com.barterplatform.domain.trade.enums.TradeOfferStatus;
import com.barterplatform.infrastructure.catalog.repository.CategoryRepository;
import com.barterplatform.infrastructure.catalog.repository.ItemImageRepository;
import com.barterplatform.infrastructure.catalog.repository.ItemRepository;
import com.barterplatform.infrastructure.identity.repository.UserRepository;
import com.barterplatform.infrastructure.trade.repository.TradeOfferRepository;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PublicProfileServiceImpl implements PublicProfileService {

    private static final String DEFAULT_ITEM_SORT_FIELD = "createdAt";
    private static final Set<String> ALLOWED_ITEM_SORT_FIELDS = Set.of(
            "createdAt", "updatedAt", "title");

    private final UserRepository userRepository;
    private final ItemRepository itemRepository;
    private final TradeOfferRepository tradeOfferRepository;
    private final CategoryRepository categoryRepository;
    private final ItemImageRepository itemImageRepository;
    private final ItemMapper itemMapper;
    private final ItemImageMapper itemImageMapper;
    private final PageRequestFactory pageRequestFactory;
    private final PageResponseMapper pageResponseMapper;

    public PublicProfileServiceImpl(UserRepository userRepository,
                                    ItemRepository itemRepository,
                                    TradeOfferRepository tradeOfferRepository,
                                    CategoryRepository categoryRepository,
                                    ItemImageRepository itemImageRepository,
                                    ItemMapper itemMapper,
                                    ItemImageMapper itemImageMapper,
                                    PageRequestFactory pageRequestFactory,
                                    PageResponseMapper pageResponseMapper) {
        this.userRepository = userRepository;
        this.itemRepository = itemRepository;
        this.tradeOfferRepository = tradeOfferRepository;
        this.categoryRepository = categoryRepository;
        this.itemImageRepository = itemImageRepository;
        this.itemMapper = itemMapper;
        this.itemImageMapper = itemImageMapper;
        this.pageRequestFactory = pageRequestFactory;
        this.pageResponseMapper = pageResponseMapper;
    }

    @Override
    public PublicProfileResponse getPublicProfile(UUID userUuid) {
        UserEntity user = findActiveUser(userUuid);

        int activeItemCount = (int) itemRepository.countByOwnerIdAndStatus(user.getId(), ItemStatus.ACTIVE);

        long completedAsSender = tradeOfferRepository.countBySenderUserIdAndStatus(
                user.getId(), TradeOfferStatus.ACCEPTED);
        long completedAsReceiver = tradeOfferRepository.countByReceiverUserIdAndStatus(
                user.getId(), TradeOfferStatus.ACCEPTED);
        int completedTradeCount = (int) (completedAsSender + completedAsReceiver);

        long cancelledAsSender = tradeOfferRepository.countBySenderUserIdAndStatus(
                user.getId(), TradeOfferStatus.CANCELLED);
        long cancelledAsReceiver = tradeOfferRepository.countByReceiverUserIdAndStatus(
                user.getId(), TradeOfferStatus.CANCELLED);
        int cancelledTradeCount = (int) (cancelledAsSender + cancelledAsReceiver);

        return new PublicProfileResponse()
                .uuid(user.getUuid())
                .username(user.getUsername())
                .joinedAt(user.getCreatedAt())
                .activeItemCount(activeItemCount)
                .completedTradeCount(completedTradeCount)
                .cancelledTradeCount(cancelledTradeCount)
                .averageRating(null);
    }

    @Override
    public ItemPagedResponse listPublicItems(UUID userUuid, Integer page, Integer size, String sort) {
        UserEntity user = findActiveUser(userUuid);

        PageRequestFactory.ResolvedPageRequest pageRequest = pageRequestFactory.create(
                page, size, sort, DEFAULT_ITEM_SORT_FIELD, ALLOWED_ITEM_SORT_FIELDS);

        Page<ItemEntity> itemPage = itemRepository.findByOwnerIdAndStatusAndDeletedAtIsNull(
                user.getId(), ItemStatus.ACTIVE, pageRequest.pageable());

        List<ItemSummaryResponse> content = mapItemSummaries(itemPage.getContent(), user);

        return pageResponseMapper.toItemPagedResponse(itemPage, content, pageRequest.sort());
    }

    // ── Private helpers ──────────────────────────────────────────

    private UserEntity findActiveUser(UUID userUuid) {
        UserEntity user = userRepository.findByUuid(userUuid)
                .orElseThrow(() -> notFound("User with uuid '%s' was not found.", userUuid));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw notFound("User with uuid '%s' was not found.", userUuid);
        }

        return user;
    }

    private List<ItemSummaryResponse> mapItemSummaries(List<ItemEntity> items, UserEntity owner) {
        if (items.isEmpty()) {
            return List.of();
        }

        // Batch-load categories
        Set<Long> categoryIds = items.stream()
                .map(ItemEntity::getCategoryId)
                .collect(Collectors.toSet());
        Map<Long, CategoryEntity> categoriesById = categoryRepository.findAllById(categoryIds).stream()
                .collect(Collectors.toMap(CategoryEntity::getId, Function.identity()));

        // Batch-load primary images
        Set<Long> itemIds = items.stream()
                .map(ItemEntity::getId)
                .collect(Collectors.toSet());
        Map<Long, String> primaryImageUrlByItemId = new java.util.HashMap<>();
        for (Long itemId : itemIds) {
            itemImageRepository.findFirstByItemIdAndPrimaryTrue(itemId)
                    .ifPresent(img -> primaryImageUrlByItemId.put(itemId,
                            itemImageMapper.toResponse(img).getUrl()));
        }

        return items.stream().map(item -> {
            CategoryEntity category = categoriesById.get(item.getCategoryId());
            String primaryImageUrl = primaryImageUrlByItemId.get(item.getId());
            return itemMapper.toSummaryResponse(
                    item,
                    category,
                    owner.getUuid(),
                    owner.getUsername(),
                    primaryImageUrl);
        }).toList();
    }

    private ApiException notFound(String messageTemplate, Object... args) {
        return new ApiException(
                HttpStatus.NOT_FOUND,
                ErrorCode.NOT_FOUND,
                messageTemplate.formatted(args));
    }
}

