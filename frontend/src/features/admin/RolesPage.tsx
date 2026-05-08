import { useQuery } from "@tanstack/react-query";
import { apiClient } from "../../api/axios";
import type { RoleResponse } from "../../api/generated/types";
import { Card, CardContent } from "../../components/ui/Card";
import { Badge } from "../../components/ui/Badge";
import { Skeleton } from "../../components/ui/Skeleton";
import { EmptyState } from "../../components/ui/EmptyState";
import { KeyRound } from "lucide-react";

export function RolesPage() {
  const { data: roles, isLoading } = useQuery({
    queryKey: ["roles"],
    queryFn: async () => {
      const response = await apiClient.get<RoleResponse[]>("/roles");
      return response.data;
    },
  });

  if (isLoading) {
    return (
      <div className="max-w-4xl mx-auto">
        <h1 className="text-3xl font-bold text-slate-900 dark:text-white mb-8">Roles</h1>
        <div className="space-y-4">
          {[...Array(3)].map((_, i) => (
            <Skeleton key={i} className="h-24 w-full" />
          ))}
        </div>
      </div>
    );
  }

  if (!roles || roles.length === 0) {
    return (
      <div className="max-w-4xl mx-auto">
        <h1 className="text-3xl font-bold text-slate-900 dark:text-white mb-8">Roles</h1>
        <EmptyState
          icon={<KeyRound className="size-16" />}
          title="No roles found"
          description="There are no roles configured in the system."
        />
      </div>
    );
  }

  return (
    <div className="max-w-4xl mx-auto">
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-slate-900 dark:text-white">Roles</h1>
        <p className="text-slate-600 dark:text-slate-400 mt-2">
          System roles and their descriptions
        </p>
      </div>

      <div className="space-y-4">
        {roles.map((role) => (
          <Card key={role.code}>
            <CardContent className="pt-6">
              <div className="flex items-center justify-between">
                <div>
                  <h3 className="text-lg font-semibold text-slate-900 dark:text-white">
                    {role.name}
                  </h3>
                  <p className="text-sm text-slate-600 dark:text-slate-400 mt-1">
                    Code: {role.code}
                  </p>
                </div>
                <Badge variant="primary">{role.code}</Badge>
              </div>
            </CardContent>
          </Card>
        ))}
      </div>
    </div>
  );
}
