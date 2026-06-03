import { apiClient } from "./axios";
import type { BetaFeedbackRequest, MessageResponse } from "./generated/types";

export async function submitBetaFeedback(data: BetaFeedbackRequest): Promise<MessageResponse> {
  const response = await apiClient.post<MessageResponse>("/feedback/beta", data);
  return response.data;
}

