package com.barterplatform.application.common.pagination;

import com.barterplatform.api.model.ItemPagedResponse;
import com.barterplatform.api.model.ItemSummaryResponse;
import com.barterplatform.api.model.NotificationPagedResponse;
import com.barterplatform.api.model.NotificationResponse;
import com.barterplatform.api.model.TradeOfferPagedResponse;
import com.barterplatform.api.model.TradeOfferSummaryResponse;
import com.barterplatform.api.model.UserPagedResponse;
import com.barterplatform.api.model.UserSummaryResponse;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

@Component
public class PageResponseMapper {

    public UserPagedResponse toUserPagedResponse(
            Page<?> page,
            List<UserSummaryResponse> content,
            String sort) {
        return new UserPagedResponse()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .sort(sort);
    }

    public ItemPagedResponse toItemPagedResponse(
            Page<?> page,
            List<ItemSummaryResponse> content,
            String sort) {
        return new ItemPagedResponse()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .sort(sort);
    }

    public TradeOfferPagedResponse toTradeOfferPagedResponse(
            Page<?> page,
            List<TradeOfferSummaryResponse> content,
            String sort) {
        return new TradeOfferPagedResponse()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .sort(sort);
    }

    public NotificationPagedResponse toNotificationPagedResponse(
            Page<?> page,
            List<NotificationResponse> content,
            String sort) {
        return new NotificationPagedResponse()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .sort(sort);
    }
}

