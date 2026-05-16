package com.barterplatform.domain.trade.entity;

import com.barterplatform.common.persistence.AuditableEntity;
import com.barterplatform.domain.trade.enums.TradeOfferMode;
import com.barterplatform.domain.trade.enums.TradeOfferStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
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

    @Column(name = "sender_item_id")
    private Long senderItemId;

    @Column(name = "receiver_item_id", nullable = false)
    private Long receiverItemId;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode", nullable = false, length = 32)
    private TradeOfferMode mode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private TradeOfferStatus status;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Column(name = "responded_at")
    private OffsetDateTime respondedAt;

    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;

    @Column(name = "sender_completed_at")
    private OffsetDateTime senderCompletedAt;

    @Column(name = "receiver_completed_at")
    private OffsetDateTime receiverCompletedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @OneToMany(mappedBy = "tradeOffer", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<TradeOfferItemEntity> items = new ArrayList<>();

    // --- Domain methods ---

    public boolean isPending() {
        return status == TradeOfferStatus.PENDING;
    }

    public boolean isAccepted() {
        return status == TradeOfferStatus.ACCEPTED;
    }

    public boolean isCompleted() {
        return status == TradeOfferStatus.COMPLETED;
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

    public void invalidate() {
        assertPending("invalidate");
        this.status = TradeOfferStatus.INVALIDATED;
        this.respondedAt = OffsetDateTime.now();
    }

    public void confirmCompletionBySender() {
        assertAcceptedForCompletion();
        if (senderCompletedAt != null) {
            throw new IllegalStateException("Sender has already confirmed completion.");
        }

        this.senderCompletedAt = OffsetDateTime.now();
        completeIfBothConfirmed();
    }

    public void confirmCompletionByReceiver() {
        assertAcceptedForCompletion();
        if (receiverCompletedAt != null) {
            throw new IllegalStateException("Receiver has already confirmed completion.");
        }

        this.receiverCompletedAt = OffsetDateTime.now();
        completeIfBothConfirmed();
    }

    public void completeIfBothConfirmed() {
        if (senderCompletedAt != null && receiverCompletedAt != null) {
            this.status = TradeOfferStatus.COMPLETED;
            if (completedAt == null) {
                this.completedAt = OffsetDateTime.now();
            }
        }
    }

    private void assertPending(String action) {
        if (status != TradeOfferStatus.PENDING) {
            throw new IllegalStateException(
                    "Cannot " + action + " trade offer in status " + status + "; must be PENDING");
        }
    }

    private void assertAcceptedForCompletion() {
        if (status != TradeOfferStatus.ACCEPTED) {
            throw new IllegalStateException(
                    "Cannot confirm completion for trade offer in status " + status + "; must be ACCEPTED");
        }
    }
}

