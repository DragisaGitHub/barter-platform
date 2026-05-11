CREATE TABLE trade_offer_messages (
                                      id BIGSERIAL PRIMARY KEY,
                                      uuid UUID NOT NULL UNIQUE,
                                      trade_offer_id BIGINT NOT NULL,
                                      sender_user_id BIGINT NOT NULL,
                                      recipient_user_id BIGINT NOT NULL,
                                      content TEXT NOT NULL,
                                      is_read BOOLEAN NOT NULL DEFAULT FALSE,
                                      read_at TIMESTAMPTZ,
                                      created_at TIMESTAMPTZ NOT NULL,
                                      updated_at TIMESTAMPTZ,
                                      CONSTRAINT fk_trade_offer_messages_trade_offer
                                          FOREIGN KEY (trade_offer_id) REFERENCES trade_offers(id),
                                      CONSTRAINT fk_trade_offer_messages_sender_user
                                          FOREIGN KEY (sender_user_id) REFERENCES users(id),
                                      CONSTRAINT fk_trade_offer_messages_recipient_user
                                          FOREIGN KEY (recipient_user_id) REFERENCES users(id),
                                      CONSTRAINT ck_trade_offer_messages_content_length
                                          CHECK (char_length(trim(content)) > 0 AND char_length(content) <= 2000)
);

CREATE INDEX idx_trade_offer_messages_trade_offer_id
    ON trade_offer_messages(trade_offer_id);

CREATE INDEX idx_trade_offer_messages_trade_offer_created_at
    ON trade_offer_messages(trade_offer_id, created_at);

CREATE INDEX idx_trade_offer_messages_recipient_unread
    ON trade_offer_messages(recipient_user_id, is_read);