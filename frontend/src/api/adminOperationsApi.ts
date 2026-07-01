import { apiClient } from "./axios";
import type {
  AdminOperationsBackupsResponse,
  AdminOperationsDeploymentsResponse,
  AdminOperationsOverviewResponse,
} from "./generated/types";

export async function getAdminOperationsOverview(): Promise<AdminOperationsOverviewResponse> {
  const response = await apiClient.get<AdminOperationsOverviewResponse>("/admin/operations/overview");
  return response.data;
}

export async function getAdminOperationsBackups(): Promise<AdminOperationsBackupsResponse> {
  const response = await apiClient.get<AdminOperationsBackupsResponse>("/admin/operations/backups");
  return response.data;
}

export async function getAdminOperationsDeployments(): Promise<AdminOperationsDeploymentsResponse> {
  const response = await apiClient.get<AdminOperationsDeploymentsResponse>("/admin/operations/deployments");
  return response.data;
}
