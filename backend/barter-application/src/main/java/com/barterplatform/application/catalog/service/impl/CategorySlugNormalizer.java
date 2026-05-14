package com.barterplatform.application.catalog.service.impl;

import com.barterplatform.common.exception.ApiException;
import com.barterplatform.common.exception.ErrorCode;
import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;

final class CategorySlugNormalizer {

    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");
    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9]+");
    private static final Pattern EDGE_DASHES = Pattern.compile("(^-+|-+$)");
    private static final int MAX_SLUG_LENGTH = 140;

    private CategorySlugNormalizer() {
    }

    static String normalize(String value) {
        if (value == null || value.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.BAD_REQUEST, "Category slug is required.");
        }

        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD);
        normalized = DIACRITICS.matcher(normalized).replaceAll("");
        normalized = normalized.toLowerCase(Locale.ROOT).trim();
        normalized = NON_ALPHANUMERIC.matcher(normalized).replaceAll("-");
        normalized = EDGE_DASHES.matcher(normalized).replaceAll("");

        if (normalized.length() > MAX_SLUG_LENGTH) {
            normalized = normalized.substring(0, MAX_SLUG_LENGTH);
            normalized = EDGE_DASHES.matcher(normalized).replaceAll("");
        }

        if (normalized.isBlank()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    ErrorCode.BAD_REQUEST,
                    "Category slug must contain letters or numbers.");
        }

        return normalized;
    }
}

