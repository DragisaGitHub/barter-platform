import { apiClient } from "./axios";
import type {
  ForgotPasswordRequest,
  MessageResponse,
  ResendVerificationCodeRequest, ResetPasswordRequest,
  VerifyEmailRequest
} from "./generated/types";

export async function verifyEmail(email: string, code: string): Promise<MessageResponse> {
  const body: VerifyEmailRequest = { email, code };
  const response = await apiClient.post<MessageResponse>("/auth/verify-email", body);
  return response.data;
}

export async function resendVerificationCode(email: string): Promise<MessageResponse> {
  const body: ResendVerificationCodeRequest = { email };
  const response = await apiClient.post<MessageResponse>("/auth/resend-verification-code", body);
  return response.data;
}

export async function forgotPassword(email: string): Promise<MessageResponse> {
  const body: ForgotPasswordRequest = { email };
  const response = await apiClient.post<MessageResponse>("/auth/forgot-password", body);
  return response.data;
}

export async function resetPassword(
  email: string,
  token: string,
  newPassword: string
): Promise<MessageResponse> {
  const body: ResetPasswordRequest = { email, token, newPassword };
  const response = await apiClient.post<MessageResponse>("/auth/reset-password", body);
  return response.data;
}

