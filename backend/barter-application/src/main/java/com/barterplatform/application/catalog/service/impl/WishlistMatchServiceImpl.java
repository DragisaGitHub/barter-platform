package com.barterplatform.application.catalog.service.impl;

import com.barterplatform.api.model.ItemImageResponse;
import com.barterplatform.api.model.ItemSummaryResponse;
import com.barterplatform.api.model.WishlistMatchReason;
import com.barterplatform.api.model.WishlistMatchResponse;
import com.barterplatform.application.catalog.mapper.ItemImageMapper;
import com.barterplatform.application.catalog.mapper.ItemMapper;
import com.barterplatform.application.catalog.service.WishlistMatchService;
import com.barterplatform.domain.catalog.entity.CategoryEntity;
import com.barterplatform.domain.catalog.entity.ItemEntity;
import com.barterplatform.domain.catalog.entity.ItemListingEntryEntity;
import com.barterplatform.domain.catalog.enums.ItemStatus;
import com.barterplatform.domain.catalog.enums.ListingTemplateType;
import com.barterplatform.domain.identity.entity.UserEntity;
import com.barterplatform.infrastructure.catalog.repository.CategoryRepository;
import com.barterplatform.infrastructure.catalog.repository.ItemImageRepository;
import com.barterplatform.infrastructure.catalog.repository.ItemListingEntryRepository;
import com.barterplatform.infrastructure.catalog.repository.ItemRepository;
import com.barterplatform.infrastructure.identity.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class WishlistMatchServiceImpl implements WishlistMatchService {

    private static final int CANDIDATE_LIMIT = 50;
    private static final int RESULT_LIMIT = 20;

    private final ItemRepository itemRepository;
    private final ItemListingEntryRepository itemListingEntryRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ItemImageRepository itemImageRepository;
    private final ItemMapper itemMapper;
    private final ItemImageMapper itemImageMapper;

    public WishlistMatchServiceImpl(
            ItemRepository itemRepository,
            ItemListingEntryRepository itemListingEntryRepository,
            UserRepository userRepository,
            CategoryRepository categoryRepository,
            ItemImageRepository itemImageRepository,
            ItemMapper itemMapper,
            ItemImageMapper itemImageMapper) {
        this.itemRepository = itemRepository;
        this.itemListingEntryRepository = itemListingEntryRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.itemImageRepository = itemImageRepository;
        this.itemMapper = itemMapper;
        this.itemImageMapper = itemImageMapper;
    }

    @Override
    public List<WishlistMatchResponse> listWishlistMatches(UUID currentUserUuid, UUID wishlistItemUuid) {
        UserEntity currentUser = userRepository.findByUuid(currentUserUuid)
                .orElseThrow(() -> new EntityNotFoundException("Current user not found."));

        ItemEntity wishlistItem = itemRepository.findByUuid(wishlistItemUuid)
                .orElseThrow(() -> new EntityNotFoundException("Wishlist item not found."));

        if (!wishlistItem.getOwnerId().equals(currentUser.getId())) {
            throw new SecurityException("Only the wishlist owner can view matches.");
        }

        if (wishlistItem.getStatus() != ItemStatus.ACTIVE) {
            throw new IllegalStateException("Wishlist item must be active.");
        }

        if (wishlistItem.getListingTemplateType() != ListingTemplateType.WISHLIST) {
            throw new IllegalStateException("Item is not a wishlist listing.");
        }

        List<ItemEntity> candidates = itemRepository.findWishlistMatchCandidates(
                wishlistItem.getId(),
                wishlistItem.getOwnerId(),
                wishlistItem.getCategoryId(),
                EnumSet.of(
                        ListingTemplateType.STANDARD_ITEM,
                        ListingTemplateType.PICK_FROM_COLLECTION,
                        ListingTemplateType.COLLECTION_ALBUM),
                ItemStatus.ACTIVE,
                PageRequest.of(0, CANDIDATE_LIMIT));

        List<Long> candidateIds = candidates.stream()
                .map(ItemEntity::getId)
                .toList();

        List<ItemListingEntryEntity> candidateEntries = candidateIds.isEmpty()
                ? List.of()
                : itemListingEntryRepository.findByItemIdInOrderByItemIdAscSortOrderAsc(candidateIds);

        Set<String> wishlistTerms = extractTerms(
                wishlistItem.getTitle(),
                wishlistItem.getDescription(),
                wishlistItem.getTemplateMetadataJson());

        return candidates.stream()
                .map(candidate -> toMatchResponse(candidate, candidateEntries, wishlistTerms, wishlistItem))
                .filter(match -> match.getScore() > 0)
                .sorted(Comparator.comparing(WishlistMatchResponse::getScore).reversed())
                .limit(RESULT_LIMIT)
                .toList();
    }

    private WishlistMatchResponse toMatchResponse(
            ItemEntity candidate,
            List<ItemListingEntryEntity> allCandidateEntries,
            Set<String> wishlistTerms,
            ItemEntity wishlistItem) {
        int score = 0;
        List<WishlistMatchReason> reasons = new ArrayList<>();

        if (candidate.getCategoryId().equals(wishlistItem.getCategoryId())) {
            score += 40;
            reasons.add(WishlistMatchReason.SAME_CATEGORY);
        }

        if (candidate.getListingTemplateType() != null) {
            score += 10;
            reasons.add(WishlistMatchReason.COMPATIBLE_TEMPLATE);
        }

        Set<String> candidateTerms = extractTerms(
                candidate.getTitle(),
                candidate.getDescription(),
                candidate.getTemplateMetadataJson());

        if (hasOverlap(wishlistTerms, candidateTerms)) {
            score += 25;
            reasons.add(WishlistMatchReason.TITLE_MATCH);
        }

        List<ItemListingEntryEntity> entries = allCandidateEntries.stream()
                .filter(entry -> entry.getItemId().equals(candidate.getId()))
                .toList();

        Set<String> entryTerms = new HashSet<>();
        for (ItemListingEntryEntity entry : entries) {
            entryTerms.addAll(extractTerms(entry.getTitle(), entry.getDescription()));
        }

        if (hasOverlap(wishlistTerms, entryTerms)) {
            score += 30;
            reasons.add(WishlistMatchReason.ENTRY_MATCH);
        }

        if (sameText(candidate.getExchangeCity(), wishlistItem.getExchangeCity())) {
            score += 15;
            reasons.add(WishlistMatchReason.SAME_CITY);
        }

        if (sameText(candidate.getExchangeArea(), wishlistItem.getExchangeArea())) {
            score += 10;
            reasons.add(WishlistMatchReason.SAME_AREA);
        }

        CategoryEntity category = categoryRepository.findById(candidate.getCategoryId())
                .orElseThrow(() -> new EntityNotFoundException("Category not found."));

        UserEntity owner = userRepository.findById(candidate.getOwnerId())
                .orElseThrow(() -> new EntityNotFoundException("Owner not found."));

        String primaryImageUrl = itemImageRepository.findFirstByItemIdAndPrimaryTrue(candidate.getId())
                .map(itemImageMapper::toResponse)
                .map(ItemImageResponse::getUrl)
                .orElse(null);

        List<ItemListingEntryEntity> previewEntries = entries.stream()
                .limit(3)
                .toList();

        ItemSummaryResponse itemSummary = itemMapper.toSummaryResponse(
                candidate,
                category,
                owner.getUuid(),
                owner.getUsername(),
                primaryImageUrl,
                entries.size(),
                previewEntries);

        return new WishlistMatchResponse()
                .item(itemSummary)
                .score(score)
                .reasons(reasons);
    }

    private Set<String> extractTerms(String... values) {
        Set<String> terms = new HashSet<>();

        for (String value : values) {
            if (value == null || value.isBlank()) {
                continue;
            }

            String normalized = value
                    .toLowerCase(Locale.ROOT)
                    .replaceAll("[^\\p{L}\\p{N}]+", " ");

            for (String token : normalized.split("\\s+")) {
                if (token.length() >= 2) {
                    terms.add(token);
                }
            }
        }

        return terms;
    }

    private boolean hasOverlap(Set<String> left, Set<String> right) {
        return left.stream().anyMatch(right::contains);
    }

    private boolean sameText(String left, String right) {
        return left != null && right != null && left.trim().equalsIgnoreCase(right.trim());
    }
}