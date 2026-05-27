import { useQuery } from "@tanstack/react-query";
import type { AdminOperationsOverviewResponse } from "@/api/generated/types";
import { getAdminOperationsOverview } from "@/api/adminOperationsApi";

export const adminOperationsKeys = {
  overview: () => ["admin", "operations", "overview"] as const,
};

export function useAdminOperationsOverview() {
  return useQuery<AdminOperationsOverviewResponse>({
    queryKey: adminOperationsKeys.overview(),
    queryFn: getAdminOperationsOverview,
    retry: 1,
  });
}

