package com.barterplatform.application.common.pagination;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.barterplatform.api.model.UserPagedResponse;
import com.barterplatform.api.model.UserStatus;
import com.barterplatform.api.model.UserSummaryResponse;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

class PageResponseMapperTest {

    private final PageResponseMapper pageResponseMapper = new PageResponseMapper();

    @Test
    void shouldMapUserPagedResponseContentAndMetadata() {
        List<UserSummaryResponse> content = List.of(
                userSummaryResponse("alice", "alice@example.com"),
                userSummaryResponse("bob", "bob@example.com"));

        PageImpl<String> page = new PageImpl<>(
                List.of("first", "second"),
                PageRequest.of(1, 2),
                5);

        UserPagedResponse response = pageResponseMapper.toUserPagedResponse(page, content, "createdAt,desc");

        assertSame(content, response.getContent());
        assertEquals(1, response.getPage());
        assertEquals(2, response.getSize());
        assertEquals(5L, response.getTotalElements());
        assertEquals(3, response.getTotalPages());
        assertFalse(response.getFirst());
        assertFalse(response.getLast());
        assertEquals("createdAt,desc", response.getSort());
    }

    @Test
    void shouldPreserveFirstAndLastFlagsFromPage() {
        List<UserSummaryResponse> content = List.of(userSummaryResponse("carol", "carol@example.com"));

        PageImpl<String> page = new PageImpl<>(
                List.of("only"),
                PageRequest.of(0, 20),
                1);

        UserPagedResponse response = pageResponseMapper.toUserPagedResponse(page, content, "username,asc");

        assertTrue(response.getFirst());
        assertTrue(response.getLast());
        assertEquals(1L, response.getTotalElements());
        assertEquals(1, response.getTotalPages());
        assertEquals("username,asc", response.getSort());
    }

    private UserSummaryResponse userSummaryResponse(String username, String email) {
        return new UserSummaryResponse(
                UUID.randomUUID(),
                username,
                email,
                UserStatus.ACTIVE,
                true,
                false,
                OffsetDateTime.parse("2026-05-07T10:15:30Z"));
    }
}

