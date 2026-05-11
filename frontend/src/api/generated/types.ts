/**
 * Re-exports from the OpenAPI-generated schema for convenient usage.
 *
 * These types are derived from the backend contract at:
 *   backend/barter-api/src/main/resources/openapi/openapi.yaml
 *
 * Regenerate with: yarn generate:api
 */
import type { components } from "./schema";

// ─── Enums ──────────────────────────────────────────────────────────────────

export type UserStatus = components["schemas"]["UserStatus"];
export type RoleCode = components["schemas"]["RoleCode"];
export type PermissionCode = components["schemas"]["PermissionCode"];
export type OAuthProvider = components["schemas"]["OAuthProvider"];

// ─── Response DTOs ──────────────────────────────────────────────────────────

export type PingResponse = components["schemas"]["PingResponse"];
export type ErrorResponse = components["schemas"]["ErrorResponse"];
export type FieldErrorResponse = components["schemas"]["FieldErrorResponse"];
export type RoleResponse = components["schemas"]["RoleResponse"];
export type PermissionResponse = components["schemas"]["PermissionResponse"];
export type UserSummaryResponse = components["schemas"]["UserSummaryResponse"];
export type UserPagedResponse = components["schemas"]["UserPagedResponse"];
export type UserResponse = components["schemas"]["UserResponse"];
export type CurrentUserResponse = components["schemas"]["CurrentUserResponse"];
export type TokenResponse = components["schemas"]["TokenResponse"];
export type OAuthProviderResponse = components["schemas"]["OAuthProviderResponse"];
export type OAuthAccountResponse = components["schemas"]["OAuthAccountResponse"];
export type MfaSettingsResponse = components["schemas"]["MfaSettingsResponse"];
export type MfaSetupResponse = components["schemas"]["MfaSetupResponse"];
export type MfaRecoveryCodeResponse = components["schemas"]["MfaRecoveryCodeResponse"];
export type PageMetadata = components["schemas"]["PageMetadata"];

// ─── Request DTOs ───────────────────────────────────────────────────────────

export type RegisterRequest = components["schemas"]["RegisterUserRequest"];
export type LoginRequest = components["schemas"]["LoginRequest"];
export type RefreshRequest = components["schemas"]["RefreshTokenRequest"];
export type LogoutRequest = components["schemas"]["RefreshTokenRequest"];
export type UpdateStatusRequest = components["schemas"]["UpdateUserStatusRequest"];
export type MfaVerifyRequest = components["schemas"]["MfaVerifyRequest"];
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

// ─── Catalog Request DTOs ───────────────────────────────────────────────────

export type CreateItemRequest = components["schemas"]["CreateItemRequest"];
export type UpdateItemRequest = components["schemas"]["UpdateItemRequest"];
export type ArchiveItemRequest = components["schemas"]["ArchiveItemRequest"];

// ─── Trade Offer Enums ──────────────────────────────────────────────────────

export type TradeOfferStatus = components["schemas"]["TradeOfferStatus"];
export type TradeOfferMode = components["schemas"]["TradeOfferMode"];

// ─── Trade Offer Response DTOs ──────────────────────────────────────────────

export type TradeOfferUserSummary = components["schemas"]["TradeOfferUserSummary"];
export type TradeOfferItemSummary = components["schemas"]["TradeOfferItemSummary"];
export type TradeOfferSummaryResponse = components["schemas"]["TradeOfferSummaryResponse"];
export type TradeOfferResponse = components["schemas"]["TradeOfferResponse"];
export type TradeOfferPagedResponse = components["schemas"]["TradeOfferPagedResponse"];

// ─── Trade Offer Request DTOs ───────────────────────────────────────────────

export type CreateTradeOfferRequest = components["schemas"]["CreateTradeOfferRequest"];
