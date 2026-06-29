package com.barterplatform.web.bootstrap;

import com.barterplatform.domain.catalog.entity.CategoryEntity;
import com.barterplatform.domain.catalog.entity.TagEntity;
import com.barterplatform.infrastructure.catalog.repository.CategoryRepository;
import com.barterplatform.infrastructure.catalog.repository.TagRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds default categories and tags for development/test environments.
 *
 * <p>This seeder is controlled by {@code barter.seed.demo-content} (default: false).
 * Production databases start completely empty of business content — admins
 * create all categories, tags, and catalog entries manually after launch.
 *
 * <p>Enable in local/dev/test via:
 * <pre>
 *   barter.seed.demo-content=true
 * </pre>
 */
@Component
@Order(10)
public class DemoContentSeeder {

    private static final Logger log = LoggerFactory.getLogger(DemoContentSeeder.class);

    private final DemoContentSeedProperties properties;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;

    public DemoContentSeeder(DemoContentSeedProperties properties,
                             CategoryRepository categoryRepository,
                             TagRepository tagRepository) {
        this.properties = properties;
        this.categoryRepository = categoryRepository;
        this.tagRepository = tagRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void onApplicationReady() {
        if (!properties.demoContent()) {
            log.debug("Demo content seeding is disabled (barter.seed.demo-content=false)");
            return;
        }

        log.info("Demo content seeding is enabled — inserting default categories and tags if absent");

        seedCategories();
        seedTags();
    }

    private void seedCategories() {
        List<CategorySeed> categories = List.of(
                new CategorySeed("c0a80101-0001-4000-8000-000000000001", "Toys", "toys", 1),
                new CategorySeed("c0a80101-0002-4000-8000-000000000002", "Books", "books", 2),
                new CategorySeed("c0a80101-0003-4000-8000-000000000003", "Electronics", "electronics", 3),
                new CategorySeed("c0a80101-0004-4000-8000-000000000004", "Clothes", "clothes", 4),
                new CategorySeed("c0a80101-0005-4000-8000-000000000005", "Home", "home", 5),
                new CategorySeed("c0a80101-0006-4000-8000-000000000006", "Sports", "sports", 6)
        );

        for (CategorySeed seed : categories) {
            if (!categoryRepository.existsBySlug(seed.slug())) {
                CategoryEntity entity = new CategoryEntity();
                entity.setUuid(UUID.fromString(seed.uuid()));
                entity.setName(seed.name());
                entity.setSlug(seed.slug());
                entity.setSortOrder(seed.sortOrder());
                entity.setCreatedAt(OffsetDateTime.now());
                categoryRepository.save(entity);
                log.debug("Seeded category: {}", seed.name());
            }
        }
    }

    private void seedTags() {
        List<TagSeed> tags = List.of(
                new TagSeed("d0a80101-0001-4000-8000-000000000001", "Kids", "kids"),
                new TagSeed("d0a80101-0002-4000-8000-000000000002", "Collectible", "collectible"),
                new TagSeed("d0a80101-0003-4000-8000-000000000003", "Vintage", "vintage"),
                new TagSeed("d0a80101-0004-4000-8000-000000000004", "New", "new"),
                new TagSeed("d0a80101-0005-4000-8000-000000000005", "Used", "used"),
                new TagSeed("d0a80101-0006-4000-8000-000000000006", "Handmade", "handmade")
        );

        for (TagSeed seed : tags) {
            if (!tagRepository.existsBySlug(seed.slug())) {
                TagEntity entity = new TagEntity();
                entity.setUuid(UUID.fromString(seed.uuid()));
                entity.setName(seed.name());
                entity.setSlug(seed.slug());
                entity.setCreatedAt(OffsetDateTime.now());
                tagRepository.save(entity);
                log.debug("Seeded tag: {}", seed.name());
            }
        }
    }

    private record CategorySeed(String uuid, String name, String slug, int sortOrder) {}
    private record TagSeed(String uuid, String name, String slug) {}
}
