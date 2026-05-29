/**
 * Re-exports from the OpenAPI-generated schema for convenient usage.
 *
 * These types are derived from the backend contract at:
 *   backend/barter-api/src/main/resources/openapi/openapi.yaml
 *
 * Regenerate with: yarn generate:api
 */
import type {components} from "./schema";

// ─── Enums ──────────────────────────────────────────────────────────────────

export type UserStatus = components["schemas"]["UserStatus"];
export type PreferredLanguage = components["schemas"]["PreferredLanguage"];

// ─── Response DTOs ──────────────────────────────────────────────────────────

export type PingResponse = components["schemas"]["PingResponse"];
export type ErrorResponse = components["schemas"]["ErrorResponse"];
export type RoleResponse = components["schemas"]["RoleResponse"];
export type PermissionResponse = components["schemas"]["PermissionResponse"];
export type UserSummaryResponse = components["schemas"]["UserSummaryResponse"];
export type UserPagedResponse = components["schemas"]["UserPagedResponse"];
export type UserResponse = components["schemas"]["UserResponse"];
export type CurrentUserResponse = components["schemas"]["CurrentUserResponse"];
export type UserPreferencesResponse = components["schemas"]["UserPreferencesResponse"];
export type TokenResponse = components["schemas"]["TokenResponse"];

// ─── Request DTOs ───────────────────────────────────────────────────────────

export type RegisterRequest = components["schemas"]["RegisterUserRequest"];
export type LoginRequest = components["schemas"]["LoginRequest"];
export type ForgotPasswordRequest = components["schemas"]["ForgotPasswordRequest"];
export type ResetPasswordRequest = components["schemas"]["ResetPasswordRequest"];
export type VerifyEmailRequest = components["schemas"]["VerifyEmailRequest"];
export type ResendVerificationCodeRequest = components["schemas"]["ResendVerificationCodeRequest"];
export type UpdateUserPreferencesRequest = components["schemas"]["UpdateUserPreferencesRequest"];

// ─── Misc Response DTOs ─────────────────────────────────────────────────────

export type MessageResponse = components["schemas"]["MessageResponse"];

// ─── Catalog Enums ──────────────────────────────────────────────────────────

export type ItemStatus = components["schemas"]["ItemStatus"];
export type ItemCondition = components["schemas"]["ItemCondition"];

// ─── Catalog Response DTOs ──────────────────────────────────────────────────

export type CategoryResponse = components["schemas"]["CategoryResponse"];
export type PopularCategoryResponse = components["schemas"]["PopularCategoryResponse"];
export type AdminCategoryResponse = components["schemas"]["AdminCategoryResponse"];
export type AdminCategoryPagedResponse = components["schemas"]["AdminCategoryPagedResponse"];
export type AdminListingSummaryResponse = components["schemas"]["AdminListingSummaryResponse"];
export type AdminListingDetailResponse = components["schemas"]["AdminListingDetailResponse"];
export type AdminListingPagedResponse = components["schemas"]["AdminListingPagedResponse"];
export type TagResponse = components["schemas"]["TagResponse"];
export type AdminTagResponse = components["schemas"]["AdminTagResponse"];
export type AdminTagPagedResponse = components["schemas"]["AdminTagPagedResponse"];
export type ItemSummaryResponse = components["schemas"]["ItemSummaryResponse"];
export type ItemDetailResponse = components["schemas"]["ItemDetailResponse"];
export type ItemPagedResponse = components["schemas"]["ItemPagedResponse"];
export type RecommendationReason = components["schemas"]["RecommendationReason"];
export type RecommendationItemResponse = components["schemas"]["RecommendationItemResponse"];
export type RecommendationPagedResponse = components["schemas"]["RecommendationPagedResponse"];
export type SavedSearchCriteria = components["schemas"]["SavedSearchCriteria"];
export type SavedSearchResponse = components["schemas"]["SavedSearchResponse"];
export type SavedSearchPagedResponse = components["schemas"]["SavedSearchPagedResponse"];
export type ListingModerationActionResponse = components["schemas"]["ListingModerationActionResponse"];
export type OwnerListingModerationSummary = components["schemas"]["OwnerListingModerationSummary"];

// ─── Catalog Image Response DTOs ────────────────────────────────────────────

export type ItemImageResponse = components["schemas"]["ItemImageResponse"];

// ─── Catalog Request DTOs ───────────────────────────────────────────────────

export type CreateItemRequest = components["schemas"]["CreateItemRequest"];
export type CreateCategoryRequest = components["schemas"]["CreateCategoryRequest"];
export type CreateTagRequest = components["schemas"]["CreateTagRequest"];
export type UpdateItemRequest = components["schemas"]["UpdateItemRequest"];
export type CreateSavedSearchRequest = components["schemas"]["CreateSavedSearchRequest"];
export type UpdateCategoryRequest = components["schemas"]["UpdateCategoryRequest"];
export type UpdateTagRequest = components["schemas"]["UpdateTagRequest"];
export type ArchiveItemRequest = components["schemas"]["ArchiveItemRequest"];
export type AdminRemoveListingRequest = components["schemas"]["AdminRemoveListingRequest"];
export type AdminRestoreListingRequest = components["schemas"]["AdminRestoreListingRequest"];

// ─── Moderation Enums ────────────────────────────────────────────────────────

export type ListingModerationActionType = components["schemas"]["ListingModerationActionType"];
export type ListingModerationReasonCode = components["schemas"]["ListingModerationReasonCode"];
export type ListingModerationSourceType = components["schemas"]["ListingModerationSourceType"];

// ─── Reporting Enums ─────────────────────────────────────────────────────────

export type ReportReasonCode = components["schemas"]["ReportReasonCode"];
export type ReportStatus = components["schemas"]["ReportStatus"];
export type ReportTargetType = components["schemas"]["ReportTargetType"];

// ─── Reporting Response DTOs ────────────────────────────────────────────────

export type ReportUserSummaryResponse = components["schemas"]["ReportUserSummaryResponse"];
export type ReportTargetSummaryResponse = components["schemas"]["ReportTargetSummaryResponse"];
export type ReportSummaryResponse = components["schemas"]["ReportSummaryResponse"];
export type ReportDetailResponse = components["schemas"]["ReportDetailResponse"];
export type ReportPagedResponse = components["schemas"]["ReportPagedResponse"];
export type AdminReportQueueSummaryResponse = components["schemas"]["AdminReportQueueSummaryResponse"];

// ─── Admin Operations Response DTOs ─────────────────────────────────────────

export type AdminOperationsOverviewResponse = components["schemas"]["AdminOperationsOverviewResponse"];
export type AdminOperationsSystemResponse = components["schemas"]["AdminOperationsSystemResponse"];
export type AdminOperationsHealthResponse = components["schemas"]["AdminOperationsHealthResponse"];
export type AdminOperationsUsersResponse = components["schemas"]["AdminOperationsUsersResponse"];
export type AdminOperationsMarketplaceResponse = components["schemas"]["AdminOperationsMarketplaceResponse"];
export type AdminOperationsModerationResponse = components["schemas"]["AdminOperationsModerationResponse"];
export type AdminOperationsStorageResponse = components["schemas"]["AdminOperationsStorageResponse"];
export type AdminOperationsDeploymentResponse = components["schemas"]["AdminOperationsDeploymentResponse"];

// ─── Reporting Request DTOs ─────────────────────────────────────────────────

export type CreateReportRequest = components["schemas"]["CreateReportRequest"];
export type AdminUpdateReportRequest = components["schemas"]["AdminUpdateReportRequest"];

// ─── Trade Offer Enums ──────────────────────────────────────────────────────

export type TradeOfferStatus = components["schemas"]["TradeOfferStatus"];
export type TradeOfferMode = components["schemas"]["TradeOfferMode"];

// ─── Trade Offer Response DTOs ──────────────────────────────────────────────

export type TradeOfferItemSummary = components["schemas"]["TradeOfferItemSummary"];
export type TradeOfferSummaryResponse = components["schemas"]["TradeOfferSummaryResponse"];
export type TradeOfferResponse = components["schemas"]["TradeOfferResponse"];
export type TradeOfferPagedResponse = components["schemas"]["TradeOfferPagedResponse"];
export type TradeOfferMessageResponse = components["schemas"]["TradeOfferMessageResponse"];

// ─── Reputation / Review DTOs ───────────────────────────────────────────────

export type TradeReviewRating = components["schemas"]["TradeReviewRating"];
export type TradeReviewNegativeReason = components["schemas"]["TradeReviewNegativeReason"];
export type ReviewDirection = components["schemas"]["ReviewDirection"];
export type CreateTradeReviewRequest = components["schemas"]["CreateTradeReviewRequest"];
export type TradeReviewResponse = components["schemas"]["TradeReviewResponse"];
export type UserTradeReviewSummaryResponse = components["schemas"]["UserTradeReviewSummaryResponse"];
export type UserTradeReviewPagedResponse = components["schemas"]["UserTradeReviewPagedResponse"];
export type AdminTradeReviewSummaryResponse = components["schemas"]["AdminTradeReviewSummaryResponse"];
export type AdminTradeReviewPagedResponse = components["schemas"]["AdminTradeReviewPagedResponse"];
export type ReputationSummaryResponse = components["schemas"]["ReputationSummaryResponse"];

// ─── Trade Offer Request DTOs ───────────────────────────────────────────────

export type CreateTradeOfferRequest = components["schemas"]["CreateTradeOfferRequest"];
export type SendTradeOfferMessageRequest = components["schemas"]["SendTradeOfferMessageRequest"];

// ─── Notification Enums ─────────────────────────────────────────────────────

export type NotificationType = components["schemas"]["NotificationType"];

// ─── Notification Response DTOs ─────────────────────────────────────────────

export type NotificationResponse = components["schemas"]["NotificationResponse"];
export type NotificationPagedResponse = components["schemas"]["NotificationPagedResponse"];
export type NotificationUnreadCountResponse = components["schemas"]["NotificationUnreadCountResponse"];

// ─── Profile Response DTOs ──────────────────────────────────────────────────

export type PublicProfileResponse = components["schemas"]["PublicProfileResponse"];
export type PublicProfileReviewSnippetResponse = components["schemas"]["PublicProfileReviewSnippetResponse"];

