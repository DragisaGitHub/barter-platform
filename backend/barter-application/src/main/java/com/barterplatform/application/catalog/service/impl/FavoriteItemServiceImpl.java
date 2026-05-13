package com.barterplatform.application.catalog.service.impl;

import com.barterplatform.api.model.ItemPagedResponse;
import com.barterplatform.api.model.ItemSummaryResponse;
import com.barterplatform.api.model.MessageResponse;
import com.barterplatform.application.catalog.mapper.ItemImageMapper;
import com.barterplatform.application.catalog.mapper.ItemMapper;
import com.barterplatform.application.catalog.service.FavoriteItemService;
import com.barterplatform.application.common.pagination.PageRequestFactory;
import com.barterplatform.application.common.pagination.PageResponseMapper;
import com.barterplatform.common.exception.ApiException;
import com.barterplatform.common.exception.ErrorCode;
import com.barterplatform.domain.catalog.entity.CategoryEntity;
import com.barterplatform.domain.catalog.entity.FavoriteItemEntity;
import com.barterplatform.domain.catalog.entity.ItemEntity;
import com.barterplatform.domain.catalog.entity.ItemImageEntity;
import com.barterplatform.domain.catalog.enums.ItemStatus;
import com.barterplatform.domain.identity.entity.UserEntity;
import com.barterplatform.infrastructure.catalog.repository.CategoryRepository;
import com.barterplatform.infrastructure.catalog.repository.FavoriteItemRepository;
import com.barterplatform.infrastructure.catalog.repository.ItemImageRepository;
import com.barterplatform.infrastructure.catalog.repository.ItemRepository;
import com.barterplatform.infrastructure.identity.repository.UserRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class FavoriteItemServiceImpl implements FavoriteItemService {

    private static final String DEFAULT_SORT = "createdAt,desc";
    private static final String DEFAULT_SORT_FIELD = "createdAt";
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("createdAt");
    private static final String FAVORITE_SUCCESS_MESSAGE = "Item favorited successfully.";

    private final FavoriteItemRepository favoriteItemRepository;
    private final UserRepository userRepository;
    private final ItemRepository itemRepository;
    private final CategoryRepository categoryRepository;
    private final ItemImageRepository itemImageRepository;
    private final ItemImageMapper itemImageMapper;
    private final ItemMapper itemMapper;
    private final PageRequestFactory pageRequestFactory;
    private final PageResponseMapper pageResponseMapper;

    public FavoriteItemServiceImpl(FavoriteItemRepository favoriteItemRepository,
                                   UserRepository userRepository,
                                   ItemRepository itemRepository,
                                   CategoryRepository categoryRepository,
                                   ItemImageRepository itemImageRepository,
                                   ItemImageMapper itemImageMapper,
                                   ItemMapper itemMapper,
                                   PageRequestFactory pageRequestFactory,
                                   PageResponseMapper pageResponseMapper) {
        this.favoriteItemRepository = favoriteItemRepository;
        this.userRepository = userRepository;
        this.itemRepository = itemRepository;
        this.categoryRepository = categoryRepository;
        this.itemImageRepository = itemImageRepository;
        this.itemImageMapper = itemImageMapper;
        this.itemMapper = itemMapper;
        this.pageRequestFactory = pageRequestFactory;
        this.pageResponseMapper = pageResponseMapper;
    }

    @Override
    public MessageResponse favoriteItem(UUID currentUserUuid, UUID itemUuid) {
        UserEntity currentUser = resolveUser(currentUserUuid);
        ItemEntity item = resolveItem(itemUuid);

        if (favoriteItemRepository.existsByUserIdAndItemId(currentUser.getId(), item.getId())) {
            return successResponse();
        }

        FavoriteItemEntity favoriteItem = new FavoriteItemEntity();
        favoriteItem.setUserId(currentUser.getId());
        favoriteItem.setItemId(item.getId());

        try {
            favoriteItemRepository.save(favoriteItem);
        } catch (DataIntegrityViolationException ignored) {
            // Preserve idempotency in concurrent requests when the unique constraint wins the race.
        }

        return successResponse();
    }

    @Override
    public void unfavoriteItem(UUID currentUserUuid, UUID itemUuid) {
        UserEntity currentUser = resolveUser(currentUserUuid);
        ItemEntity item = resolveItem(itemUuid);

        favoriteItemRepository.findByUserIdAndItemId(currentUser.getId(), item.getId())
                .ifPresent(favoriteItemRepository::delete);
    }

    @Override
    @Transactional(readOnly = true)
    public ItemPagedResponse listFavoriteItems(UUID currentUserUuid, Integer page, Integer size, String sort) {
        UserEntity currentUser = resolveUser(currentUserUuid);

        PageRequestFactory.ResolvedPageRequest pageRequest = pageRequestFactory.create(
                page,
                size,
                sort == null || sort.isBlank() ? DEFAULT_SORT : sort,
                DEFAULT_SORT_FIELD,
                ALLOWED_SORT_FIELDS);

        Page<FavoriteItemEntity> favoritePage = favoriteItemRepository.findVisibleByUserId(
                currentUser.getId(), ItemStatus.REMOVED, pageRequest.pageable());

        List<ItemEntity> items = loadItemsInFavoriteOrder(favoritePage.getContent());
        List<ItemSummaryResponse> content = mapItemSummaries(items);

        return pageResponseMapper.toItemPagedResponse(favoritePage, content, pageRequest.sort());
    }

    private List<ItemEntity> loadItemsInFavoriteOrder(List<FavoriteItemEntity> favorites) {
        if (favorites.isEmpty()) {
            return List.of();
        }

        Map<Long, ItemEntity> itemsById = itemRepository.findAllById(favorites.stream()
                        .map(FavoriteItemEntity::getItemId)
                        .toList())
                .stream()
                .collect(Collectors.toMap(ItemEntity::getId, Function.identity()));

        return favorites.stream()
                .map(favorite -> itemsById.get(favorite.getItemId()))
                .toList();
    }

    private List<ItemSummaryResponse> mapItemSummaries(List<ItemEntity> items) {
        if (items.isEmpty()) {
            return List.of();
        }

        Set<Long> categoryIds = items.stream()
                .map(ItemEntity::getCategoryId)
                .collect(Collectors.toSet());
        Map<Long, CategoryEntity> categoriesById = categoryRepository.findAllById(categoryIds).stream()
                .collect(Collectors.toMap(CategoryEntity::getId, Function.identity()));

        Set<Long> ownerIds = items.stream()
                .map(ItemEntity::getOwnerId)
                .collect(Collectors.toSet());
        Map<Long, UserEntity> ownersById = userRepository.findAllById(ownerIds).stream()
                .collect(Collectors.toMap(UserEntity::getId, Function.identity()));

        Map<Long, String> primaryImageUrlByItemId = new HashMap<>();
        for (ItemEntity item : items) {
            itemImageRepository.findFirstByItemIdAndPrimaryTrue(item.getId())
                    .ifPresent(image -> primaryImageUrlByItemId.put(item.getId(), mapPrimaryImageUrl(image)));
        }

        return items.stream()
                .map(item -> itemMapper.toSummaryResponse(
                        item,
                        categoriesById.get(item.getCategoryId()),
                        ownersById.get(item.getOwnerId()) != null ? ownersById.get(item.getOwnerId()).getUuid() : null,
                        ownersById.get(item.getOwnerId()) != null ? ownersById.get(item.getOwnerId()).getUsername() : null,
                        primaryImageUrlByItemId.get(item.getId())))
                .toList();
    }

    private String mapPrimaryImageUrl(ItemImageEntity imageEntity) {
        return itemImageMapper.toResponse(imageEntity).getUrl();
    }

    private UserEntity resolveUser(UUID userUuid) {
        return userRepository.findByUuid(userUuid)
                .orElseThrow(() -> notFound("User with uuid '%s' was not found.", userUuid));
    }

    private ItemEntity resolveItem(UUID itemUuid) {
        ItemEntity item = itemRepository.findByUuid(itemUuid)
                .orElseThrow(() -> notFound("Item with uuid '%s' was not found.", itemUuid));

        if (item.getDeletedAt() != null || item.getStatus() == ItemStatus.REMOVED) {
            throw notFound("Item with uuid '%s' was not found.", itemUuid);
        }

        return item;
    }

    private MessageResponse successResponse() {
        return new MessageResponse().message(FAVORITE_SUCCESS_MESSAGE);
    }

    private ApiException notFound(String messageTemplate, Object... args) {
        return new ApiException(
                HttpStatus.NOT_FOUND,
                ErrorCode.NOT_FOUND,
                messageTemplate.formatted(args));
    }
}

