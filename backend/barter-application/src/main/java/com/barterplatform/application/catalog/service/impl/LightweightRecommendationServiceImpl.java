package com.barterplatform.application.catalog.service.impl;

import com.barterplatform.api.model.ItemSummaryResponse;
import com.barterplatform.api.model.RecommendationItemResponse;
import com.barterplatform.api.model.RecommendationPagedResponse;
import com.barterplatform.api.model.RecommendationReason;
import com.barterplatform.api.model.SavedSearchCriteria;
import com.barterplatform.application.catalog.mapper.ItemImageMapper;
import com.barterplatform.application.catalog.mapper.ItemMapper;
import com.barterplatform.application.catalog.service.RecommendationService;
import com.barterplatform.domain.catalog.entity.CategoryEntity;
import com.barterplatform.domain.catalog.entity.ItemEntity;
import com.barterplatform.domain.catalog.entity.ItemTagEntity;
import com.barterplatform.domain.catalog.entity.TagEntity;
import com.barterplatform.domain.catalog.enums.ItemStatus;
import com.barterplatform.domain.identity.entity.UserEntity;
import com.barterplatform.domain.trade.entity.TradeOfferEntity;
import com.barterplatform.domain.trade.entity.TradeOfferItemEntity;
import com.barterplatform.infrastructure.catalog.repository.CategoryRepository;
import com.barterplatform.infrastructure.catalog.repository.ItemImageRepository;
import com.barterplatform.infrastructure.catalog.repository.ItemRepository;
import com.barterplatform.infrastructure.catalog.repository.ItemTagRepository;
import com.barterplatform.infrastructure.catalog.repository.SavedSearchRepository;
import com.barterplatform.infrastructure.catalog.repository.TagRepository;
import com.barterplatform.infrastructure.identity.repository.UserRepository;
import com.barterplatform.infrastructure.trade.repository.TradeOfferRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class LightweightRecommendationServiceImpl implements RecommendationService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 12;
    private static final int MIN_SIZE = 1;
    private static final int MAX_SIZE = 50;
    private static final int MAX_CANDIDATES = 300;
    private static final int MAX_SIGNAL_ITEMS = 50;
    private static final int MAX_SAVED_SEARCHES = 25;
    private static final String RECOMMENDATION_SORT = "recommendationScore,desc";

    private static final int SAVED_CATEGORY_WEIGHT = 6;
    private static final int SAVED_TAG_WEIGHT = 5;
    private static final int SAVED_LOCATION_WEIGHT = 3;
    private static final int SAVED_QUERY_WEIGHT = 2;
    private static final int LISTING_CATEGORY_WEIGHT = 5;
    private static final int LISTING_TAG_WEIGHT = 4;
    private static final int LISTING_LOCATION_WEIGHT = 2;
    private static final int TRADE_CATEGORY_WEIGHT = 7;
    private static final int TRADE_TAG_WEIGHT = 5;
    private static final int TRADE_LOCATION_WEIGHT = 3;

    private final ItemRepository itemRepository;
    private final ItemTagRepository itemTagRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final UserRepository userRepository;
    private final SavedSearchRepository savedSearchRepository;
    private final TradeOfferRepository tradeOfferRepository;
    private final ItemImageRepository itemImageRepository;
    private final ItemMapper itemMapper;
    private final ItemImageMapper itemImageMapper;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    public LightweightRecommendationServiceImpl(ItemRepository itemRepository,
                                                ItemTagRepository itemTagRepository,
                                                CategoryRepository categoryRepository,
                                                TagRepository tagRepository,
                                                UserRepository userRepository,
                                                SavedSearchRepository savedSearchRepository,
                                                TradeOfferRepository tradeOfferRepository,
                                                ItemImageRepository itemImageRepository,
                                                ItemMapper itemMapper,
                                                ItemImageMapper itemImageMapper) {
        this.itemRepository = itemRepository;
        this.itemTagRepository = itemTagRepository;
        this.categoryRepository = categoryRepository;
        this.tagRepository = tagRepository;
        this.userRepository = userRepository;
        this.savedSearchRepository = savedSearchRepository;
        this.tradeOfferRepository = tradeOfferRepository;
        this.itemImageRepository = itemImageRepository;
        this.itemMapper = itemMapper;
        this.itemImageMapper = itemImageMapper;
    }

    @Override
    public RecommendationPagedResponse listRecommendations(UUID requesterUuid, Integer page, Integer size, String sort) {
        int resolvedPage = Math.max(DEFAULT_PAGE, page == null ? DEFAULT_PAGE : page);
        int resolvedSize = resolveSize(size);

        Optional<UserEntity> requester = requesterUuid == null ? Optional.empty() : userRepository.findByUuid(requesterUuid);
        Long requesterId = requester.map(UserEntity::getId).orElse(null);
        RecommendationSignals signals = requester.map(this::collectSignals).orElseGet(RecommendationSignals::empty);

        List<ItemEntity> candidates = loadCandidateItems(resolvedPage, resolvedSize).stream()
                .filter(item -> item.getStatus() == ItemStatus.ACTIVE && item.getDeletedAt() == null)
                .filter(item -> requesterId == null || !Objects.equals(item.getOwnerId(), requesterId))
                .toList();

        Map<Long, Set<Long>> tagIdsByItemId = loadTagIdsByItemId(candidates.stream().map(ItemEntity::getId).toList());
        List<ScoredRecommendation> scoredRecommendations = candidates.stream()
                .map(item -> score(item, tagIdsByItemId.getOrDefault(item.getId(), Set.of()), signals))
                .sorted(recommendationComparator())
                .toList();

        int fromIndex = Math.min(resolvedPage * resolvedSize, scoredRecommendations.size());
        int toIndex = Math.min(fromIndex + resolvedSize, scoredRecommendations.size());
        List<ScoredRecommendation> pageItems = scoredRecommendations.subList(fromIndex, toIndex);
        List<RecommendationItemResponse> content = mapRecommendations(pageItems);
        int totalPages = scoredRecommendations.isEmpty()
                ? 0
                : (int) Math.ceil(scoredRecommendations.size() / (double) resolvedSize);

        return new RecommendationPagedResponse()
                .content(content)
                .page(resolvedPage)
                .size(resolvedSize)
                .totalElements((long) scoredRecommendations.size())
                .totalPages(totalPages)
                .first(resolvedPage == 0)
                .last(resolvedPage >= Math.max(totalPages - 1, 0))
                .sort(RECOMMENDATION_SORT);
    }

    private int resolveSize(Integer size) {
        if (size == null) {
            return DEFAULT_SIZE;
        }
        return Math.clamp(size, MIN_SIZE, MAX_SIZE);
    }

    private List<ItemEntity> loadCandidateItems(int page, int size) {
        int candidateLimit = Math.min(MAX_CANDIDATES, Math.max(100, (page + 1) * size * 4));
        Page<ItemEntity> candidatePage = itemRepository.findByStatusAndDeletedAtIsNull(
                ItemStatus.ACTIVE,
                PageRequest.of(0, candidateLimit, Sort.by(Sort.Direction.DESC, "createdAt")));
        return candidatePage.getContent();
    }

    private RecommendationSignals collectSignals(UserEntity user) {
        RecommendationSignals signals = new RecommendationSignals();
        collectListingSignals(user, signals);
        collectSavedSearchSignals(user, signals);
        collectTradeSignals(user, signals);
        return signals;
    }

    private void collectListingSignals(UserEntity user, RecommendationSignals signals) {
        Page<ItemEntity> activeListings = itemRepository.findByOwnerIdAndStatusAndDeletedAtIsNull(
                user.getId(),
                ItemStatus.ACTIVE,
                PageRequest.of(0, MAX_SIGNAL_ITEMS, Sort.by(Sort.Direction.DESC, "updatedAt")));
        Map<Long, Set<Long>> tagsByItemId = loadTagIdsByItemId(activeListings.getContent().stream().map(ItemEntity::getId).toList());

        for (ItemEntity item : activeListings.getContent()) {
            signals.addListingCategory(item.getCategoryId());
            tagsByItemId.getOrDefault(item.getId(), Set.of()).forEach(signals::addListingTag);
            extractLocationTokens(item).forEach(signals::addListingLocationToken);
        }
    }

    private void collectSavedSearchSignals(UserEntity user, RecommendationSignals signals) {
        Page<com.barterplatform.domain.catalog.entity.SavedSearchEntity> savedSearches = savedSearchRepository.findByUserId(
                user.getId(),
                PageRequest.of(0, MAX_SAVED_SEARCHES, Sort.by(Sort.Direction.DESC, "updatedAt")));
        Map<UUID, Optional<CategoryEntity>> categoryCache = new HashMap<>();
        Map<UUID, Optional<TagEntity>> tagCache = new HashMap<>();

        for (com.barterplatform.domain.catalog.entity.SavedSearchEntity savedSearch : savedSearches.getContent()) {
            deserializeCriteria(savedSearch.getCriteriaPayload()).ifPresent(criteria -> {
                if (criteria.getCategoryUuid() != null) {
                    categoryCache.computeIfAbsent(criteria.getCategoryUuid(), categoryRepository::findByUuid)
                            .map(CategoryEntity::getId)
                            .ifPresent(signals::addInterestCategory);
                }
                if (criteria.getTagUuids() != null) {
                    criteria.getTagUuids().forEach(tagUuid -> tagCache.computeIfAbsent(tagUuid, tagRepository::findByUuid)
                            .map(TagEntity::getId)
                            .ifPresent(signals::addInterestTag));
                }
                normalizeTokens(criteria.getLocation()).forEach(signals::addInterestLocationToken);
                normalizeTokens(criteria.getQ()).forEach(signals::addInterestQueryToken);
            });
        }
    }

    private void collectTradeSignals(UserEntity user, RecommendationSignals signals) {
        Page<TradeOfferEntity> tradeOffers = tradeOfferRepository.findBySenderUserIdOrReceiverUserId(
                user.getId(),
                user.getId(),
                PageRequest.of(0, MAX_SIGNAL_ITEMS, Sort.by(Sort.Direction.DESC, "updatedAt")));
        Set<Long> itemIds = new LinkedHashSet<>();
        for (TradeOfferEntity offer : tradeOffers.getContent()) {
            if (offer.getSenderItemId() != null) {
                itemIds.add(offer.getSenderItemId());
            }
            if (offer.getReceiverItemId() != null) {
                itemIds.add(offer.getReceiverItemId());
            }
            offer.getItems().stream().map(TradeOfferItemEntity::getItemId).forEach(itemIds::add);
        }
        if (itemIds.isEmpty()) {
            return;
        }

        List<ItemEntity> tradeItems = itemRepository.findAllById(itemIds);
        Map<Long, Set<Long>> tagsByItemId = loadTagIdsByItemId(tradeItems.stream().map(ItemEntity::getId).toList());
        for (ItemEntity item : tradeItems) {
            signals.addTradeCategory(item.getCategoryId());
            tagsByItemId.getOrDefault(item.getId(), Set.of()).forEach(signals::addTradeTag);
            extractLocationTokens(item).forEach(signals::addTradeLocationToken);
        }
    }

    private Optional<SavedSearchCriteria> deserializeCriteria(String payload) {
        if (payload == null || payload.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(payload, SavedSearchCriteria.class));
        } catch (JsonProcessingException ex) {
            return Optional.empty();
        }
    }

    private ScoredRecommendation score(ItemEntity item, Set<Long> itemTagIds, RecommendationSignals signals) {
        int interestScore = signals.savedCategoryWeight(item.getCategoryId()) * SAVED_CATEGORY_WEIGHT;
        interestScore += itemTagIds.stream().mapToInt(tagId -> signals.savedTagWeight(tagId) * SAVED_TAG_WEIGHT).sum();
        interestScore += titleTokenMatches(item, signals.interestQueryTokens()) * SAVED_QUERY_WEIGHT;

        int listingScore = signals.listingCategoryWeight(item.getCategoryId()) * LISTING_CATEGORY_WEIGHT;
        listingScore += itemTagIds.stream().mapToInt(tagId -> signals.listingTagWeight(tagId) * LISTING_TAG_WEIGHT).sum();

        int tradeScore = signals.tradeCategoryWeight(item.getCategoryId()) * TRADE_CATEGORY_WEIGHT;
        tradeScore += itemTagIds.stream().mapToInt(tagId -> signals.tradeTagWeight(tagId) * TRADE_TAG_WEIGHT).sum();

        int locationScore = matchingLocationTokens(item, signals.interestLocationTokens()) * SAVED_LOCATION_WEIGHT;
        locationScore += matchingLocationTokens(item, signals.listingLocationTokens()) * LISTING_LOCATION_WEIGHT;
        locationScore += matchingLocationTokens(item, signals.tradeLocationTokens()) * TRADE_LOCATION_WEIGHT;

        int score = interestScore + listingScore + tradeScore + locationScore;
        RecommendationReason reason = resolveReason(interestScore + tradeScore, listingScore, locationScore, score);
        return new ScoredRecommendation(item, score, reason);
    }

    private RecommendationReason resolveReason(int interestScore, int listingScore, int locationScore, int totalScore) {
        if (totalScore <= 0) {
            return RecommendationReason.POPULAR_RECENTLY;
        }
        if (locationScore > 0 && locationScore >= interestScore && locationScore >= listingScore) {
            return RecommendationReason.NEAR_PREFERRED_EXCHANGE_AREA;
        }
        if (listingScore > 0 && listingScore >= interestScore) {
            return RecommendationReason.SIMILAR_TO_YOUR_LISTINGS;
        }
        return RecommendationReason.BECAUSE_OF_INTERESTS;
    }

    private int titleTokenMatches(ItemEntity item, Set<String> queryTokens) {
        if (queryTokens.isEmpty() || item.getTitle() == null) {
            return 0;
        }
        String title = item.getTitle().toLowerCase(Locale.ROOT);
        int matches = 0;
        for (String token : queryTokens) {
            if (title.contains(token)) {
                matches++;
            }
        }
        return matches;
    }

    private int matchingLocationTokens(ItemEntity item, Set<String> preferredTokens) {
        if (preferredTokens.isEmpty()) {
            return 0;
        }
        Set<String> itemTokens = extractLocationTokens(item);
        int matches = 0;
        for (String token : preferredTokens) {
            if (itemTokens.contains(token)) {
                matches++;
            }
        }
        return matches;
    }

    private Set<String> extractLocationTokens(ItemEntity item) {
        Set<String> tokens = new HashSet<>();
        tokens.addAll(normalizeTokens(item.getExchangeCity()));
        tokens.addAll(normalizeTokens(item.getExchangeArea()));
        tokens.addAll(normalizeTokens(item.getExchangeLocation()));
        return tokens;
    }

    private Set<String> normalizeTokens(String value) {
        if (value == null || value.isBlank()) {
            return Set.of();
        }
        return java.util.Arrays.stream(value.toLowerCase(Locale.ROOT).split("[^\\p{IsAlphabetic}\\p{IsDigit}]+"))
                .map(String::trim)
                .filter(token -> token.length() >= 3)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Map<Long, Set<Long>> loadTagIdsByItemId(Collection<Long> itemIds) {
        List<Long> ids = itemIds.stream().filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        Map<Long, Set<Long>> tagsByItemId = new HashMap<>();
        for (ItemTagEntity itemTag : itemTagRepository.findByIdItemIdIn(ids)) {
            tagsByItemId.computeIfAbsent(itemTag.getId().getItemId(), ignored -> new HashSet<>())
                    .add(itemTag.getId().getTagId());
        }
        return tagsByItemId;
    }

    private Comparator<ScoredRecommendation> recommendationComparator() {
        return Comparator
                .comparingInt(ScoredRecommendation::score).reversed()
                .thenComparing(ScoredRecommendation::createdAt, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(ScoredRecommendation::uuid);
    }

    private List<RecommendationItemResponse> mapRecommendations(List<ScoredRecommendation> recommendations) {
        if (recommendations.isEmpty()) {
            return List.of();
        }
        List<ItemEntity> items = recommendations.stream().map(ScoredRecommendation::item).toList();
        Map<Long, ItemSummaryResponse> summariesByItemId = mapItemSummaries(items).entrySet().stream()
                .collect(Collectors.toMap(entry -> entry.getKey().getId(), Map.Entry::getValue));

        List<RecommendationItemResponse> responses = new ArrayList<>();
        for (ScoredRecommendation recommendation : recommendations) {
            ItemSummaryResponse item = summariesByItemId.get(recommendation.item().getId());
            if (item != null) {
                responses.add(new RecommendationItemResponse()
                        .item(item)
                        .reason(recommendation.reason()));
            }
        }
        return responses;
    }

    private Map<ItemEntity, ItemSummaryResponse> mapItemSummaries(List<ItemEntity> items) {
        Set<Long> categoryIds = items.stream().map(ItemEntity::getCategoryId).collect(Collectors.toSet());
        Map<Long, CategoryEntity> categoriesById = categoryRepository.findAllById(categoryIds).stream()
                .collect(Collectors.toMap(CategoryEntity::getId, Function.identity()));

        Set<Long> ownerIds = items.stream().map(ItemEntity::getOwnerId).collect(Collectors.toSet());
        Map<Long, UserEntity> ownersById = userRepository.findAllById(ownerIds).stream()
                .collect(Collectors.toMap(UserEntity::getId, Function.identity()));

        Map<Long, String> primaryImageUrlByItemId = new HashMap<>();
        for (ItemEntity item : items) {
            itemImageRepository.findFirstByItemIdAndPrimaryTrue(item.getId())
                    .ifPresent(img -> primaryImageUrlByItemId.put(item.getId(), itemImageMapper.toResponse(img).getUrl()));
        }

        Map<ItemEntity, ItemSummaryResponse> result = new HashMap<>();
        for (ItemEntity item : items) {
            CategoryEntity category = categoriesById.get(item.getCategoryId());
            UserEntity owner = ownersById.get(item.getOwnerId());
            if (category == null || owner == null) {
                continue;
            }
            result.put(item, itemMapper.toSummaryResponse(
                    item,
                    category,
                    owner.getUuid(),
                    owner.getUsername(),
                    primaryImageUrlByItemId.get(item.getId())));
        }
        return result;
    }

    private record ScoredRecommendation(ItemEntity item, int score, RecommendationReason reason) {
        OffsetDateTime createdAt() {
            return item.getCreatedAt();
        }

        UUID uuid() {
            return item.getUuid();
        }
    }

    private static final class RecommendationSignals {
        private final Map<Long, Integer> interestCategories = new HashMap<>();
        private final Map<Long, Integer> interestTags = new HashMap<>();
        private final Set<String> interestLocationTokens = new LinkedHashSet<>();
        private final Set<String> interestQueryTokens = new LinkedHashSet<>();
        private final Map<Long, Integer> listingCategories = new HashMap<>();
        private final Map<Long, Integer> listingTags = new HashMap<>();
        private final Set<String> listingLocationTokens = new LinkedHashSet<>();
        private final Map<Long, Integer> tradeCategories = new HashMap<>();
        private final Map<Long, Integer> tradeTags = new HashMap<>();
        private final Set<String> tradeLocationTokens = new LinkedHashSet<>();

        static RecommendationSignals empty() {
            return new RecommendationSignals();
        }

        void addInterestCategory(Long id) { increment(interestCategories, id); }
        void addInterestTag(Long id) { increment(interestTags, id); }
        void addInterestLocationToken(String token) { interestLocationTokens.add(token); }
        void addInterestQueryToken(String token) { interestQueryTokens.add(token); }
        void addListingCategory(Long id) { increment(listingCategories, id); }
        void addListingTag(Long id) { increment(listingTags, id); }
        void addListingLocationToken(String token) { listingLocationTokens.add(token); }
        void addTradeCategory(Long id) { increment(tradeCategories, id); }
        void addTradeTag(Long id) { increment(tradeTags, id); }
        void addTradeLocationToken(String token) { tradeLocationTokens.add(token); }

        int savedCategoryWeight(Long id) { return interestCategories.getOrDefault(id, 0); }
        int savedTagWeight(Long id) { return interestTags.getOrDefault(id, 0); }
        int listingCategoryWeight(Long id) { return listingCategories.getOrDefault(id, 0); }
        int listingTagWeight(Long id) { return listingTags.getOrDefault(id, 0); }
        int tradeCategoryWeight(Long id) { return tradeCategories.getOrDefault(id, 0); }
        int tradeTagWeight(Long id) { return tradeTags.getOrDefault(id, 0); }
        Set<String> interestLocationTokens() { return interestLocationTokens; }
        Set<String> interestQueryTokens() { return interestQueryTokens; }
        Set<String> listingLocationTokens() { return listingLocationTokens; }
        Set<String> tradeLocationTokens() { return tradeLocationTokens; }

        private void increment(Map<Long, Integer> map, Long id) {
            if (id != null) {
                map.merge(id, 1, Integer::sum);
            }
        }
    }
}


