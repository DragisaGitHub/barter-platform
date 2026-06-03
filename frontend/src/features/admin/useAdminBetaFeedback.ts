import { keepPreviousData, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  listAdminBetaFeedback,
  updateAdminBetaFeedbackStatus,
  type ListAdminBetaFeedbackParams,
} from "@/api/adminBetaFeedbackApi";
import type {
  AdminBetaFeedbackPagedResponse,
  AdminBetaFeedbackSummaryResponse,
  AdminUpdateBetaFeedbackStatusRequest,
} from "@/api/generated/types";

export const adminBetaFeedbackKeys = {
  all: ["admin", "beta-feedback"] as const,
  lists: () => ["admin", "beta-feedback", "list"] as const,
  list: (params: ListAdminBetaFeedbackParams) => ["admin", "beta-feedback", "list", params] as const,
};

export function useAdminBetaFeedback(params: ListAdminBetaFeedbackParams = {}, enabled = true) {
  return useQuery<AdminBetaFeedbackPagedResponse>({
    queryKey: adminBetaFeedbackKeys.list(params),
    queryFn: () => listAdminBetaFeedback(params),
    enabled,
    placeholderData: keepPreviousData,
  });
}

export function useUpdateAdminBetaFeedbackStatus() {
  const queryClient = useQueryClient();

  return useMutation<
    AdminBetaFeedbackSummaryResponse,
    unknown,
    { feedbackUuid: string; data: AdminUpdateBetaFeedbackStatusRequest }
  >({
    mutationFn: ({ feedbackUuid, data }) => updateAdminBetaFeedbackStatus(feedbackUuid, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: adminBetaFeedbackKeys.all });
    },
  });
}

