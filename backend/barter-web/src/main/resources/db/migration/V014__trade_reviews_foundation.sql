-- Trade reviews foundation

CREATE TABLE trade_reviews (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE,
    trade_offer_id BIGINT NOT NULL,
    reviewer_user_id BIGINT NOT NULL,
    reviewed_user_id BIGINT NOT NULL,
    rating VARCHAR(32) NOT NULL,
    negative_reason VARCHAR(64),
    comment TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_trade_reviews_trade_offer FOREIGN KEY (trade_offer_id) REFERENCES trade_offers (id),
    CONSTRAINT fk_trade_reviews_reviewer_user FOREIGN KEY (reviewer_user_id) REFERENCES users (id),
    CONSTRAINT fk_trade_reviews_reviewed_user FOREIGN KEY (reviewed_user_id) REFERENCES users (id),
    CONSTRAINT uk_trade_reviews_trade_offer_reviewer UNIQUE (trade_offer_id, reviewer_user_id),
    CONSTRAINT ck_trade_reviews_self_review CHECK (reviewer_user_id <> reviewed_user_id),
    CONSTRAINT ck_trade_reviews_rating CHECK (rating IN ('POSITIVE', 'NEGATIVE')),
    CONSTRAINT ck_trade_reviews_negative_reason_required CHECK (
        (rating = 'NEGATIVE' AND negative_reason IS NOT NULL)
        OR (rating = 'POSITIVE' AND negative_reason IS NULL)
    ),
    CONSTRAINT ck_trade_reviews_negative_reason_enum CHECK (
        negative_reason IS NULL
        OR negative_reason IN (
            'NO_SHOW',
            'ITEM_NOT_AS_DESCRIBED',
            'DAMAGED_OR_UNSAFE_ITEM',
            'RUDE_OR_ABUSIVE_BEHAVIOR',
            'SPAM_OR_SCAM_BEHAVIOR',
            'OTHER'
        )
    ),
    CONSTRAINT ck_trade_reviews_other_comment_required CHECK (
        negative_reason IS DISTINCT FROM 'OTHER'
        OR (comment IS NOT NULL AND btrim(comment) <> '')
    )
);

CREATE INDEX idx_trade_reviews_trade_offer_id
    ON trade_reviews (trade_offer_id);

CREATE INDEX idx_trade_reviews_reviewed_user_rating
    ON trade_reviews (reviewed_user_id, rating);

CREATE INDEX idx_trade_reviews_reviewer_user_id
    ON trade_reviews (reviewer_user_id);

CREATE INDEX idx_trade_reviews_created_at_desc
    ON trade_reviews (created_at DESC);

CREATE INDEX idx_trade_reviews_rating_created_at_desc
    ON trade_reviews (rating, created_at DESC);

