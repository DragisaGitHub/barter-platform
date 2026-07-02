import { apiClient } from "./axios";
import type {
  AdminOperationsBackupsResponse,
  AdminOperationsCostsResponse,
  AdminOperationsDeploymentsResponse,
  AdminOperationsMonitoringResponse,
  AdminOperationsOverviewResponse,
  AdminOperationsSecurityResponse,
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

export async function getAdminOperationsCosts(): Promise<AdminOperationsCostsResponse> {
  const response = await apiClient.get<AdminOperationsCostsResponse>("/admin/operations/costs");
  return response.data;
}

export async function getAdminOperationsMonitoring(): Promise<AdminOperationsMonitoringResponse> {
  const response = await apiClient.get<AdminOperationsMonitoringResponse>("/admin/operations/monitoring");
  return response.data;
}

export async function getAdminOperationsSecurity(): Promise<AdminOperationsSecurityResponse> {
  const response = await apiClient.get<AdminOperationsSecurityResponse>("/admin/operations/security");
  return response.data;
}

