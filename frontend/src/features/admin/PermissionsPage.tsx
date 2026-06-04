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
import { useTranslation } from "react-i18next";

export function PermissionsPage() {
  const { t } = useTranslation("admin");
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
        title={t("permissions")}
        description={t("permissionsPage.loadingDescription")}
        badges={
          <>
            <Badge variant="primary">{t("permissionsPage.badges.authorization")}</Badge>
            <Badge>{t("permissionsPage.badges.permissionCatalog")}</Badge>
          </>
        }
      >
        <AdminSurface title={t("permissionsPage.loadingTitle")} description={t("permissionsPage.loadingSurfaceDescription")}>
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
        title={t("permissions")}
        description={t("permissionsPage.loadingDescription")}
        badges={
          <>
            <Badge variant="primary">{t("permissionsPage.badges.authorization")}</Badge>
            <Badge>{t("permissionsPage.badges.permissionCatalog")}</Badge>
          </>
        }
      >
        <AdminSurface title={t("permissionsPage.errorSurfaceTitle")} description={t("permissionsPage.errorSurfaceDescription")}>
          <EmptyState
            icon={<Lock className="size-16" />}
            title={t("permissionsPage.errorEmptyTitle")}
            description={t("permissionsPage.errorEmptyDescription")}
            action={
              <Button type="button" variant="outline" onClick={() => refetch()}>
                {t("permissionsPage.retry")}
              </Button>
            }
          />
        </AdminSurface>
      </AdminPageShell>
    );
  }

  return (
    <AdminPageShell
      title={t("permissions")}
      description={t("permissionsPage.description")}
      badges={
        <>
          <Badge variant="primary">{t("permissionsPage.badges.authorization")}</Badge>
          <Badge>{t("permissionsPage.badges.permissions", { count: permissions.length })}</Badge>
        </>
      }
    >
      <AdminToolbar>
        <div className="relative w-full max-w-md">
          <Search className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-slate-400" />
          <Input
            value={search}
            onChange={(event) => setSearch(event.target.value)}
            placeholder={t("permissionsPage.searchPlaceholder")}
            className="pl-9"
            aria-label={t("permissionsPage.searchAriaLabel")}
          />
        </div>

        <div className="text-sm text-slate-600 dark:text-slate-400">
          {t("permissionsPage.showingPermissions", { shown: filteredPermissions.length, total: permissions.length })}
        </div>
      </AdminToolbar>

      <AdminSurface
        title={t("permissionsPage.catalogTitle")}
        description={t("permissionsPage.catalogDescription")}
        contentClassName="grid gap-4 md:grid-cols-2"
      >
        {permissions.length === 0 ? (
          <div className="md:col-span-2">
            <EmptyState
              icon={<Lock className="size-16" />}
              title={t("permissionsPage.emptyTitle")}
              description={t("permissionsPage.emptyDescription")}
            />
          </div>
        ) : filteredPermissions.length === 0 ? (
          <div className="md:col-span-2">
            <EmptyState
              icon={<Search className="size-14" />}
              title={t("permissionsPage.emptyFilteredTitle")}
              description={t("permissionsPage.emptyFilteredDescription")}
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
                    {permission.description || t("permissionsPage.noDescription")}
                  </p>
                </div>
              </div>

              <div className="mt-4 flex flex-wrap gap-2 text-xs text-slate-500 dark:text-slate-400">
                <span className="rounded-full bg-white px-3 py-1 dark:bg-slate-900">
                  {t("permissionsPage.createdDate", { date: new Date(permission.createdAt).toLocaleDateString() })}
                </span>
                <span className="rounded-full bg-white px-3 py-1 dark:bg-slate-900">
                  {permission.updatedAt
                    ? t("updatedAt", { date: new Date(permission.updatedAt).toLocaleDateString() })
                    : t("noUpdatesRecorded")}
                </span>
              </div>
            </div>
          ))
        )}
      </AdminSurface>
    </AdminPageShell>
  );
}
