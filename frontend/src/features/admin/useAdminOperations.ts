import { useQuery } from "@tanstack/react-query";
import type {
  AdminOperationsBackupsResponse,
  AdminOperationsDeploymentsResponse,
  AdminOperationsOverviewResponse,
} from "@/api/generated/types";
import {
  getAdminOperationsBackups,
  getAdminOperationsDeployments,
  getAdminOperationsOverview,
} from "@/api/adminOperationsApi";

export const adminOperationsKeys = {
  overview: () => ["admin", "operations", "overview"] as const,
  backups: () => ["admin", "operations", "backups"] as const,
  deployments: () => ["admin", "operations", "deployments"] as const,
};

export function useAdminOperationsOverview() {
  return useQuery<AdminOperationsOverviewResponse>({
    queryKey: adminOperationsKeys.overview(),
    queryFn: getAdminOperationsOverview,
    retry: 1,
  });
}

export function useAdminOperationsBackups() {
  return useQuery<AdminOperationsBackupsResponse>({
    queryKey: adminOperationsKeys.backups(),
    queryFn: getAdminOperationsBackups,
    retry: 1,
  });
}

export function useAdminOperationsDeployments() {
  return useQuery<AdminOperationsDeploymentsResponse>({
    queryKey: adminOperationsKeys.deployments(),
    queryFn: getAdminOperationsDeployments,
    retry: 1,
  });
}
