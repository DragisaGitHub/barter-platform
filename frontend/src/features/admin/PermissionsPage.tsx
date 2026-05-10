import { useQuery } from "@tanstack/react-query";
import { apiClient } from "../../api/axios";
import type { PermissionResponse } from "../../api/generated/types";
import { Card, CardContent } from "../../components/ui/Card";
import { Badge } from "../../components/ui/Badge";
import { Skeleton } from "../../components/ui/Skeleton";
import { EmptyState } from "../../components/ui/EmptyState";
import { Lock } from "lucide-react";

export function PermissionsPage() {
  const { data: permissions, isLoading } = useQuery({
    queryKey: ["permissions"],
    queryFn: async () => {
      const response = await apiClient.get<PermissionResponse[]>("/permissions");
      return response.data;
    },
  });

  if (isLoading) {
    return (
      <div className="max-w-4xl mx-auto">
        <h1 className="text-3xl font-bold text-slate-900 dark:text-white mb-8">
          Permissions
        </h1>
        <div className="space-y-4">
          {[...Array(5)].map((_, i) => (
            <Skeleton key={i} className="h-20 w-full" />
          ))}
        </div>
      </div>
    );
  }

  if (!permissions || permissions.length === 0) {
    return (
      <div className="max-w-4xl mx-auto">
        <h1 className="text-3xl font-bold text-slate-900 dark:text-white mb-8">
          Permissions
        </h1>
        <EmptyState
          icon={<Lock className="size-16" />}
          title="No permissions found"
          description="There are no permissions configured in the system."
        />
      </div>
    );
  }

  return (
    <div className="max-w-4xl mx-auto">
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-slate-900 dark:text-white">Permissions</h1>
        <p className="text-slate-600 dark:text-slate-400 mt-2">
          System permissions and access controls
        </p>
      </div>

      <div className="space-y-3">
        {permissions.map((permission) => (
          <Card key={permission.code}>
            <CardContent className="pt-6">
              <div className="flex items-center justify-between">
                <div>
                  <h3 className="font-medium text-slate-900 dark:text-white">
                    {permission.name}
                  </h3>
                  <p className="text-sm text-slate-600 dark:text-slate-400 mt-0.5">
                    {permission.code}
                  </p>
                </div>
                <Badge variant="secondary">{permission.code}</Badge>
              </div>
            </CardContent>
          </Card>
        ))}
      </div>
    </div>
  );
}
