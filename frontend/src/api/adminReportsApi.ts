import { apiClient } from "./axios";
import type {
  AdminReportQueueSummaryResponse,
  AdminUpdateReportRequest,
  ReportDetailResponse,
  ReportPagedResponse,
  ReportReasonCode,
  ReportStatus,
  ReportTargetType,
} from "./generated/types";

export interface ListAdminReportsParams {
  page?: number;
  size?: number;
  sort?: string;
  status?: ReportStatus;
  targetType?: ReportTargetType;
  reasonCode?: ReportReasonCode;
}

export async function getAdminReportQueueSummary(): Promise<AdminReportQueueSummaryResponse> {
  const response = await apiClient.get<AdminReportQueueSummaryResponse>("/admin/reports/summary");
  return response.data;
}

export async function listAdminReports(
  params: ListAdminReportsParams = {},
): Promise<ReportPagedResponse> {
  const response = await apiClient.get<ReportPagedResponse>("/admin/reports", { params });
  return response.data;
}

export async function getAdminReportByUuid(reportUuid: string): Promise<ReportDetailResponse> {
  const response = await apiClient.get<ReportDetailResponse>(`/admin/reports/${reportUuid}`);
  return response.data;
}

export async function updateAdminReport(
  reportUuid: string,
  data: AdminUpdateReportRequest,
): Promise<ReportDetailResponse> {
  const response = await apiClient.patch<ReportDetailResponse>(
    `/admin/reports/${reportUuid}/status`,
    data,
  );
  return response.data;
}

