package com.barterplatform.application.catalog.service.impl;

import com.barterplatform.api.model.ListingTemplateMetadata;
import com.barterplatform.common.exception.ApiException;
import com.barterplatform.common.exception.ErrorCode;
import com.barterplatform.domain.catalog.enums.ListingMode;
import com.barterplatform.domain.catalog.enums.ListingTemplateType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;

final class ListingTemplateMetadataSupport {

    private static final int MAX_COLLECTION_ENTRIES = 100;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    ListingTemplateType resolveTemplateType(
            com.barterplatform.api.model.ListingTemplateType requestedType,
            ListingMode listingMode,
            ListingTemplateType existingType) {
        if (requestedType != null) {
            ListingTemplateType mapped = ListingTemplateType.valueOf(requestedType.name());
            validateTemplateCompatibility(mapped, listingMode);
            return mapped;
        }
        if (existingType != null && isCompatible(existingType, listingMode)) {
            return existingType;
        }
        return inferTemplateType(listingMode);
    }

    String validateAndSerializeMetadata(
            ListingTemplateMetadata metadata,
            ListingTemplateType templateType,
            ListingMode listingMode) {
        validateTemplateCompatibility(templateType, listingMode);
        ListingTemplateMetadata normalized = normalizeMetadata(metadata, templateType);
        if (normalized == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(normalized);
        } catch (JsonProcessingException ex) {
            throw badRequest("Template metadata could not be serialized.");
        }
    }

    ListingTemplateMetadata deserialize(String rawJson) {
        if (rawJson == null || rawJson.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(rawJson, ListingTemplateMetadata.class);
        } catch (JsonProcessingException ex) {
            throw badRequest("Template metadata could not be read.");
        }
    }

    com.barterplatform.api.model.ListingTemplateType toApiType(ListingTemplateType templateType, ListingMode listingMode) {
        ListingTemplateType resolved = templateType != null ? templateType : inferTemplateType(listingMode);
        return com.barterplatform.api.model.ListingTemplateType.valueOf(resolved.name());
    }

    private ListingTemplateMetadata normalizeMetadata(ListingTemplateMetadata metadata, ListingTemplateType templateType) {
        if (metadata == null) {
            return null;
        }

        ListingTemplateMetadata normalized = new ListingTemplateMetadata();
        normalized.setBundleTitle(normalizeText(metadata.getBundleTitle()));
        normalized.setGroupingDescription(normalizeText(metadata.getGroupingDescription()));
        normalized.setSelectionHint(normalizeText(metadata.getSelectionHint()));
        normalized.setCollectionName(normalizeText(metadata.getCollectionName()));
        normalized.setTotalOwned(metadata.getTotalOwned());
        normalized.setDuplicateCount(metadata.getDuplicateCount());
        normalized.setMissingEntries(normalizeList(metadata.getMissingEntries(), "missingEntries"));
        normalized.setWantedEntries(normalizeList(metadata.getWantedEntries(), "wantedEntries"));
        normalized.setExchangeRules(normalizeText(metadata.getExchangeRules()));
        normalized.setWishlistSummary(normalizeText(metadata.getWishlistSummary()));
        normalized.setWantedConditionNotes(normalizeText(metadata.getWantedConditionNotes()));

        validateSupportedFields(templateType, normalized);
        validateNumericRules(templateType, normalized);

        return hasAnyValue(normalized) ? normalized : null;
    }

    private void validateSupportedFields(ListingTemplateType templateType, ListingTemplateMetadata metadata) {
        List<String> unsupported = new ArrayList<>();
        switch (templateType) {
            case STANDARD_ITEM -> {
                collectIfPresent(unsupported, metadata.getBundleTitle(), "bundleTitle");
                collectIfPresent(unsupported, metadata.getGroupingDescription(), "groupingDescription");
                collectIfPresent(unsupported, metadata.getSelectionHint(), "selectionHint");
                collectIfPresent(unsupported, metadata.getCollectionName(), "collectionName");
                collectIfPresent(unsupported, metadata.getTotalOwned(), "totalOwned");
                collectIfPresent(unsupported, metadata.getDuplicateCount(), "duplicateCount");
                collectIfPresent(unsupported, metadata.getMissingEntries(), "missingEntries");
                collectIfPresent(unsupported, metadata.getWantedEntries(), "wantedEntries");
                collectIfPresent(unsupported, metadata.getExchangeRules(), "exchangeRules");
                collectIfPresent(unsupported, metadata.getWishlistSummary(), "wishlistSummary");
                collectIfPresent(unsupported, metadata.getWantedConditionNotes(), "wantedConditionNotes");
            }
            case BUNDLE -> {
                collectIfPresent(unsupported, metadata.getSelectionHint(), "selectionHint");
                collectIfPresent(unsupported, metadata.getCollectionName(), "collectionName");
                collectIfPresent(unsupported, metadata.getTotalOwned(), "totalOwned");
                collectIfPresent(unsupported, metadata.getDuplicateCount(), "duplicateCount");
                collectIfPresent(unsupported, metadata.getMissingEntries(), "missingEntries");
                collectIfPresent(unsupported, metadata.getWantedEntries(), "wantedEntries");
                collectIfPresent(unsupported, metadata.getExchangeRules(), "exchangeRules");
                collectIfPresent(unsupported, metadata.getWishlistSummary(), "wishlistSummary");
                collectIfPresent(unsupported, metadata.getWantedConditionNotes(), "wantedConditionNotes");
            }
            case PICK_FROM_COLLECTION -> {
                collectIfPresent(unsupported, metadata.getBundleTitle(), "bundleTitle");
                collectIfPresent(unsupported, metadata.getGroupingDescription(), "groupingDescription");
                collectIfPresent(unsupported, metadata.getTotalOwned(), "totalOwned");
                collectIfPresent(unsupported, metadata.getDuplicateCount(), "duplicateCount");
                collectIfPresent(unsupported, metadata.getMissingEntries(), "missingEntries");
                collectIfPresent(unsupported, metadata.getWantedEntries(), "wantedEntries");
                collectIfPresent(unsupported, metadata.getWishlistSummary(), "wishlistSummary");
                collectIfPresent(unsupported, metadata.getWantedConditionNotes(), "wantedConditionNotes");
            }
            case COLLECTION_ALBUM -> {
                collectIfPresent(unsupported, metadata.getBundleTitle(), "bundleTitle");
                collectIfPresent(unsupported, metadata.getGroupingDescription(), "groupingDescription");
                collectIfPresent(unsupported, metadata.getWishlistSummary(), "wishlistSummary");
                collectIfPresent(unsupported, metadata.getWantedConditionNotes(), "wantedConditionNotes");
            }
            case WISHLIST -> {
                collectIfPresent(unsupported, metadata.getBundleTitle(), "bundleTitle");
                collectIfPresent(unsupported, metadata.getGroupingDescription(), "groupingDescription");
                collectIfPresent(unsupported, metadata.getSelectionHint(), "selectionHint");
                collectIfPresent(unsupported, metadata.getCollectionName(), "collectionName");
                collectIfPresent(unsupported, metadata.getTotalOwned(), "totalOwned");
                collectIfPresent(unsupported, metadata.getDuplicateCount(), "duplicateCount");
                collectIfPresent(unsupported, metadata.getMissingEntries(), "missingEntries");
                collectIfPresent(unsupported, metadata.getExchangeRules(), "exchangeRules");
            }
        }

        if (!unsupported.isEmpty()) {
            throw badRequest("Unsupported template metadata fields for %s: %s."
                    .formatted(templateType, String.join(", ", unsupported)));
        }
    }

    private void validateNumericRules(ListingTemplateType templateType, ListingTemplateMetadata metadata) {
        if (metadata.getDuplicateCount() != null && metadata.getTotalOwned() != null
                && metadata.getDuplicateCount() > metadata.getTotalOwned()) {
            throw badRequest("duplicateCount cannot be greater than totalOwned.");
        }
        if (templateType == ListingTemplateType.COLLECTION_ALBUM
                && metadata.getDuplicateCount() != null
                && metadata.getDuplicateCount() == 0
                && (metadata.getMissingEntries() == null || metadata.getMissingEntries().isEmpty())
                && (metadata.getWantedEntries() == null || metadata.getWantedEntries().isEmpty())) {
            throw badRequest("Collection album metadata should include duplicates or wanted entries.");
        }
        if (templateType == ListingTemplateType.WISHLIST
                && isBlank(metadata.getWishlistSummary())
                && (metadata.getWantedEntries() == null || metadata.getWantedEntries().isEmpty())) {
            throw badRequest("Wishlist listings require a wishlist summary or wanted entries.");
        }
    }

    private void validateTemplateCompatibility(ListingTemplateType templateType, ListingMode listingMode) {
        if (!isCompatible(templateType, listingMode)) {
            throw badRequest("Listing template %s is not supported for listing mode %s."
                    .formatted(templateType, listingMode));
        }
    }

    private boolean isCompatible(ListingTemplateType templateType, ListingMode listingMode) {
        return switch (templateType) {
            case STANDARD_ITEM, WISHLIST -> listingMode == ListingMode.SINGLE;
            case BUNDLE -> listingMode == ListingMode.BUNDLE;
            case PICK_FROM_COLLECTION, COLLECTION_ALBUM -> listingMode == ListingMode.PICK_ANY;
        };
    }

    private ListingTemplateType inferTemplateType(ListingMode listingMode) {
        return switch (listingMode) {
            case SINGLE -> ListingTemplateType.STANDARD_ITEM;
            case BUNDLE -> ListingTemplateType.BUNDLE;
            case PICK_ANY -> ListingTemplateType.PICK_FROM_COLLECTION;
        };
    }

    private List<String> normalizeList(List<String> values, String fieldName) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        List<String> normalized = values.stream()
                .map(this::normalizeText)
                .filter(value -> value != null)
                .distinct()
                .toList();
        if (normalized.isEmpty()) {
            return null;
        }
        if (normalized.size() > MAX_COLLECTION_ENTRIES) {
            throw badRequest("%s can contain at most %d values.".formatted(fieldName, MAX_COLLECTION_ENTRIES));
        }
        return normalized;
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().replaceAll("\\s+", " ");
        return normalized.isBlank() ? null : normalized;
    }

    private boolean hasAnyValue(ListingTemplateMetadata metadata) {
        return metadata.getBundleTitle() != null
                || metadata.getGroupingDescription() != null
                || metadata.getSelectionHint() != null
                || metadata.getCollectionName() != null
                || metadata.getTotalOwned() != null
                || metadata.getDuplicateCount() != null
                || metadata.getMissingEntries() != null
                || metadata.getWantedEntries() != null
                || metadata.getExchangeRules() != null
                || metadata.getWishlistSummary() != null
                || metadata.getWantedConditionNotes() != null;
    }

    private void collectIfPresent(List<String> unsupported, Object value, String fieldName) {
        if (value instanceof String stringValue && !isBlank(stringValue)) {
            unsupported.add(fieldName);
            return;
        }
        if (value instanceof List<?> listValue && !listValue.isEmpty()) {
            unsupported.add(fieldName);
            return;
        }
        if (value != null && !(value instanceof String) && !(value instanceof List<?>)) {
            unsupported.add(fieldName);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private ApiException badRequest(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.BAD_REQUEST, message);
    }
}

