package com.barterplatform.application.common.pagination;

import com.barterplatform.common.exception.ApiException;
import com.barterplatform.common.exception.ErrorCode;
import java.util.Locale;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class PageRequestFactory {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MIN_SIZE = 1;
    private static final int MAX_SIZE = 100;
    private static final Sort.Direction DEFAULT_DIRECTION = Sort.Direction.ASC;

    public ResolvedPageRequest create(
            Integer page,
            Integer size,
            String sort,
            String defaultSortField,
            Set<String> allowedSortFields) {
        int resolvedPage = Math.max(DEFAULT_PAGE, page == null ? DEFAULT_PAGE : page);
        int resolvedSize = resolveSize(size);
        ResolvedSort resolvedSort = resolveSort(sort, defaultSortField, allowedSortFields);

        return new ResolvedPageRequest(
                PageRequest.of(resolvedPage, resolvedSize, resolvedSort.sort()),
                resolvedSort.value());
    }

    private int resolveSize(Integer size) {
        if (size == null) {
            return DEFAULT_SIZE;
        }

        return Math.clamp(size, MIN_SIZE, MAX_SIZE);
    }

    private ResolvedSort resolveSort(String sort, String defaultSortField, Set<String> allowedSortFields) {
        if (sort == null || sort.isBlank()) {
            return defaultSort(defaultSortField);
        }

        String[] tokens = sort.split(",", 2);
        String field = tokens[0].trim();
        if (field.isEmpty()) {
            return defaultSort(defaultSortField);
        }

        validateSortField(field, allowedSortFields);

        Sort.Direction direction = resolveDirection(tokens.length > 1 ? tokens[1].trim() : null);
        return new ResolvedSort(
                Sort.by(direction, field),
                "%s,%s".formatted(field, direction.name().toLowerCase(Locale.ROOT)));
    }

    private void validateSortField(String field, Set<String> allowedSortFields) {
        if (allowedSortFields == null || allowedSortFields.isEmpty() || allowedSortFields.contains(field)) {
            return;
        }

        throw new ApiException(
                HttpStatus.BAD_REQUEST,
                ErrorCode.BAD_REQUEST,
                "Unsupported sort field '%s'.".formatted(field));
    }

    private Sort.Direction resolveDirection(String directionToken) {
        if (directionToken == null || directionToken.isBlank()) {
            return DEFAULT_DIRECTION;
        }

        try {
            return Sort.Direction.valueOf(directionToken.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return DEFAULT_DIRECTION;
        }
    }

    private ResolvedSort defaultSort(String defaultSortField) {
        return new ResolvedSort(
                Sort.by(DEFAULT_DIRECTION, defaultSortField),
                "%s,%s".formatted(defaultSortField, DEFAULT_DIRECTION.name().toLowerCase(Locale.ROOT)));
    }

    public record ResolvedPageRequest(Pageable pageable, String sort) {
    }

    private record ResolvedSort(Sort sort, String value) {
    }
}

