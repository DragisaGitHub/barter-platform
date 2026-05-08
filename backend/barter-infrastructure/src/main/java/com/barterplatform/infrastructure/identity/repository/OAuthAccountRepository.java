package com.barterplatform.infrastructure.identity.repository;

import com.barterplatform.domain.identity.entity.OAuthAccountEntity;
import com.barterplatform.domain.identity.enums.OAuthProvider;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OAuthAccountRepository extends JpaRepository<OAuthAccountEntity, Long> {

    List<OAuthAccountEntity> findAllByUserIdOrderByLinkedAtAsc(Long userId);

    Optional<OAuthAccountEntity> findByProviderAndProviderUserId(OAuthProvider provider, String providerUserId);
}

