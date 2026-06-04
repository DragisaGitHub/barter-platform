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
import { useTranslation } from "react-i18next";

export function RolesPage() {
  const { t } = useTranslation("admin");
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
        title={t("roles")}
        description={t("rolesPage.loadingDescription")}
        badges={
          <>
            <Badge variant="primary">{t("rolesPage.badges.authorization")}</Badge>
            <Badge>{t("rolesPage.badges.roleCatalog")}</Badge>
          </>
        }
      >
        <AdminSurface title={t("rolesPage.loadingTitle")} description={t("rolesPage.loadingSurfaceDescription")}>
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
        title={t("roles")}
        description={t("rolesPage.loadingDescription")}
        badges={
          <>
            <Badge variant="primary">{t("rolesPage.badges.authorization")}</Badge>
            <Badge>{t("rolesPage.badges.roleCatalog")}</Badge>
          </>
        }
      >
        <AdminSurface title={t("rolesPage.errorSurfaceTitle")} description={t("rolesPage.errorSurfaceDescription")}>
          <EmptyState
            icon={<KeyRound className="size-16" />}
            title={t("rolesPage.errorEmptyTitle")}
            description={t("rolesPage.errorEmptyDescription")}
            action={
              <Button type="button" variant="outline" onClick={() => refetch()}>
                {t("rolesPage.retry")}
              </Button>
            }
          />
        </AdminSurface>
      </AdminPageShell>
    );
  }

  return (
    <AdminPageShell
      title={t("roles")}
      description={t("rolesPage.description")}
      badges={
        <>
          <Badge variant="primary">{t("rolesPage.badges.authorization")}</Badge>
          <Badge>{t("rolesPage.badges.roles", { count: roles.length })}</Badge>
        </>
      }
    >
      <AdminToolbar>
        <div className="relative w-full max-w-md">
          <Search className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-slate-400" />
          <Input
            value={search}
            onChange={(event) => setSearch(event.target.value)}
            placeholder={t("rolesPage.searchPlaceholder")}
            className="pl-9"
            aria-label={t("rolesPage.searchAriaLabel")}
          />
        </div>

        <div className="text-sm text-slate-600 dark:text-slate-400">
          {t("rolesPage.showingRoles", { shown: filteredRoles.length, total: roles.length })}
        </div>
      </AdminToolbar>

      <AdminSurface
        title={t("rolesPage.definitionsTitle")}
        description={t("rolesPage.definitionsDescription")}
        contentClassName="space-y-4"
      >
        {roles.length === 0 ? (
          <EmptyState
            icon={<KeyRound className="size-16" />}
            title={t("rolesPage.emptyTitle")}
            description={t("rolesPage.emptyDescription")}
          />
        ) : filteredRoles.length === 0 ? (
          <EmptyState
            icon={<Search className="size-14" />}
            title={t("rolesPage.emptyFilteredTitle")}
            description={t("rolesPage.emptyFilteredDescription")}
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
                    {role.description || t("rolesPage.noDescription")}
                  </p>
                </div>

                <div className="grid gap-2 text-sm text-slate-600 dark:text-slate-400 md:text-right">
                  <span>{t("rolesPage.createdDate", { date: new Date(role.createdAt).toLocaleDateString() })}</span>
                  <span>{role.updatedAt ? t("updatedAt", { date: new Date(role.updatedAt).toLocaleDateString() }) : t("noUpdatesRecorded")}</span>
                </div>
              </div>
            </div>
          ))
        )}
      </AdminSurface>
    </AdminPageShell>
  );
}
