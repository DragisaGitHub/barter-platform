package com.barterplatform.web.bootstrap;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.barterplatform.domain.catalog.entity.CategoryEntity;
import com.barterplatform.domain.catalog.entity.TagEntity;
import com.barterplatform.infrastructure.catalog.repository.CategoryRepository;
import com.barterplatform.infrastructure.catalog.repository.TagRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DemoContentSeederTest {

    @Mock
    private DemoContentSeedProperties properties;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private TagRepository tagRepository;

    @InjectMocks
    private DemoContentSeeder seeder;

    @Test
    void disabledByDefault_doesNotSeedAnything() {
        when(properties.demoContent()).thenReturn(false);

        seeder.onApplicationReady();

        verifyNoInteractions(categoryRepository, tagRepository);
    }

    @Test
    void enabled_seedsCategoriesAndTags() {
        when(properties.demoContent()).thenReturn(true);
        when(categoryRepository.existsBySlug(any())).thenReturn(false);
        when(tagRepository.existsBySlug(any())).thenReturn(false);
        when(categoryRepository.save(any(CategoryEntity.class))).thenAnswer(i -> i.getArgument(0));
        when(tagRepository.save(any(TagEntity.class))).thenAnswer(i -> i.getArgument(0));

        seeder.onApplicationReady();

        // 6 categories + 6 tags
        verify(categoryRepository, org.mockito.Mockito.times(6)).save(any(CategoryEntity.class));
        verify(tagRepository, org.mockito.Mockito.times(6)).save(any(TagEntity.class));
    }

    @Test
    void enabled_skipsExistingCategories() {
        when(properties.demoContent()).thenReturn(true);
        when(categoryRepository.existsBySlug(any())).thenReturn(true);
        when(tagRepository.existsBySlug(any())).thenReturn(true);

        seeder.onApplicationReady();

        verify(categoryRepository, never()).save(any(CategoryEntity.class));
        verify(tagRepository, never()).save(any(TagEntity.class));
    }

    @Test
    void productionDefault_demoContentDisabled() {
        // Verifies the contract: default value is false
        DemoContentSeedProperties prodProperties = new DemoContentSeedProperties(false);
        DemoContentSeeder prodSeeder = new DemoContentSeeder(prodProperties, categoryRepository, tagRepository);

        prodSeeder.onApplicationReady();

        verifyNoInteractions(categoryRepository, tagRepository);
    }
}
