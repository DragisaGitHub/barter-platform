import { useMemo, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { apiClient } from "@/api/axios.ts";
import type { PermissionResponse } from "@/api/generated/types.ts";
import { Button } from "../../components/ui/Button";
import { Badge } from "../../components/ui/Badge";
import { Input } from "../../components/ui/Input";
import { Skeleton } from "../../components/ui/Skeleton";
import { EmptyState } from "../../components/ui/EmptyState";
import { Lock, Search } from "lucide-react";
import { AdminPageShell, AdminSurface, AdminToolbar } from "./components/AdminPageShell";

export function PermissionsPage() {
  const [search, setSearch] = useState("");
  const { data: permissions, isLoading, isError, refetch } = useQuery({
    queryKey: ["permissions"],
    queryFn: async () => {
      const response = await apiClient.get<PermissionResponse[]>("/permissions");
      return response.data;
    },
  });

  const filteredPermissions = useMemo(() => {
    const normalizedSearch = search.trim().toLowerCase();
    const sortedPermissions = [...(permissions ?? [])].sort((a, b) => a.name.localeCompare(b.name));

    if (!normalizedSearch) {
      return sortedPermissions;
    }

    return sortedPermissions.filter((permission) =>
      [permission.name, permission.code, permission.description ?? ""].some((value) =>
        value.toLowerCase().includes(normalizedSearch)
      )
    );
  }, [permissions, search]);

  if (isLoading) {
    return (
      <AdminPageShell
        title="Permissions"
        description="Audit the permission catalog currently exposed by the authorization backend."
        badges={
          <>
            <Badge variant="primary">Authorization</Badge>
            <Badge>Permission catalog</Badge>
          </>
        }
      >
        <AdminSurface title="Loading permissions" description="Fetching permission definitions.">
          <div className="space-y-4">
            {[...Array(5)].map((_, i) => (
              <Skeleton key={i} className="h-24 w-full" />
            ))}
          </div>
        </AdminSurface>
      </AdminPageShell>
    );
  }

  if (isError || !permissions) {
    return (
      <AdminPageShell
        title="Permissions"
        description="Audit the permission catalog currently exposed by the authorization backend."
        badges={
          <>
            <Badge variant="primary">Authorization</Badge>
            <Badge>Permission catalog</Badge>
          </>
        }
      >
        <AdminSurface title="Unable to load permissions" description="The permission catalog request failed.">
          <EmptyState
            icon={<Lock className="size-16" />}
            title="Permissions unavailable"
            description="Retry to load permission definitions from the existing backend endpoint."
            action={
              <Button type="button" variant="outline" onClick={() => refetch()}>
                Retry
              </Button>
            }
          />
        </AdminSurface>
      </AdminPageShell>
    );
  }

  return (
    <AdminPageShell
      title="Permissions"
      description="Review permission coverage and naming in a dedicated admin catalog without faking unsupported edit actions."
      badges={
        <>
          <Badge variant="primary">Authorization</Badge>
          <Badge>{permissions.length} permissions</Badge>
        </>
      }
    >
      <AdminToolbar>
        <div className="relative w-full max-w-md">
          <Search className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-slate-400" />
          <Input
            value={search}
            onChange={(event) => setSearch(event.target.value)}
            placeholder="Search permissions by name, code, or description"
            className="pl-9"
            aria-label="Search permissions"
          />
        </div>

        <div className="text-sm text-slate-600 dark:text-slate-400">
          Showing {filteredPermissions.length} of {permissions.length} permissions
        </div>
      </AdminToolbar>

      <AdminSurface
        title="Permission catalog"
        description="Permissions are rendered from the current API response and can be filtered locally for quicker review."
        contentClassName="grid gap-4 md:grid-cols-2"
      >
        {permissions.length === 0 ? (
          <div className="md:col-span-2">
            <EmptyState
              icon={<Lock className="size-16" />}
              title="No permissions found"
              description="There are no permissions configured in the system."
            />
          </div>
        ) : filteredPermissions.length === 0 ? (
          <div className="md:col-span-2">
            <EmptyState
              icon={<Search className="size-14" />}
              title="No permissions match this filter"
              description="Try a different search term to continue reviewing the permission catalog."
            />
          </div>
        ) : (
          filteredPermissions.map((permission) => (
            <div
              key={permission.code}
              className="rounded-2xl border border-slate-200 bg-slate-50 p-5 dark:border-slate-800 dark:bg-slate-950"
            >
              <div className="flex items-start justify-between gap-3">
                <div className="min-w-0">
                  <div className="flex flex-wrap items-center gap-2">
                    <h3 className="font-semibold text-slate-900 dark:text-white">{permission.name}</h3>
                    <Badge variant="secondary">{permission.code}</Badge>
                  </div>
                  <p className="mt-2 text-sm text-slate-600 dark:text-slate-400">
                    {permission.description || "No permission description is currently provided by the backend contract."}
                  </p>
                </div>
              </div>

              <div className="mt-4 flex flex-wrap gap-2 text-xs text-slate-500 dark:text-slate-400">
                <span className="rounded-full bg-white px-3 py-1 dark:bg-slate-900">
                  Created {new Date(permission.createdAt).toLocaleDateString()}
                </span>
                <span className="rounded-full bg-white px-3 py-1 dark:bg-slate-900">
                  {permission.updatedAt
                    ? `Updated ${new Date(permission.updatedAt).toLocaleDateString()}`
                    : "No updates recorded"}
                </span>
              </div>
            </div>
          ))
        )}
      </AdminSurface>
    </AdminPageShell>
  );
}
