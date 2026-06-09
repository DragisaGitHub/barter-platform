import { useMemo, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { useNavigate } from "react-router-dom";
import { apiClient } from "@/api/axios.ts";
import type { UserPagedResponse, UserSummaryResponse } from "@/api/generated/types.ts";
import { DataTable } from "../../components/data/DataTable";
import { Pagination } from "../../components/data/Pagination";
import { Button } from "../../components/ui/Button";
import { Badge } from "../../components/ui/Badge";
import { Input } from "../../components/ui/Input";
import { StatusBadge } from "../../components/ui/StatusBadge";
import { Skeleton } from "../../components/ui/Skeleton";
import { EmptyState } from "../../components/ui/EmptyState";
import { Search, ShieldCheck, Users, X } from "lucide-react";
import { AdminPageShell, AdminSurface, AdminToolbar } from "./components/AdminPageShell";
import { useTranslation } from "react-i18next";

export function UsersListPage() {
  const { t } = useTranslation(["admin", "common"]);
  const navigate = useNavigate();
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState("");
  const [sort, setSort] = useState<{ field: string; direction: "asc" | "desc" } | null>({
    field: "createdAt",
    direction: "desc",
  });

  const { data, isLoading, isError, refetch } = useQuery({
    queryKey: ["users", page, sort],
    queryFn: async () => {
      const sortParam = sort ? `${sort.field},${sort.direction}` : undefined;
      const response = await apiClient.get<UserPagedResponse>("/users", {
        params: {
          page,
          size: 20,
          sort: sortParam,
        },
      });
      return response.data;
    },
  });

  const handleSort = (field: string) => {
    setSort((prev) => {
      if (prev?.field === field) {
        return { field, direction: prev.direction === "asc" ? "desc" : "asc" };
      }
      return { field, direction: "asc" };
    });
  };

  const handleRowClick = (user: UserSummaryResponse) => {
    navigate(`/admin/users/${user.uuid}`);
  };

  const clearSearch = () => {
    setSearch("");
  };

  const content = data?.content ?? [];
  const normalizedSearch = search.trim().toLowerCase();
  const filteredUsers = useMemo(
    () =>
      content.filter((user) => {
        if (!normalizedSearch) {
          return true;
        }

        return [
          user.username,
          user.email,
          user.status,
          user.emailVerified ? "email verified" : "email pending",
          user.mfaEnabled ? "mfa enabled" : "mfa disabled",
        ].some((value) => value.toLowerCase().includes(normalizedSearch));
      }),
    [content, normalizedSearch]
  );

  const totalUsers = data?.totalElements ?? 0;

  if (isLoading) {
    return (
      <AdminPageShell
        title={t("users")}
        description={t("usersPage.loadingDescription")}
        badges={
          <>
            <Badge variant="primary">{t("usersPage.badges.accessReview")}</Badge>
            <Badge>{t("usersPage.badges.userDirectory")}</Badge>
          </>
        }
      >
        <AdminSurface title={t("usersPage.loadingTitle")} description={t("usersPage.loadingSurfaceDescription")}>
          <div className="space-y-3">
            {[...Array(6)].map((_, i) => (
              <Skeleton key={i} className="h-16 w-full" />
            ))}
          </div>
        </AdminSurface>
      </AdminPageShell>
    );
  }

  if (isError || !data) {
    return (
      <AdminPageShell
        title={t("users")}
        description={t("usersPage.loadingDescription")}
        badges={
          <>
            <Badge variant="primary">{t("usersPage.badges.accessReview")}</Badge>
            <Badge>{t("usersPage.badges.userDirectory")}</Badge>
          </>
        }
      >
        <AdminSurface title={t("usersPage.errorSurfaceTitle")} description={t("usersPage.errorSurfaceDescription")}>
          <EmptyState
            icon={<Users className="size-14" />}
            title={t("usersPage.errorEmptyTitle")}
            description={t("usersPage.errorEmptyDescription")}
            action={
              <Button type="button" variant="outline" onClick={() => refetch()}>
                {t("usersPage.retry")}
              </Button>
            }
          />
        </AdminSurface>
      </AdminPageShell>
    );
  }

  return (
    <AdminPageShell
      title={t("users")}
      description={t("usersPage.description")}
      badges={
        <>
          <Badge variant="primary">{t("usersPage.badges.accessReview")}</Badge>
          <Badge>{t("usersPage.badges.totalUsers", { count: totalUsers })}</Badge>
        </>
      }
    >
      <AdminToolbar>
        <div className="flex flex-1 flex-col gap-3 md:flex-row md:items-center">
          <div className="relative w-full max-w-md">
            <Search className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-slate-400" />
            <Input
              value={search}
              onChange={(event) => setSearch(event.target.value)}
              onKeyDown={(event) => {
                if (event.key === "Escape" && search) {
                  event.preventDefault();
                  clearSearch();
                }
              }}
              placeholder={t("usersPage.searchPlaceholder")}
              className="pl-9 pr-10"
              aria-label={t("usersPage.searchAriaLabel")}
            />

            {search ? (
              <button
                type="button"
                onClick={clearSearch}
                className="absolute right-2 top-1/2 -translate-y-1/2 rounded-full p-1 text-slate-400 transition hover:bg-slate-100 hover:text-slate-600 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 dark:hover:bg-slate-700 dark:hover:text-slate-200"
                aria-label={t("common:clearSearch")}
                title={t("common:clearSearch")}
              >
                <X className="size-4" />
              </button>
            ) : null}
          </div>
        </div>
      </AdminToolbar>

      <AdminSurface
        title={t("usersPage.directoryTitle")}
        description={t("usersPage.directoryDescription")}
        contentClassName="space-y-0"
      >
        {content.length === 0 ? (
          <EmptyState
            icon={<Users className="size-16" />}
            title={t("usersPage.emptyTitle")}
            description={t("usersPage.emptyDescription")}
          />
        ) : filteredUsers.length === 0 ? (
          <EmptyState
            icon={<Search className="size-14" />}
            title={t("usersPage.emptyFilteredTitle")}
            description={t("usersPage.emptyFilteredDescription")}
          />
        ) : (
          <>
            <DataTable
              columns={[
                {
                  key: "username",
                  label: t("usersPage.columns.user"),
                  sortable: true,
                  render: (user: UserSummaryResponse) => (
                    <div>
                      <p className="font-semibold text-slate-900 dark:text-slate-100">{user.username}</p>
                      <p className="text-xs text-slate-500 dark:text-slate-400">{user.email}</p>
                    </div>
                  ),
                },
                {
                  key: "status",
                  label: t("usersPage.columns.status"),
                  sortable: true,
                  render: (user: UserSummaryResponse) => <StatusBadge status={user.status} />,
                },
                {
                  key: "emailVerified",
                  label: t("usersPage.columns.security"),
                  sortable: false,
                  render: (user: UserSummaryResponse) => (
                    <div className="flex flex-wrap gap-2">
                      <Badge variant={user.emailVerified ? "success" : "warning"}>
                        {user.emailVerified ? t("usersPage.emailVerified") : t("usersPage.emailPending")}
                      </Badge>
                      <Badge variant={user.mfaEnabled ? "primary" : "default"}>
                        {user.mfaEnabled ? t("usersPage.mfaEnabled") : t("usersPage.mfaOff")}
                      </Badge>
                    </div>
                  ),
                },
                {
                  key: "lastLoginAt",
                  label: t("usersPage.columns.lastLogin"),
                  sortable: false,
                  render: (user: UserSummaryResponse) =>
                    user.lastLoginAt ? new Date(user.lastLoginAt).toLocaleString() : t("usersPage.never"),
                },
                {
                  key: "createdAt",
                  label: t("created"),
                  sortable: true,
                  render: (user: UserSummaryResponse) => new Date(user.createdAt).toLocaleDateString(),
                },
                {
                  key: "actions",
                  label: t("usersPage.columns.actions"),
                  sortable: false,
                  render: (user: UserSummaryResponse) => (
                    <Button
                      type="button"
                      variant="ghost"
                      size="sm"
                      onClick={(event) => {
                        event.stopPropagation();
                        handleRowClick(user);
                      }}
                    >
                      <ShieldCheck className="size-4" />
                      {t("usersPage.open")}
                    </Button>
                  ),
                },
              ]}
              data={filteredUsers}
              currentSort={sort}
              onSort={handleSort}
              onRowClick={handleRowClick}
            />

            <Pagination
              currentPage={data.page ?? 0}
              totalPages={Math.max(data.totalPages ?? 0, 1)}
              onPageChange={setPage}
              statusContent={
                <>
                  <span className="rounded-full bg-slate-100 px-3 py-1 text-sm text-slate-600 dark:bg-slate-800 dark:text-slate-300">
                    {t("usersPage.sortingStatus", {
                      sort: sort ? `${sort.field} (${sort.direction})` : t("usersPage.sortNone"),
                    })}
                  </span>
                </>
              }
            />
          </>
        )}
      </AdminSurface>
    </AdminPageShell>
  );
}
