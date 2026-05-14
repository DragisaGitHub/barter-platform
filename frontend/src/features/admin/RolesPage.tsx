import { useMemo, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { apiClient } from "@/api/axios.ts";
import type { RoleResponse } from "@/api/generated/types.ts";
import { Button } from "../../components/ui/Button";
import { Badge } from "../../components/ui/Badge";
import { Input } from "../../components/ui/Input";
import { Skeleton } from "../../components/ui/Skeleton";
import { EmptyState } from "../../components/ui/EmptyState";
import { KeyRound, Search } from "lucide-react";
import { AdminPageShell, AdminSurface, AdminToolbar } from "./components/AdminPageShell";

export function RolesPage() {
  const [search, setSearch] = useState("");
  const { data: roles, isLoading, isError, refetch } = useQuery({
    queryKey: ["roles"],
    queryFn: async () => {
      const response = await apiClient.get<RoleResponse[]>("/roles");
      return response.data;
    },
  });

  const filteredRoles = useMemo(() => {
    const normalizedSearch = search.trim().toLowerCase();
    const sortedRoles = [...(roles ?? [])].sort((a, b) => a.name.localeCompare(b.name));

    if (!normalizedSearch) {
      return sortedRoles;
    }

    return sortedRoles.filter((role) =>
      [role.name, role.code, role.description ?? ""].some((value) =>
        value.toLowerCase().includes(normalizedSearch)
      )
    );
  }, [roles, search]);

  if (isLoading) {
    return (
      <AdminPageShell
        title="Roles"
        description="Review the platform roles currently defined by the backend authorization model."
        badges={
          <>
            <Badge variant="primary">Authorization</Badge>
            <Badge>Role catalog</Badge>
          </>
        }
      >
        <AdminSurface title="Loading roles" description="Fetching the current role catalog.">
          <div className="space-y-4">
            {[...Array(4)].map((_, i) => (
              <Skeleton key={i} className="h-28 w-full" />
            ))}
          </div>
        </AdminSurface>
      </AdminPageShell>
    );
  }

  if (isError || !roles) {
    return (
      <AdminPageShell
        title="Roles"
        description="Review the platform roles currently defined by the backend authorization model."
        badges={
          <>
            <Badge variant="primary">Authorization</Badge>
            <Badge>Role catalog</Badge>
          </>
        }
      >
        <AdminSurface title="Unable to load roles" description="The current role catalog request failed.">
          <EmptyState
            icon={<KeyRound className="size-16" />}
            title="Roles unavailable"
            description="Retry to load the role definitions from the existing backend endpoint."
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
      title="Roles"
      description="Inspect role definitions and their descriptive scope without introducing unsupported editing or assignment flows."
      badges={
        <>
          <Badge variant="primary">Authorization</Badge>
          <Badge>{roles.length} roles</Badge>
        </>
      }
    >
      <AdminToolbar>
        <div className="relative w-full max-w-md">
          <Search className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-slate-400" />
          <Input
            value={search}
            onChange={(event) => setSearch(event.target.value)}
            placeholder="Search roles by name, code, or description"
            className="pl-9"
            aria-label="Search roles"
          />
        </div>

        <div className="text-sm text-slate-600 dark:text-slate-400">
          Showing {filteredRoles.length} of {roles.length} roles
        </div>
      </AdminToolbar>

      <AdminSurface
        title="Role definitions"
        description="Roles are displayed from the current API response and filtered locally for fast admin review."
        contentClassName="space-y-4"
      >
        {roles.length === 0 ? (
          <EmptyState
            icon={<KeyRound className="size-16" />}
            title="No roles found"
            description="There are no roles configured in the system."
          />
        ) : filteredRoles.length === 0 ? (
          <EmptyState
            icon={<Search className="size-14" />}
            title="No roles match this filter"
            description="Try a different search term to review the available role definitions."
          />
        ) : (
          filteredRoles.map((role) => (
            <div
              key={role.code}
              className="rounded-2xl border border-slate-200 bg-slate-50 p-5 dark:border-slate-800 dark:bg-slate-950"
            >
              <div className="flex flex-col gap-4 md:flex-row md:items-start md:justify-between">
                <div className="min-w-0">
                  <div className="flex flex-wrap items-center gap-2">
                    <h3 className="text-lg font-semibold text-slate-900 dark:text-white">{role.name}</h3>
                    <Badge variant="primary">{role.code}</Badge>
                  </div>
                  <p className="mt-2 text-sm text-slate-600 dark:text-slate-400">
                    {role.description || "No role description is currently provided by the backend contract."}
                  </p>
                </div>

                <div className="grid gap-2 text-sm text-slate-600 dark:text-slate-400 md:text-right">
                  <span>Created {new Date(role.createdAt).toLocaleDateString()}</span>
                  <span>{role.updatedAt ? `Updated ${new Date(role.updatedAt).toLocaleDateString()}` : "No updates recorded"}</span>
                </div>
              </div>
            </div>
          ))
        )}
      </AdminSurface>
    </AdminPageShell>
  );
}
