import type { ReactNode } from "react";

import { useAuth } from "../../auth/auth-context";
import type { UserRole } from "../../api/types";
import { AppShell } from "./AppShell";
import { PageHeader } from "./PageHeader";

export function WorkspacePage({
  role,
  title,
  description,
  actions,
  children,
}: {
  role: UserRole;
  title: string;
  description?: string;
  actions?: ReactNode;
  children: ReactNode;
}) {
  const { user } = useAuth();

  if (!user) {
    return null;
  }

  return (
    <AppShell role={role} user={user}>
      <PageHeader
        title={title}
        {...(description ? { description } : {})}
        {...(actions ? { actions } : {})}
      />
      {children}
    </AppShell>
  );
}
