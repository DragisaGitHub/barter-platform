import { keepPreviousData, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  getAdminReportByUuid,
  listAdminReports,
  updateAdminReport,
  type ListAdminReportsParams,
} from "@/api/adminReportsApi";
import type {
  AdminUpdateReportRequest,
  ReportDetailResponse,
  ReportPagedResponse,
} from "@/api/generated/types";

export const adminReportKeys = {
  all: ["admin", "reports"] as const,
  lists: () => ["admin", "reports", "list"] as const,
  list: (params: ListAdminReportsParams) => ["admin", "reports", "list", params] as const,
  detail: (reportUuid: string) => ["admin", "reports", "detail", reportUuid] as const,
};

export function useAdminReports(params: ListAdminReportsParams = {}, enabled = true) {
  return useQuery<ReportPagedResponse>({
    queryKey: adminReportKeys.list(params),
    queryFn: () => listAdminReports(params),
    enabled,
    placeholderData: keepPreviousData,
  });
}

export function useAdminReport(reportUuid: string, enabled = true) {
  return useQuery<ReportDetailResponse>({
    queryKey: adminReportKeys.detail(reportUuid),
    queryFn: () => getAdminReportByUuid(reportUuid),
    enabled: enabled && !!reportUuid,
  });
}

export function useUpdateAdminReport() {
  const queryClient = useQueryClient();

  return useMutation<
    ReportDetailResponse,
    unknown,
    { reportUuid: string; data: AdminUpdateReportRequest }
  >({
    mutationFn: ({ reportUuid, data }) => updateAdminReport(reportUuid, data),
    onSuccess: (report) => {
      queryClient.setQueryData(adminReportKeys.detail(report.uuid), report);
      queryClient.invalidateQueries({ queryKey: adminReportKeys.all });
    },
  });
}

