import { apiClient } from "./axios";
import type { MessageResponse, ResendVerificationCodeRequest, VerifyEmailRequest } from "./generated/types";

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

