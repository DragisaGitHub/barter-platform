package com.barterplatform.application.common.pagination;

import com.barterplatform.api.model.AdminCategoryPagedResponse;
import com.barterplatform.api.model.AdminCategoryResponse;
import com.barterplatform.api.model.AdminListingPagedResponse;
import com.barterplatform.api.model.AdminListingSummaryResponse;
import com.barterplatform.api.model.AdminTradeReviewPagedResponse;
import com.barterplatform.api.model.AdminTradeReviewSummaryResponse;
import com.barterplatform.api.model.AdminTagPagedResponse;
import com.barterplatform.api.model.AdminTagResponse;
import com.barterplatform.api.model.ItemPagedResponse;
import com.barterplatform.api.model.ItemSummaryResponse;
import com.barterplatform.api.model.NotificationPagedResponse;
import com.barterplatform.api.model.NotificationResponse;
import com.barterplatform.api.model.ReportPagedResponse;
import com.barterplatform.api.model.ReportSummaryResponse;
import com.barterplatform.api.model.SavedSearchPagedResponse;
import com.barterplatform.api.model.SavedSearchResponse;
import com.barterplatform.api.model.TradeOfferPagedResponse;
import com.barterplatform.api.model.TradeOfferSummaryResponse;
import com.barterplatform.api.model.UserPagedResponse;
import com.barterplatform.api.model.UserSummaryResponse;
import com.barterplatform.api.model.UserTradeReviewPagedResponse;
import com.barterplatform.api.model.UserTradeReviewSummaryResponse;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

@Component
public class PageResponseMapper {

    public AdminCategoryPagedResponse toAdminCategoryPagedResponse(
            Page<?> page,
            List<AdminCategoryResponse> content,
            String sort) {
        return new AdminCategoryPagedResponse()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .sort(sort);
    }

    public AdminTagPagedResponse toAdminTagPagedResponse(
            Page<?> page,
            List<AdminTagResponse> content,
            String sort) {
        return new AdminTagPagedResponse()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .sort(sort);
    }

    public AdminListingPagedResponse toAdminListingPagedResponse(
            Page<?> page,
            List<AdminListingSummaryResponse> content,
            String sort) {
        return new AdminListingPagedResponse()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .sort(sort);
    }

    public AdminTradeReviewPagedResponse toAdminTradeReviewPagedResponse(
            Page<?> page,
            List<AdminTradeReviewSummaryResponse> content,
            String sort) {
        return new AdminTradeReviewPagedResponse()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .sort(sort);
    }

    public UserTradeReviewPagedResponse toUserTradeReviewPagedResponse(
            Page<?> page,
            List<UserTradeReviewSummaryResponse> content,
            String sort) {
        return new UserTradeReviewPagedResponse()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .sort(sort);
    }

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

    public SavedSearchPagedResponse toSavedSearchPagedResponse(
            Page<?> page,
            List<SavedSearchResponse> content,
            String sort) {
        return new SavedSearchPagedResponse()
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

    public ReportPagedResponse toReportPagedResponse(
            Page<?> page,
            List<ReportSummaryResponse> content,
            String sort) {
        return new ReportPagedResponse()
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

