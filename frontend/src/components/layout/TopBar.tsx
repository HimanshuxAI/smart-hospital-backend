import { Bell, Menu } from "lucide-react";

import type { AuthUser } from "../../api/types";
import { Avatar } from "../ui/avatar";

export function TopBar({
  user,
  onOpenNavigation,
  demoMode = false,
}: {
  user: AuthUser;
  onOpenNavigation(): void;
  demoMode?: boolean;
}) {
  return (
    <header className="topbar">
      <div className="topbar-leading">
        <button
          className="icon-button mobile-menu-trigger"
          type="button"
          aria-label="Open navigation"
          onClick={onOpenNavigation}
        >
          <Menu size={21} aria-hidden="true" />
        </button>
        {demoMode ? <span className="demo-badge">Demo data</span> : null}
      </div>
      <div className="topbar-actions">
        <button className="icon-button" type="button" aria-label="Notifications">
          <Bell size={19} aria-hidden="true" />
        </button>
        <div className="user-summary">
          <Avatar name={user.email.split("@")[0] ?? user.email} size="sm" />
          <span>
            <strong>{user.email}</strong>
            <small>{user.role.toLowerCase()}</small>
          </span>
        </div>
      </div>
    </header>
  );
}
