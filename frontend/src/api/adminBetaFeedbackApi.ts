import { apiClient } from "./axios";
import type {
  AdminBetaFeedbackPagedResponse,
  AdminBetaFeedbackSummaryResponse,
  AdminUpdateBetaFeedbackStatusRequest,
  BetaFeedbackStatus,
} from "./generated/types";

export interface ListAdminBetaFeedbackParams {
  page?: number;
  size?: number;
  sort?: string;
  status?: BetaFeedbackStatus;
}

export async function listAdminBetaFeedback(
  params: ListAdminBetaFeedbackParams = {},
): Promise<AdminBetaFeedbackPagedResponse> {
  const response = await apiClient.get<AdminBetaFeedbackPagedResponse>("/admin/feedback/beta", { params });
  return response.data;
}

export async function updateAdminBetaFeedbackStatus(
  feedbackUuid: string,
  data: AdminUpdateBetaFeedbackStatusRequest,
): Promise<AdminBetaFeedbackSummaryResponse> {
  const response = await apiClient.patch<AdminBetaFeedbackSummaryResponse>(
    `/admin/feedback/beta/${feedbackUuid}/status`,
    data,
  );
  return response.data;
}

