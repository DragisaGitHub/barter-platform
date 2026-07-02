import { useQuery } from "@tanstack/react-query";
import type {
  AdminOperationsBackupsResponse,
  AdminOperationsCostsResponse,
  AdminOperationsDeploymentsResponse,
  AdminOperationsOverviewResponse,
} from "@/api/generated/types";
import {
  getAdminOperationsBackups,
  getAdminOperationsCosts,
  getAdminOperationsDeployments,
  getAdminOperationsOverview,
} from "@/api/adminOperationsApi";

export const adminOperationsKeys = {
  overview: () => ["admin", "operations", "overview"] as const,
  backups: () => ["admin", "operations", "backups"] as const,
  deployments: () => ["admin", "operations", "deployments"] as const,
  costs: () => ["admin", "operations", "costs"] as const,
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

export function useAdminOperationsCosts() {
  return useQuery<AdminOperationsCostsResponse>({
    queryKey: adminOperationsKeys.costs(),
    queryFn: getAdminOperationsCosts,
    retry: 1,
    // Results are cached server-side for 15 min; staleTime prevents unnecessary re-fetches.
    staleTime: 10 * 60 * 1000,
  });
}

