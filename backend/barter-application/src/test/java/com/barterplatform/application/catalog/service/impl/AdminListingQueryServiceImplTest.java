package com.barterplatform.application.catalog.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.barterplatform.application.catalog.mapper.ItemImageMapper;
import com.barterplatform.application.common.pagination.PageRequestFactory;
import com.barterplatform.application.common.pagination.PageResponseMapper;
import com.barterplatform.domain.catalog.entity.CategoryEntity;
import com.barterplatform.domain.catalog.entity.ItemEntity;
import com.barterplatform.domain.catalog.enums.ItemCondition;
import com.barterplatform.domain.catalog.enums.ItemStatus;
import com.barterplatform.domain.identity.entity.UserEntity;
import com.barterplatform.infrastructure.catalog.repository.CategoryRepository;
import com.barterplatform.infrastructure.catalog.repository.ItemImageRepository;
import com.barterplatform.infrastructure.catalog.repository.ItemRepository;
import com.barterplatform.infrastructure.catalog.repository.ItemTagRepository;
import com.barterplatform.infrastructure.catalog.repository.ListingModerationActionRepository;
import com.barterplatform.infrastructure.catalog.repository.TagRepository;
import com.barterplatform.infrastructure.identity.repository.UserRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class AdminListingQueryServiceImplTest {

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private TagRepository tagRepository;

    @Mock
    private ItemTagRepository itemTagRepository;

    @Mock
    private ItemImageRepository itemImageRepository;

    @Mock
    private ListingModerationActionRepository listingModerationActionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ItemImageMapper itemImageMapper;

    private AdminListingQueryServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AdminListingQueryServiceImpl(
                itemRepository,
                categoryRepository,
                tagRepository,
                itemTagRepository,
                itemImageRepository,
                listingModerationActionRepository,
                userRepository,
                itemImageMapper,
                new PageRequestFactory(),
                new PageResponseMapper());
    }

    @Test
    void listListingsFiltersByOwnerQueryCaseInsensitively() {
        UserEntity owner = user(77L, "alice", "alice@example.com");
        CategoryEntity category = category(33L, "Furniture");
        ItemEntity item = item(101L, owner.getId(), category.getId(), "Desk Lamp");

        when(userRepository.findIdsByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase("alice"))
                .thenReturn(List.of(owner.getId()));
        when(itemRepository.findAll(org.mockito.ArgumentMatchers.<Specification<ItemEntity>>any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(item), PageRequest.of(0, 20), 1));
        when(categoryRepository.findAllById(any())).thenReturn(List.of(category));
        when(userRepository.findAllById(any())).thenReturn(List.of(owner));
        when(itemImageRepository.findFirstByItemIdAndPrimaryTrue(item.getId())).thenReturn(Optional.empty());

        var response = service.listListings(0, 20, "createdAt,desc", null, "Alice", null, null);

        assertEquals(1, response.getContent().size());
        assertEquals("alice", response.getContent().getFirst().getOwnerUsername());
        assertEquals(owner.getUuid(), response.getContent().getFirst().getOwnerUuid());
        assertEquals("Furniture", response.getContent().getFirst().getCategoryName());
        verify(userRepository).findIdsByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase("alice");
    }

    @Test
    void listListingsReturnsEmptyPageWhenOwnerFilterMatchesNoUsers() {
        when(userRepository.findIdsByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase("unknown"))
                .thenReturn(List.of());

        var response = service.listListings(2, 20, "createdAt,desc", null, "unknown", null, null);

        assertTrue(response.getContent().isEmpty());
        assertEquals(0L, response.getTotalElements());
        assertEquals(2, response.getPage());
        verify(itemRepository, never()).findAll(org.mockito.ArgumentMatchers.<Specification<ItemEntity>>any(), any(Pageable.class));
    }

    @Test
    void listListingsReturnsListingsForMultipleMatchedOwners() {
        UserEntity alice = user(77L, "alice", "alice@example.com");
        UserEntity dragisa = user(88L, "dragisa", "dragisa@example.com");
        CategoryEntity category = category(33L, "Furniture");
        ItemEntity firstItem = item(101L, alice.getId(), category.getId(), "Desk Lamp");
        ItemEntity secondItem = item(102L, dragisa.getId(), category.getId(), "Office Chair");

        when(userRepository.findIdsByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase("a"))
                .thenReturn(List.of(alice.getId(), dragisa.getId()));
        when(itemRepository.findAll(org.mockito.ArgumentMatchers.<Specification<ItemEntity>>any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(firstItem, secondItem), PageRequest.of(0, 20), 2));
        when(categoryRepository.findAllById(any())).thenReturn(List.of(category));
        when(userRepository.findAllById(any())).thenReturn(List.of(alice, dragisa));
        when(itemImageRepository.findFirstByItemIdAndPrimaryTrue(firstItem.getId())).thenReturn(Optional.empty());
        when(itemImageRepository.findFirstByItemIdAndPrimaryTrue(secondItem.getId())).thenReturn(Optional.empty());

        var response = service.listListings(0, 20, "createdAt,desc", null, "a", null, null);

        assertEquals(2, response.getContent().size());
        assertEquals(List.of("alice", "dragisa"),
                response.getContent().stream().map(it -> it.getOwnerUsername()).toList());
        verify(userRepository).findIdsByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase("a");
    }

    private UserEntity user(Long id, String username, String email) {
        UserEntity entity = new UserEntity();
        entity.setId(id);
        entity.setUuid(UUID.randomUUID());
        entity.setUsername(username);
        entity.setEmail(email);
        entity.setCreatedAt(OffsetDateTime.parse("2026-05-15T10:15:30Z"));
        return entity;
    }

    private CategoryEntity category(Long id, String name) {
        CategoryEntity entity = new CategoryEntity();
        entity.setId(id);
        entity.setUuid(UUID.randomUUID());
        entity.setName(name);
        entity.setSlug(name.toLowerCase().replace(" ", "-"));
        entity.setSortOrder(0);
        entity.setCreatedAt(OffsetDateTime.parse("2026-05-15T10:15:30Z"));
        return entity;
    }

    private ItemEntity item(Long id, Long ownerId, Long categoryId, String title) {
        ItemEntity entity = new ItemEntity();
        entity.setId(id);
        entity.setUuid(UUID.randomUUID());
        entity.setOwnerId(ownerId);
        entity.setCategoryId(categoryId);
        entity.setTitle(title);
        entity.setStatus(ItemStatus.ACTIVE);
        entity.setCondition(ItemCondition.GOOD);
        entity.setCreatedAt(OffsetDateTime.parse("2026-05-15T10:15:30Z"));
        return entity;
    }
}

