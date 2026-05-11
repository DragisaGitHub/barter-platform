package com.barterplatform.infrastructure.trade.repository;

import com.barterplatform.domain.trade.entity.TradeOfferEntity;
import com.barterplatform.domain.trade.enums.TradeOfferStatus;
import java.util.Collection;
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

    long countBySenderUserIdAndStatus(Long senderUserId, TradeOfferStatus status);

    long countByReceiverUserIdAndStatus(Long receiverUserId, TradeOfferStatus status);

    /**
     * Find all PENDING offers (excluding the accepted one) that involve any of the given item IDs,
     * either as the receiver_item_id on the offer itself or as an item in trade_offer_items.
     */
    @Query("""
            SELECT DISTINCT o FROM TradeOfferEntity o
            LEFT JOIN o.items toi
            WHERE o.status = 'PENDING'
              AND o.id <> :acceptedOfferId
              AND (o.receiverItemId IN (:itemIds)
                OR o.senderItemId IN (:itemIds)
                OR toi.itemId IN (:itemIds))
            """)
    List<TradeOfferEntity> findCompetingPendingOffers(
            @Param("acceptedOfferId") Long acceptedOfferId,
            @Param("itemIds") Collection<Long> itemIds);

    /**
     * @deprecated Use {@link #findCompetingPendingOffers(Long, Collection)} instead.
     */
    @Deprecated
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
