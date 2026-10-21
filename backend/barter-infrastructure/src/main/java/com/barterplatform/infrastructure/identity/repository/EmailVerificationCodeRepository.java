package com.barterplatform.infrastructure.identity.repository;

import com.barterplatform.domain.identity.entity.EmailVerificationCodeEntity;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface EmailVerificationCodeRepository extends JpaRepository<EmailVerificationCodeEntity, Long> {

    Optional<EmailVerificationCodeEntity> findFirstByUserIdAndUsedAtIsNullOrderByCreatedAtDesc(Long userId);

    Optional<EmailVerificationCodeEntity> findFirstByUserIdOrderByCreatedAtDesc(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from EmailVerificationCodeEntity c where c.userId = :userId order by c.createdAt desc")
    List<EmailVerificationCodeEntity> findByUserIdForUpdate(@Param("userId") Long userId, Pageable pageable);

    long countByUserIdAndCreatedAtAfterAndCountsTowardIssuanceLimitTrue(Long userId, OffsetDateTime createdAt);

    Optional<EmailVerificationCodeEntity> findFirstByUserIdAndCreatedAtAfterAndCountsTowardIssuanceLimitTrueOrderByCreatedAtAsc(
            Long userId, OffsetDateTime createdAt);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update EmailVerificationCodeEntity c set c.invalidatedAt = :invalidatedAt "
            + "where c.userId = :userId and c.usedAt is null and c.invalidatedAt is null")
    int invalidateActiveCodes(@Param("userId") Long userId, @Param("invalidatedAt") OffsetDateTime invalidatedAt);

    void deleteAllByUserId(Long userId);
}

