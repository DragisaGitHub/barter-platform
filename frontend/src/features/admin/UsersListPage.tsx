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
import { Search, ShieldCheck, Users } from "lucide-react";
import { AdminPageShell, AdminSurface, AdminToolbar } from "./components/AdminPageShell";

export function UsersListPage() {
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
        title="Users"
        description="Review account status, verification posture, and direct navigation into existing user detail screens."
        badges={
          <>
            <Badge variant="primary">Access review</Badge>
            <Badge>User directory</Badge>
          </>
        }
      >
        <AdminSurface title="Loading users" description="Fetching the current page of user accounts.">
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
        title="Users"
        description="Review account status, verification posture, and direct navigation into existing user detail screens."
        badges={
          <>
            <Badge variant="primary">Access review</Badge>
            <Badge>User directory</Badge>
          </>
        }
      >
        <AdminSurface title="Unable to load users" description="The current user directory request could not be completed.">
          <EmptyState
            icon={<Users className="size-14" />}
            title="User directory unavailable"
            description="Try refreshing the list. Existing backend contracts and detail routes remain unchanged."
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
      title="Users"
      description="Manage the existing user directory with safe search, current backend paging, and account-status visibility for moderation workflows."
      badges={
        <>
          <Badge variant="primary">Access review</Badge>
          <Badge>{totalUsers} total users</Badge>
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
              placeholder="Search username, email, status, or verification state"
              className="pl-9"
              aria-label="Search users on the current page"
            />
          </div>
        </div>

        <Button type="button" variant="outline" onClick={() => refetch()}>
          Refresh
        </Button>
      </AdminToolbar>

      <AdminSurface
        title="User directory"
        description="Search filters the currently loaded page while sorting and pagination continue to use the existing backend support."
        contentClassName="space-y-0"
      >
        {content.length === 0 ? (
          <EmptyState
            icon={<Users className="size-16" />}
            title="No users found"
            description="There are no users in the system yet."
          />
        ) : filteredUsers.length === 0 ? (
          <EmptyState
            icon={<Search className="size-14" />}
            title="No users match this filter"
            description="Try a different search term or move to another page to continue reviewing user accounts."
          />
        ) : (
          <>
            <DataTable
              columns={[
                {
                  key: "username",
                  label: "User",
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
                  label: "Status",
                  sortable: true,
                  render: (user: UserSummaryResponse) => <StatusBadge status={user.status} />,
                },
                {
                  key: "emailVerified",
                  label: "Security",
                  sortable: false,
                  render: (user: UserSummaryResponse) => (
                    <div className="flex flex-wrap gap-2">
                      <Badge variant={user.emailVerified ? "success" : "warning"}>
                        {user.emailVerified ? "Email verified" : "Email pending"}
                      </Badge>
                      <Badge variant={user.mfaEnabled ? "primary" : "default"}>
                        {user.mfaEnabled ? "MFA enabled" : "MFA off"}
                      </Badge>
                    </div>
                  ),
                },
                {
                  key: "lastLoginAt",
                  label: "Last login",
                  sortable: false,
                  render: (user: UserSummaryResponse) =>
                    user.lastLoginAt ? new Date(user.lastLoginAt).toLocaleString() : "Never",
                },
                {
                  key: "createdAt",
                  label: "Created",
                  sortable: true,
                  render: (user: UserSummaryResponse) => new Date(user.createdAt).toLocaleDateString(),
                },
                {
                  key: "actions",
                  label: "Actions",
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
                      Open
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
                    Sorting: {sort ? `${sort.field} (${sort.direction})` : "none"}
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
