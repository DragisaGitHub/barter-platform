package com.barterplatform.domain.trade.entity;

import com.barterplatform.domain.trade.enums.TradeOfferMode;
import com.barterplatform.domain.trade.enums.TradeOfferStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

class TradeOfferEntityTest {

    private TradeOfferEntity offer;

    @BeforeEach
    void setUp() {
        offer = new TradeOfferEntity();
        offer.setSenderUserId(1L);
        offer.setReceiverUserId(2L);
        offer.setSenderItemId(10L);
        offer.setReceiverItemId(20L);
        offer.setMode(TradeOfferMode.ITEM_EXCHANGE);
        offer.setStatus(TradeOfferStatus.PENDING);
    }

    @Test
    void isPending_whenStatusIsPending_returnsTrue() {
        assertTrue(offer.isPending());
    }

    @ParameterizedTest
    @EnumSource(value = TradeOfferStatus.class, names = {"ACCEPTED", "COMPLETED", "REJECTED", "CANCELLED", "EXPIRED", "INVALIDATED"})
    void isPending_whenTerminalStatus_returnsFalse(TradeOfferStatus status) {
        offer.setStatus(status);
        assertFalse(offer.isPending());
    }

    @Test
    void isCompleted_whenStatusIsCompleted_returnsTrue() {
        offer.setStatus(TradeOfferStatus.COMPLETED);
        assertTrue(offer.isCompleted());
    }

    @Test
    void accept_fromPending_transitionsToAccepted() {
        offer.accept();
        assertEquals(TradeOfferStatus.ACCEPTED, offer.getStatus());
        assertNotNull(offer.getRespondedAt());
    }

    @Test
    void reject_fromPending_transitionsToRejected() {
        offer.reject();
        assertEquals(TradeOfferStatus.REJECTED, offer.getStatus());
        assertNotNull(offer.getRespondedAt());
    }

    @Test
    void cancel_fromPending_transitionsToCancelled() {
        offer.cancel();
        assertEquals(TradeOfferStatus.CANCELLED, offer.getStatus());
        assertNotNull(offer.getRespondedAt());
    }

    @Test
    void expire_fromPending_transitionsToExpired() {
        offer.expire();
        assertEquals(TradeOfferStatus.EXPIRED, offer.getStatus());
        assertNotNull(offer.getRespondedAt());
    }

    @ParameterizedTest
    @EnumSource(value = TradeOfferStatus.class, names = {"ACCEPTED", "COMPLETED", "REJECTED", "CANCELLED", "EXPIRED", "INVALIDATED"})
    void accept_fromTerminalStatus_throwsException(TradeOfferStatus status) {
        offer.setStatus(status);
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> offer.accept());
        assertTrue(ex.getMessage().contains("Cannot accept"));
        assertTrue(ex.getMessage().contains(status.name()));
    }

    @ParameterizedTest
    @EnumSource(value = TradeOfferStatus.class, names = {"ACCEPTED", "COMPLETED", "REJECTED", "CANCELLED", "EXPIRED", "INVALIDATED"})
    void reject_fromTerminalStatus_throwsException(TradeOfferStatus status) {
        offer.setStatus(status);
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> offer.reject());
        assertTrue(ex.getMessage().contains("Cannot reject"));
    }

    @ParameterizedTest
    @EnumSource(value = TradeOfferStatus.class, names = {"ACCEPTED", "COMPLETED", "REJECTED", "CANCELLED", "EXPIRED", "INVALIDATED"})
    void cancel_fromTerminalStatus_throwsException(TradeOfferStatus status) {
        offer.setStatus(status);
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> offer.cancel());
        assertTrue(ex.getMessage().contains("Cannot cancel"));
    }

    @ParameterizedTest
    @EnumSource(value = TradeOfferStatus.class, names = {"ACCEPTED", "COMPLETED", "REJECTED", "CANCELLED", "EXPIRED", "INVALIDATED"})
    void expire_fromTerminalStatus_throwsException(TradeOfferStatus status) {
        offer.setStatus(status);
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> offer.expire());
        assertTrue(ex.getMessage().contains("Cannot expire"));
    }

    @Test
    void confirmCompletionBySender_fromAccepted_setsSenderCompletionOnly() {
        offer.accept();

        offer.confirmCompletionBySender();

        assertEquals(TradeOfferStatus.ACCEPTED, offer.getStatus());
        assertNotNull(offer.getSenderCompletedAt());
        assertNull(offer.getReceiverCompletedAt());
        assertNull(offer.getCompletedAt());
    }

    @Test
    void confirmCompletionByReceiver_afterSenderConfirmation_completesTrade() {
        offer.accept();
        offer.confirmCompletionBySender();

        offer.confirmCompletionByReceiver();

        assertEquals(TradeOfferStatus.COMPLETED, offer.getStatus());
        assertNotNull(offer.getSenderCompletedAt());
        assertNotNull(offer.getReceiverCompletedAt());
        assertNotNull(offer.getCompletedAt());
    }

    @Test
    void confirmCompletionBySender_twice_throwsException() {
        offer.accept();
        offer.confirmCompletionBySender();

        IllegalStateException ex = assertThrows(IllegalStateException.class, offer::confirmCompletionBySender);

        assertTrue(ex.getMessage().contains("already confirmed"));
    }

    @ParameterizedTest
    @EnumSource(value = TradeOfferStatus.class, names = {"PENDING", "COMPLETED", "REJECTED", "CANCELLED", "EXPIRED", "INVALIDATED"})
    void confirmCompletion_fromNonAcceptedStatus_throwsException(TradeOfferStatus status) {
        offer.setStatus(status);

        IllegalStateException ex = assertThrows(IllegalStateException.class, offer::confirmCompletionBySender);

        assertTrue(ex.getMessage().contains("must be ACCEPTED"));
    }
}
