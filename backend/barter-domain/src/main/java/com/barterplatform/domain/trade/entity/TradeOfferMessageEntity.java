package com.barterplatform.domain.trade.entity;

import com.barterplatform.common.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "trade_offer_messages")
public class TradeOfferMessageEntity extends AuditableEntity {

    @Column(name = "trade_offer_id", nullable = false)
    private Long tradeOfferId;

    @Column(name = "sender_user_id", nullable = false)
    private Long senderUserId;

    @Column(name = "recipient_user_id", nullable = false)
    private Long recipientUserId;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    @Column(name = "read_at")
    private OffsetDateTime readAt;

    public void markAsRead() {
        if (!read) {
            this.read = true;
            this.readAt = OffsetDateTime.now();
        }
    }
}