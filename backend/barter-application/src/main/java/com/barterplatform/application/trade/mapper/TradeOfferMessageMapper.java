package com.barterplatform.application.trade.mapper;

import com.barterplatform.api.model.TradeOfferMessageResponse;
import com.barterplatform.domain.identity.entity.UserEntity;
import com.barterplatform.domain.trade.entity.TradeOfferEntity;
import com.barterplatform.domain.trade.entity.TradeOfferMessageEntity;
import org.springframework.stereotype.Component;

@Component
public class TradeOfferMessageMapper {

    public TradeOfferMessageResponse toResponse(
            TradeOfferMessageEntity message,
            TradeOfferEntity tradeOffer,
            UserEntity sender,
            UserEntity recipient) {
        return new TradeOfferMessageResponse()
                .uuid(message.getUuid())
                .tradeOfferUuid(tradeOffer.getUuid())
                .senderUserUuid(sender.getUuid())
                .senderUsername(sender.getUsername())
                .recipientUserUuid(recipient.getUuid())
                .recipientUsername(recipient.getUsername())
                .content(message.getContent())
                .isRead(message.isRead())
                .readAt(message.getReadAt())
                .createdAt(message.getCreatedAt());
    }
}