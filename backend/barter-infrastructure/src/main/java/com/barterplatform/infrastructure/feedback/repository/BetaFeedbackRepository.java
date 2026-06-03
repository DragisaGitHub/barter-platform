package com.barterplatform.infrastructure.feedback.repository;

import com.barterplatform.domain.feedback.entity.BetaFeedbackEntity;
import com.barterplatform.domain.feedback.enums.BetaFeedbackStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BetaFeedbackRepository extends JpaRepository<BetaFeedbackEntity, Long> {

    Optional<BetaFeedbackEntity> findByUuid(UUID uuid);

    Page<BetaFeedbackEntity> findAllByStatus(BetaFeedbackStatus status, Pageable pageable);
}

