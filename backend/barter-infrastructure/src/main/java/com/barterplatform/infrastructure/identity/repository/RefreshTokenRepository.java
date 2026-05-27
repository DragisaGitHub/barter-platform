package com.barterplatform.infrastructure.identity.repository;

import com.barterplatform.domain.identity.entity.RefreshTokenEntity;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, Long> {

    Optional<RefreshTokenEntity> findByTokenHash(String tokenHash);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update RefreshTokenEntity token
               set token.revokedAt = :revokedAt
             where token.id = :tokenId
               and token.revokedAt is null
            """)
    int revokeByIdIfActive(@Param("tokenId") Long tokenId, @Param("revokedAt") OffsetDateTime revokedAt);
}

