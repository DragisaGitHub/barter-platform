import { useQuery } from "@tanstack/react-query";
import { RefreshCw, CheckCircle, XCircle } from "lucide-react";
import { apiClient } from "@/api/axios.ts";
import type { PingResponse } from "@/api/generated/types.ts";
import { Button } from "../../components/ui/Button";
import { Card, CardContent, CardHeader, CardTitle } from "../../components/ui/Card";

export function SystemPage() {

  const { data: ping, isLoading, isError, refetch } = useQuery({
    queryKey: ["ping"],
    queryFn: async () => {
      const response = await apiClient.get<PingResponse>("/ping");
      return response.data;
    },
    retry: 1,
  });

  const isHealthy = !!ping?.message;

  return (
    <div className="max-w-4xl mx-auto">
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-slate-900 dark:text-white">System</h1>
        <p className="text-slate-600 dark:text-slate-400 mt-2">
          Monitor system health and configuration
        </p>
      </div>

      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <CardTitle>API Health Status</CardTitle>
            <Button
              variant="outline"
              size="sm"
              onClick={() => refetch()}
              isLoading={isLoading}
            >
              <RefreshCw className="size-4" />
              Refresh
            </Button>
          </div>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="flex items-center gap-3">
            {isHealthy ? (
              <>
                <CheckCircle className="size-8 text-emerald-600 dark:text-emerald-400" />
                <div>
                  <p className="font-semibold text-slate-900 dark:text-white">
                    System Online
                  </p>
                  <p className="text-sm text-slate-600 dark:text-slate-400">
                    All services are operational
                  </p>
                </div>
              </>
            ) : (
              <>
                <XCircle className="size-8 text-red-600 dark:text-red-400" />
                <div>
                  <p className="font-semibold text-slate-900 dark:text-white">
                    System Status Unknown
                  </p>
                  <p className="text-sm text-slate-600 dark:text-slate-400">
                    {isError
                      ? "Unable to reach backend API"
                      : "Unable to verify system health"}
                  </p>
                </div>
              </>
            )}
          </div>

          {ping && (
            <div className="pt-4 border-t border-slate-200 dark:border-slate-700">
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="text-sm font-medium text-slate-700 dark:text-slate-300">
                    Response
                  </label>
                  <p className="mt-1 text-slate-900 dark:text-slate-100">{ping.message}</p>
                </div>
              </div>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
