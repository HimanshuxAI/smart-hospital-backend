import type { ReactNode } from "react";
import { Navigate, useLocation } from "react-router-dom";

import type { UserRole } from "../api/types";
import { useAuth } from "./auth-context";

type RequireAuthProps = {
  children: ReactNode;
};

export function RequireAuth({ children }: RequireAuthProps) {
  const { status } = useAuth();
  const location = useLocation();

  if (status === "restoring") {
    return (
      <div role="status" className="page-state">
        Restoring your session
      </div>
    );
  }

  if (status === "anonymous") {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }

  return children;
}

type RequireRoleProps = {
  role: UserRole;
  children: ReactNode;
};

const dashboardByRole: Record<UserRole, string> = {
  PATIENT: "/patient",
  DOCTOR: "/doctor",
  ADMIN: "/admin",
};

export function RequireRole({ role, children }: RequireRoleProps) {
  const { user } = useAuth();

  if (!user) {
    return <Navigate to="/login" replace />;
  }

  if (user.role !== role) {
    return <Navigate to={dashboardByRole[user.role]} replace />;
  }

  return children;
}
