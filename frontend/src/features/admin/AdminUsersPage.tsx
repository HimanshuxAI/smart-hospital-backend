import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";

import { useApi } from "../../api/api-context";
import type { AdminUser } from "../../api/types";
import { WorkspacePage } from "../../components/layout/WorkspacePage";
import { Badge } from "../../components/ui/badge";
import { Button } from "../../components/ui/button";
import { Dialog } from "../../components/ui/dialog";
import { EmptyState, ErrorState, LoadingState } from "../../components/ui/feedback";
import { DataTable } from "../../components/ui/table";
import { queryKeys } from "../../lib/query-keys";

const roleTone: Record<AdminUser["role"], "info" | "success" | "neutral"> = {
  ADMIN: "info",
  DOCTOR: "success",
  PATIENT: "neutral",
};

export function AdminUsersPage() {
  const api = useApi();
  const queryClient = useQueryClient();
  const [pendingUser, setPendingUser] = useState<AdminUser | null>(null);

  const usersQuery = useQuery({
    queryKey: queryKeys.adminUsers,
    queryFn: () => api.getAdminUsers(),
  });

  const activation = useMutation({
    mutationFn: (input: { id: number; active: boolean }) =>
      api.setUserActivation(input.id, input.active),
    onSuccess: async () => {
      setPendingUser(null);
      await queryClient.invalidateQueries({ queryKey: queryKeys.adminUsers });
    },
  });

  const users = usersQuery.data ?? [];

  return (
    <WorkspacePage
      role="ADMIN"
      title="Users"
      description="Review every account, its role, and whether it can access hospital services."
    >
      {usersQuery.isLoading ? <LoadingState label="Loading users..." /> : null}

      {usersQuery.isError ? (
        <ErrorState
          title="Could not load users"
          message="The user directory could not be reached. Try again in a moment."
          onRetry={() => void usersQuery.refetch()}
        />
      ) : null}

      {!usersQuery.isLoading && !usersQuery.isError && users.length === 0 ? (
        <EmptyState
          title="No accounts yet"
          description="Accounts appear here as soon as people register or doctors are invited."
        />
      ) : null}

      {users.length > 0 ? (
        <DataTable
          caption="Hospital user accounts"
          rows={users}
          rowKey={(user) => String(user.id)}
          columns={[
            {
              key: "name",
              header: "Name",
              render: (user) => (
                <span className="table-primary">{user.fullName}</span>
              ),
            },
            { key: "email", header: "Email", render: (user) => user.email },
            {
              key: "role",
              header: "Role",
              render: (user) => <Badge tone={roleTone[user.role]}>{user.role}</Badge>,
            },
            {
              key: "status",
              header: "Status",
              render: (user) => (
                <Badge tone={user.active ? "success" : "warning"}>
                  {user.active ? "Active" : "Deactivated"}
                </Badge>
              ),
            },
            {
              key: "actions",
              header: "Actions",
              render: (user) => (
                <Button
                  variant={user.active ? "secondary" : "primary"}
                  size="sm"
                  onClick={() => setPendingUser(user)}
                >
                  {user.active ? "Deactivate" : "Activate"}
                </Button>
              ),
            },
          ]}
        />
      ) : null}

      <Dialog
        open={pendingUser !== null}
        title={pendingUser?.active ? "Deactivate account" : "Activate account"}
        description={
          pendingUser
            ? `${pendingUser.fullName} (${pendingUser.email}) will ${
                pendingUser.active ? "lose access until reactivated" : "regain access"
              }.`
            : ""
        }
        onClose={() => setPendingUser(null)}
      >
        <div className="dialog-actions">
          <Button variant="secondary" onClick={() => setPendingUser(null)}>
            Keep as is
          </Button>
          <Button
            variant={pendingUser?.active ? "danger" : "primary"}
            loading={activation.isPending}
            loadingLabel="Saving..."
            onClick={() => {
              if (pendingUser) {
                activation.mutate({ id: pendingUser.id, active: !pendingUser.active });
              }
            }}
          >
            {pendingUser?.active ? "Deactivate account" : "Activate account"}
          </Button>
        </div>
      </Dialog>
    </WorkspacePage>
  );
}
