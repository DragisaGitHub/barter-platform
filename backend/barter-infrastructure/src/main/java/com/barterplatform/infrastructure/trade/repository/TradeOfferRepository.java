package com.barterplatform.infrastructure.trade.repository;

import com.barterplatform.domain.trade.entity.TradeOfferEntity;
import com.barterplatform.domain.trade.enums.TradeOfferStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TradeOfferRepository extends JpaRepository<TradeOfferEntity, Long>, JpaSpecificationExecutor<TradeOfferEntity> {

    Optional<TradeOfferEntity> findByUuid(UUID uuid);

    Page<TradeOfferEntity> findByReceiverUserId(Long receiverUserId, Pageable pageable);

    Page<TradeOfferEntity> findByReceiverUserIdAndStatus(Long receiverUserId, TradeOfferStatus status, Pageable pageable);

    Page<TradeOfferEntity> findBySenderUserId(Long senderUserId, Pageable pageable);

    Page<TradeOfferEntity> findBySenderUserIdAndStatus(Long senderUserId, TradeOfferStatus status, Pageable pageable);

    @Query("""
            SELECT o FROM TradeOfferEntity o
            WHERE o.status = 'PENDING'
              AND o.id <> :acceptedOfferId
              AND (o.senderItemId IN (:senderItemId, :receiverItemId)
                OR o.receiverItemId IN (:senderItemId, :receiverItemId))
            """)
    List<TradeOfferEntity> findCompetingPendingOffers(
            @Param("acceptedOfferId") Long acceptedOfferId,
            @Param("senderItemId") Long senderItemId,
            @Param("receiverItemId") Long receiverItemId);
}

