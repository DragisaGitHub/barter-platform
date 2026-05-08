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
