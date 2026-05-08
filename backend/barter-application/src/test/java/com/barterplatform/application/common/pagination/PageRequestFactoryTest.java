package com.barterplatform.application.common.pagination;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.barterplatform.common.exception.ApiException;
import com.barterplatform.common.exception.ErrorCode;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;

class PageRequestFactoryTest {

    private static final String DEFAULT_SORT_FIELD = "username";
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("username", "createdAt", "email");

    private final PageRequestFactory pageRequestFactory = new PageRequestFactory();

    @Test
    void shouldUseDefaultsWhenPageAndSizeAreNull() {
        PageRequestFactory.ResolvedPageRequest resolvedPageRequest = pageRequestFactory.create(
                null,
                null,
                null,
                DEFAULT_SORT_FIELD,
                ALLOWED_SORT_FIELDS);

        assertEquals(0, resolvedPageRequest.pageable().getPageNumber());
        assertEquals(20, resolvedPageRequest.pageable().getPageSize());
        assertEquals("username,asc", resolvedPageRequest.sort());

        Sort.Order order = resolvedPageRequest.pageable().getSort().getOrderFor("username");
        assertNotNull(order);
        assertEquals(Sort.Direction.ASC, order.getDirection());
    }

    @Test
    void shouldClampNegativePageToZero() {
        PageRequestFactory.ResolvedPageRequest resolvedPageRequest = pageRequestFactory.create(
                -7,
                25,
                null,
                DEFAULT_SORT_FIELD,
                ALLOWED_SORT_FIELDS);

        assertEquals(0, resolvedPageRequest.pageable().getPageNumber());
        assertEquals(25, resolvedPageRequest.pageable().getPageSize());
    }

    @Test
    void shouldClampSizeBelowOneToOne() {
        PageRequestFactory.ResolvedPageRequest resolvedPageRequest = pageRequestFactory.create(
                3,
                0,
                null,
                DEFAULT_SORT_FIELD,
                ALLOWED_SORT_FIELDS);

        assertEquals(3, resolvedPageRequest.pageable().getPageNumber());
        assertEquals(1, resolvedPageRequest.pageable().getPageSize());
    }

    @Test
    void shouldClampSizeAboveMaxToOneHundred() {
        PageRequestFactory.ResolvedPageRequest resolvedPageRequest = pageRequestFactory.create(
                1,
                500,
                null,
                DEFAULT_SORT_FIELD,
                ALLOWED_SORT_FIELDS);

        assertEquals(1, resolvedPageRequest.pageable().getPageNumber());
        assertEquals(100, resolvedPageRequest.pageable().getPageSize());
    }

    @Test
    void shouldParseSortFieldAndDirection() {
        PageRequestFactory.ResolvedPageRequest resolvedPageRequest = pageRequestFactory.create(
                2,
                10,
                "createdAt,desc",
                DEFAULT_SORT_FIELD,
                ALLOWED_SORT_FIELDS);

        assertEquals("createdAt,desc", resolvedPageRequest.sort());

        Sort.Order order = resolvedPageRequest.pageable().getSort().getOrderFor("createdAt");
        assertNotNull(order);
        assertEquals(Sort.Direction.DESC, order.getDirection());
    }

    @Test
    void shouldDefaultDirectionToAscWhenDirectionIsMissing() {
        PageRequestFactory.ResolvedPageRequest resolvedPageRequest = pageRequestFactory.create(
                0,
                10,
                "createdAt",
                DEFAULT_SORT_FIELD,
                ALLOWED_SORT_FIELDS);

        assertEquals("createdAt,asc", resolvedPageRequest.sort());

        Sort.Order order = resolvedPageRequest.pageable().getSort().getOrderFor("createdAt");
        assertNotNull(order);
        assertEquals(Sort.Direction.ASC, order.getDirection());
    }

    @Test
    void shouldDefaultDirectionToAscWhenDirectionIsInvalid() {
        PageRequestFactory.ResolvedPageRequest resolvedPageRequest = pageRequestFactory.create(
                0,
                10,
                "createdAt,sideways",
                DEFAULT_SORT_FIELD,
                ALLOWED_SORT_FIELDS);

        assertEquals("createdAt,asc", resolvedPageRequest.sort());

        Sort.Order order = resolvedPageRequest.pageable().getSort().getOrderFor("createdAt");
        assertNotNull(order);
        assertEquals(Sort.Direction.ASC, order.getDirection());
    }

    @Test
    void shouldRejectUnsupportedSortField() {
        ApiException exception = assertThrows(
                ApiException.class,
                () -> pageRequestFactory.create(
                        0,
                        20,
                        "status,desc",
                        DEFAULT_SORT_FIELD,
                        ALLOWED_SORT_FIELDS));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals(ErrorCode.BAD_REQUEST, exception.getCode());
        assertEquals("Unsupported sort field 'status'.", exception.getMessage());
    }
}

