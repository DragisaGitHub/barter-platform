package com.barterplatform.application.feedback.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.barterplatform.api.model.MessageResponse;
import com.barterplatform.domain.feedback.entity.BetaFeedbackEntity;
import com.barterplatform.domain.identity.entity.UserEntity;
import com.barterplatform.infrastructure.feedback.repository.BetaFeedbackRepository;
import com.barterplatform.infrastructure.identity.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BetaFeedbackServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private BetaFeedbackRepository betaFeedbackRepository;

    private BetaFeedbackServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new BetaFeedbackServiceImpl(userRepository, betaFeedbackRepository);
    }

    @Test
    void submitFeedbackPersistsNewFeedbackAndReturnsMessageResponse() {
        UUID userUuid = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UserEntity user = new UserEntity();
        user.setId(10L);
        user.setUuid(userUuid);
        user.setUsername("alex99");
        user.setEmail("alex@example.com");

        when(userRepository.findByUuid(userUuid)).thenReturn(Optional.of(user));
        when(betaFeedbackRepository.save(any(BetaFeedbackEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MessageResponse response = service.submitFeedback(
                userUuid,
                "ONBOARDING",
                "I was not sure whether to save a draft or publish my first listing.",
                "/dashboard");

        assertEquals("Beta feedback submitted. Thank you for helping improve the first-time experience.", response.getMessage());

        ArgumentCaptor<BetaFeedbackEntity> captor = ArgumentCaptor.forClass(BetaFeedbackEntity.class);
        verify(betaFeedbackRepository).save(captor.capture());
        BetaFeedbackEntity saved = captor.getValue();
        assertEquals(userUuid, saved.getUserUuid());
        assertEquals("alex99", saved.getUsername());
        assertEquals("alex@example.com", saved.getEmail());
        assertEquals(com.barterplatform.domain.feedback.enums.BetaFeedbackCategory.ONBOARDING, saved.getCategory());
        assertEquals(com.barterplatform.domain.feedback.enums.BetaFeedbackStatus.NEW, saved.getStatus());
        assertEquals("/dashboard", saved.getSourcePage());
    }
}

