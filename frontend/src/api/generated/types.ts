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

// ─── Response DTOs ──────────────────────────────────────────────────────────

export type PingResponse = components["schemas"]["PingResponse"];
export type ErrorResponse = components["schemas"]["ErrorResponse"];
export type RoleResponse = components["schemas"]["RoleResponse"];
export type PermissionResponse = components["schemas"]["PermissionResponse"];
export type UserSummaryResponse = components["schemas"]["UserSummaryResponse"];
export type UserPagedResponse = components["schemas"]["UserPagedResponse"];
export type UserResponse = components["schemas"]["UserResponse"];
export type CurrentUserResponse = components["schemas"]["CurrentUserResponse"];
export type TokenResponse = components["schemas"]["TokenResponse"];

// ─── Request DTOs ───────────────────────────────────────────────────────────

export type RegisterRequest = components["schemas"]["RegisterUserRequest"];
export type LoginRequest = components["schemas"]["LoginRequest"];
export type VerifyEmailRequest = components["schemas"]["VerifyEmailRequest"];
export type ResendVerificationCodeRequest = components["schemas"]["ResendVerificationCodeRequest"];

// ─── Misc Response DTOs ─────────────────────────────────────────────────────

export type MessageResponse = components["schemas"]["MessageResponse"];

// ─── Catalog Enums ──────────────────────────────────────────────────────────

export type ItemStatus = components["schemas"]["ItemStatus"];
export type ItemCondition = components["schemas"]["ItemCondition"];

// ─── Catalog Response DTOs ──────────────────────────────────────────────────

export type CategoryResponse = components["schemas"]["CategoryResponse"];
export type TagResponse = components["schemas"]["TagResponse"];
export type ItemSummaryResponse = components["schemas"]["ItemSummaryResponse"];
export type ItemDetailResponse = components["schemas"]["ItemDetailResponse"];
export type ItemPagedResponse = components["schemas"]["ItemPagedResponse"];

// ─── Catalog Image Response DTOs ────────────────────────────────────────────

export type ItemImageResponse = components["schemas"]["ItemImageResponse"];

// ─── Catalog Request DTOs ───────────────────────────────────────────────────

export type CreateItemRequest = components["schemas"]["CreateItemRequest"];
export type UpdateItemRequest = components["schemas"]["UpdateItemRequest"];
export type ArchiveItemRequest = components["schemas"]["ArchiveItemRequest"];

// ─── Trade Offer Enums ──────────────────────────────────────────────────────

export type TradeOfferStatus = components["schemas"]["TradeOfferStatus"];
export type TradeOfferMode = components["schemas"]["TradeOfferMode"];

// ─── Trade Offer Response DTOs ──────────────────────────────────────────────

export type TradeOfferItemSummary = components["schemas"]["TradeOfferItemSummary"];
export type TradeOfferSummaryResponse = components["schemas"]["TradeOfferSummaryResponse"];
export type TradeOfferResponse = components["schemas"]["TradeOfferResponse"];
export type TradeOfferPagedResponse = components["schemas"]["TradeOfferPagedResponse"];

// ─── Trade Offer Request DTOs ───────────────────────────────────────────────

export type CreateTradeOfferRequest = components["schemas"]["CreateTradeOfferRequest"];

// ─── Notification Enums ─────────────────────────────────────────────────────

export type NotificationType = components["schemas"]["NotificationType"];

// ─── Notification Response DTOs ─────────────────────────────────────────────

export type NotificationResponse = components["schemas"]["NotificationResponse"];
export type NotificationPagedResponse = components["schemas"]["NotificationPagedResponse"];
export type NotificationUnreadCountResponse = components["schemas"]["NotificationUnreadCountResponse"];
