import { apiClient } from "./axios";
import type { AdminOperationsOverviewResponse } from "./generated/types";

export async function getAdminOperationsOverview(): Promise<AdminOperationsOverviewResponse> {
  const response = await apiClient.get<AdminOperationsOverviewResponse>("/admin/operations/overview");
  return response.data;
}

