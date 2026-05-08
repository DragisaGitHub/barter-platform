import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { useNavigate } from "react-router-dom";
import { apiClient } from "@/api/axios.ts";
import type { UserPagedResponse, UserSummaryResponse } from "@/api/generated/types.ts";
import { DataTable } from "../../components/data/DataTable";
import { Pagination } from "../../components/data/Pagination";
import { StatusBadge } from "../../components/ui/StatusBadge";
import { Skeleton } from "../../components/ui/Skeleton";
import { EmptyState } from "../../components/ui/EmptyState";
import { Users } from "lucide-react";

export function UsersListPage() {
  const navigate = useNavigate();
  const [page, setPage] = useState(0);
  const [sort, setSort] = useState<{ field: string; direction: "asc" | "desc" } | null>({
    field: "createdAt",
    direction: "desc",
  });

  const { data, isLoading } = useQuery({
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

  if (isLoading) {
    return (
      <div className="max-w-7xl mx-auto">
        <h1 className="text-3xl font-bold text-slate-900 dark:text-white mb-8">Users</h1>
        <div className="space-y-4">
          {[...Array(5)].map((_, i) => (
            <Skeleton key={i} className="h-16 w-full" />
          ))}
        </div>
      </div>
    );
  }

  const content = data?.content ?? [];

  if (!data || content.length === 0) {
    return (
      <div className="max-w-7xl mx-auto">
        <h1 className="text-3xl font-bold text-slate-900 dark:text-white mb-8">Users</h1>
        <EmptyState
          icon={<Users className="size-16" />}
          title="No users found"
          description="There are no users in the system yet."
        />
      </div>
    );
  }

  return (
    <div className="max-w-7xl mx-auto">
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-slate-900 dark:text-white">Users</h1>
        <p className="text-slate-600 dark:text-slate-400 mt-2">
          Manage user accounts and permissions
        </p>
      </div>

      <DataTable
        columns={[
          {
            key: "username",
            label: "Username",
            sortable: true,
            render: (user: UserSummaryResponse) => (
              <span className="font-medium">{user.username}</span>
            ),
          },
          {
            key: "email",
            label: "Email",
            sortable: true,
            render: (user: UserSummaryResponse) => user.email,
          },
          {
            key: "status",
            label: "Status",
            sortable: true,
            render: (user: UserSummaryResponse) => <StatusBadge status={user.status} />,
          },
          {
            key: "createdAt",
            label: "Created",
            sortable: true,
            render: (user: UserSummaryResponse) =>
              new Date(user.createdAt).toLocaleDateString(),
          },
        ]}
        data={content}
        currentSort={sort}
        onSort={handleSort}
        onRowClick={handleRowClick}
      />

      {(data.totalPages ?? 0) > 1 && (
        <Pagination
          currentPage={data.page ?? 0}
          totalPages={data.totalPages ?? 0}
          onPageChange={setPage}
        />
      )}
    </div>
  );
}
