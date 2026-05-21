import { apiClient } from "./axios";
import type { CreateReportRequest, MessageResponse } from "./generated/types";

export async function createReport(data: CreateReportRequest): Promise<MessageResponse> {
  const response = await apiClient.post<MessageResponse>("/reports", data);
  return response.data;
}

