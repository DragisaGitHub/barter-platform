import { useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { ArrowLeft } from "lucide-react";
import { toast } from "sonner";
import { apiClient } from "../../api/axios";
import type { UserResponse, UserStatus } from "../../api/generated/types";
import { Button } from "../../components/ui/Button";
import { Card, CardContent, CardHeader, CardTitle } from "../../components/ui/Card";
import { StatusBadge } from "../../components/ui/StatusBadge";
import { Badge } from "../../components/ui/Badge";
import { Modal } from "../../components/ui/Modal";
import { Skeleton } from "../../components/ui/Skeleton";
import { parseApiError } from "../../utils";

export function UserDetailPage() {
  const { uuid } = useParams<{ uuid: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [selectedStatus, setSelectedStatus] = useState<UserStatus | null>(null);

  const { data: user, isLoading } = useQuery({
    queryKey: ["user", uuid],
    queryFn: async () => {
      const response = await apiClient.get<UserResponse>(`/users/${uuid}`);
      return response.data;
    },
    enabled: !!uuid,
  });

  const updateStatusMutation = useMutation({
    mutationFn: async (status: UserStatus) => {
      const response = await apiClient.patch<UserResponse>(
        `/users/${uuid}/status`,
        { status }
      );
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["user", uuid] });
      queryClient.invalidateQueries({ queryKey: ["users"] });
      toast.success("User status updated successfully");
      setIsModalOpen(false);
      setSelectedStatus(null);
    },
    onError: (error) => {
      toast.error(parseApiError(error));
    },
  });

  const statuses: UserStatus[] = [
    "PENDING_VERIFICATION",
    "ACTIVE",
    "SUSPENDED",
    "BANNED",
    "DELETED",
  ];

  if (isLoading) {
    return (
      <div className="max-w-4xl mx-auto">
        <Skeleton className="h-8 w-32 mb-8" />
        <Skeleton className="h-64 w-full" />
      </div>
    );
  }

  if (!user) {
    return (
      <div className="max-w-4xl mx-auto">
        <p className="text-slate-600 dark:text-slate-400">User not found</p>
      </div>
    );
  }

  return (
    <div className="max-w-4xl mx-auto">
      <Button
        variant="ghost"
        size="sm"
        onClick={() => navigate("/admin/users")}
        className="mb-6"
      >
        <ArrowLeft className="size-4" />
        Back to Users
      </Button>

      <Card>
        <CardHeader>
          <CardTitle>User Details</CardTitle>
        </CardHeader>
        <CardContent className="space-y-6">
          <div className="grid gap-6 md:grid-cols-2">
            <div>
              <label className="text-sm font-medium text-slate-700 dark:text-slate-300">
                Username
              </label>
              <p className="mt-1 text-slate-900 dark:text-slate-100">{user.username}</p>
            </div>

            <div>
              <label className="text-sm font-medium text-slate-700 dark:text-slate-300">
                Email
              </label>
              <p className="mt-1 text-slate-900 dark:text-slate-100">{user.email}</p>
            </div>

            <div>
              <label className="text-sm font-medium text-slate-700 dark:text-slate-300">
                User ID
              </label>
              <p className="mt-1 text-slate-900 dark:text-slate-100 font-mono text-sm">
                {user.uuid}
              </p>
            </div>

            <div>
              <label className="text-sm font-medium text-slate-700 dark:text-slate-300">
                Status
              </label>
              <div className="mt-1">
                <StatusBadge status={user.status} />
              </div>
            </div>

            <div>
              <label className="text-sm font-medium text-slate-700 dark:text-slate-300">
                Created At
              </label>
              <p className="mt-1 text-slate-900 dark:text-slate-100">
                {new Date(user.createdAt).toLocaleString()}
              </p>
            </div>

            <div>
              <label className="text-sm font-medium text-slate-700 dark:text-slate-300">
                Updated At
              </label>
              <p className="mt-1 text-slate-900 dark:text-slate-100">
                {user.updatedAt ? new Date(user.updatedAt).toLocaleString() : "—"}
              </p>
            </div>
          </div>

          <div>
            <label className="text-sm font-medium text-slate-700 dark:text-slate-300 block mb-2">
              Roles
            </label>
            <div className="flex gap-2">
              {(user.roles ?? []).map((role) => (
                <Badge key={role.code} variant="primary">
                  {role.name}
                </Badge>
              ))}
            </div>
          </div>

          <div className="pt-4 border-t border-slate-200 dark:border-slate-700">
            <Button onClick={() => setIsModalOpen(true)}>Change Status</Button>
          </div>
        </CardContent>
      </Card>

      <Modal
        isOpen={isModalOpen}
        onClose={() => {
          setIsModalOpen(false);
          setSelectedStatus(null);
        }}
        title="Change User Status"
        size="sm"
      >
        <div className="space-y-4">
          <p className="text-sm text-slate-600 dark:text-slate-400">
            Select a new status for {user.username}:
          </p>

          <div className="space-y-2">
            {statuses.map((status) => (
              <button
                key={status}
                onClick={() => setSelectedStatus(status)}
                className={`w-full text-left px-4 py-3 rounded-lg border transition-colors ${
                  selectedStatus === status
                    ? "border-indigo-500 bg-indigo-50 dark:bg-indigo-900/20"
                    : "border-slate-200 dark:border-slate-700 hover:bg-slate-50 dark:hover:bg-slate-800"
                }`}
              >
                <StatusBadge status={status} />
              </button>
            ))}
          </div>

          <div className="flex gap-3 pt-4">
            <Button
              variant="outline"
              onClick={() => {
                setIsModalOpen(false);
                setSelectedStatus(null);
              }}
              fullWidth
            >
              Cancel
            </Button>
            <Button
              onClick={() => selectedStatus && updateStatusMutation.mutate(selectedStatus)}
              disabled={!selectedStatus || updateStatusMutation.isPending}
              isLoading={updateStatusMutation.isPending}
              fullWidth
            >
              Update
            </Button>
          </div>
        </div>
      </Modal>
    </div>
  );
}
