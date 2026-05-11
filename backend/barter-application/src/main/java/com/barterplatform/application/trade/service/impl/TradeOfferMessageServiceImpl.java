package com.barterplatform.application.trade.service.impl;

import com.barterplatform.api.model.SendTradeOfferMessageRequest;
import com.barterplatform.api.model.TradeOfferMessageResponse;
import com.barterplatform.application.trade.mapper.TradeOfferMessageMapper;
import com.barterplatform.application.trade.service.TradeOfferMessageService;
import com.barterplatform.common.exception.ApiException;
import com.barterplatform.common.exception.ErrorCode;
import com.barterplatform.domain.identity.entity.UserEntity;
import com.barterplatform.domain.trade.entity.TradeOfferEntity;
import com.barterplatform.domain.trade.entity.TradeOfferMessageEntity;
import com.barterplatform.domain.trade.enums.TradeOfferStatus;
import com.barterplatform.infrastructure.identity.repository.UserRepository;
import com.barterplatform.infrastructure.trade.repository.TradeOfferMessageRepository;
import com.barterplatform.infrastructure.trade.repository.TradeOfferRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TradeOfferMessageServiceImpl implements TradeOfferMessageService {

    private final TradeOfferRepository tradeOfferRepository;
    private final TradeOfferMessageRepository tradeOfferMessageRepository;
    private final UserRepository userRepository;
    private final TradeOfferMessageMapper tradeOfferMessageMapper;

    public TradeOfferMessageServiceImpl(
            TradeOfferRepository tradeOfferRepository,
            TradeOfferMessageRepository tradeOfferMessageRepository,
            UserRepository userRepository,
            TradeOfferMessageMapper tradeOfferMessageMapper) {
        this.tradeOfferRepository = tradeOfferRepository;
        this.tradeOfferMessageRepository = tradeOfferMessageRepository;
        this.userRepository = userRepository;
        this.tradeOfferMessageMapper = tradeOfferMessageMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TradeOfferMessageResponse> listMessages(UUID currentUserUuid, UUID tradeOfferUuid) {
        UserEntity currentUser = resolveUser(currentUserUuid);
        TradeOfferEntity tradeOffer = resolveTradeOffer(tradeOfferUuid);

        validateParticipant(currentUser, tradeOffer);

        List<TradeOfferMessageEntity> messages =
                tradeOfferMessageRepository.findByTradeOfferIdOrderByCreatedAtAsc(tradeOffer.getId());

        return messages.stream()
                .map(message -> {
                    UserEntity sender = resolveUser(message.getSenderUserId());
                    UserEntity recipient = resolveUser(message.getRecipientUserId());

                    if (recipient.getId().equals(currentUser.getId()) && !message.isRead()) {
                        message.markAsRead();
                    }

                    return tradeOfferMessageMapper.toResponse(
                            message,
                            tradeOffer,
                            sender,
                            recipient);
                })
                .toList();
    }

    @Override
    public TradeOfferMessageResponse sendMessage(
            UUID currentUserUuid,
            UUID tradeOfferUuid,
            SendTradeOfferMessageRequest request) {

        UserEntity currentUser = resolveUser(currentUserUuid);
        TradeOfferEntity tradeOffer = resolveTradeOffer(tradeOfferUuid);

        validateParticipant(currentUser, tradeOffer);

        if (tradeOffer.getStatus() != TradeOfferStatus.PENDING) {
            throw conflict();
        }

        String content = normalizeContent(request.getContent());

        UserEntity recipient = determineRecipient(currentUser, tradeOffer);

        TradeOfferMessageEntity message = new TradeOfferMessageEntity();
        message.setTradeOfferId(tradeOffer.getId());
        message.setSenderUserId(currentUser.getId());
        message.setRecipientUserId(recipient.getId());
        message.setContent(content);

        TradeOfferMessageEntity saved = tradeOfferMessageRepository.save(message);

        return tradeOfferMessageMapper.toResponse(
                saved,
                tradeOffer,
                currentUser,
                recipient);
    }

    // ── Private helpers ──────────────────────────────────────────

    private UserEntity resolveUser(UUID userUuid) {
        return userRepository.findByUuid(userUuid)
                .orElseThrow(() -> notFound("User not found."));
    }

    private UserEntity resolveUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> notFound("User not found."));
    }

    private TradeOfferEntity resolveTradeOffer(UUID tradeOfferUuid) {
        return tradeOfferRepository.findByUuid(tradeOfferUuid)
                .orElseThrow(() -> notFound("Trade offer not found."));
    }

    private void validateParticipant(UserEntity currentUser, TradeOfferEntity tradeOffer) {
        boolean participant =
                tradeOffer.getSenderUserId().equals(currentUser.getId())
                        || tradeOffer.getReceiverUserId().equals(currentUser.getId());

        if (!participant) {
            throw forbidden();
        }
    }

    private UserEntity determineRecipient(UserEntity currentUser, TradeOfferEntity tradeOffer) {
        Long recipientUserId =
                tradeOffer.getSenderUserId().equals(currentUser.getId())
                        ? tradeOffer.getReceiverUserId()
                        : tradeOffer.getSenderUserId();

        return resolveUser(recipientUserId);
    }

    private String normalizeContent(String content) {
        if (content == null) {
            throw badRequest("Message content is required.");
        }

        String normalized = content.trim();

        if (normalized.isBlank()) {
            throw badRequest("Message content cannot be blank.");
        }

        if (normalized.length() > 2000) {
            throw badRequest("Message content exceeds 2000 characters.");
        }

        return normalized;
    }

    private ApiException badRequest(String message) {
        return new ApiException(
                HttpStatus.BAD_REQUEST,
                ErrorCode.VALIDATION_ERROR,
                message);
    }

    private ApiException forbidden() {
        return new ApiException(
                HttpStatus.FORBIDDEN,
                ErrorCode.FORBIDDEN,
                "You are not a participant in this trade offer.");
    }

    private ApiException conflict() {
        return new ApiException(
                HttpStatus.CONFLICT,
                ErrorCode.CONFLICT,
                "Messages can only be sent while the trade offer is pending.");
    }

    private ApiException notFound(String message) {
        return new ApiException(
                HttpStatus.NOT_FOUND,
                ErrorCode.NOT_FOUND,
                message);
    }
}