package com.barterplatform.domain.trade.entity;

import com.barterplatform.common.persistence.AuditableEntity;
import com.barterplatform.domain.trade.enums.TradeOfferStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "trade_offers")
public class TradeOfferEntity extends AuditableEntity {

    @Column(name = "sender_user_id", nullable = false)
    private Long senderUserId;

    @Column(name = "receiver_user_id", nullable = false)
    private Long receiverUserId;

    @Column(name = "sender_item_id", nullable = false)
    private Long senderItemId;

    @Column(name = "receiver_item_id", nullable = false)
    private Long receiverItemId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private TradeOfferStatus status;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Column(name = "responded_at")
    private OffsetDateTime respondedAt;

    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;

    // --- Domain methods ---

    public boolean isPending() {
        return status == TradeOfferStatus.PENDING;
    }

    public void accept() {
        assertPending("accept");
        this.status = TradeOfferStatus.ACCEPTED;
        this.respondedAt = OffsetDateTime.now();
    }

    public void reject() {
        assertPending("reject");
        this.status = TradeOfferStatus.REJECTED;
        this.respondedAt = OffsetDateTime.now();
    }

    public void cancel() {
        assertPending("cancel");
        this.status = TradeOfferStatus.CANCELLED;
        this.respondedAt = OffsetDateTime.now();
    }

    public void expire() {
        assertPending("expire");
        this.status = TradeOfferStatus.EXPIRED;
        this.respondedAt = OffsetDateTime.now();
    }

    private void assertPending(String action) {
        if (status != TradeOfferStatus.PENDING) {
            throw new IllegalStateException(
                    "Cannot " + action + " trade offer in status " + status + "; must be PENDING");
        }
    }
}

