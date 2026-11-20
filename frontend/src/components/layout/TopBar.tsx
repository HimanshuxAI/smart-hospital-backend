import { Bell, LogOut, Menu } from "lucide-react";
import { useState } from "react";
import { useNavigate } from "react-router-dom";

import type { AuthUser } from "../../api/types";
import { useAuth } from "../../auth/auth-context";
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
  const { signOut } = useAuth();
  const navigate = useNavigate();
  const [signingOut, setSigningOut] = useState(false);

  const handleSignOut = async () => {
    setSigningOut(true);
    try {
      await signOut();
    } finally {
      setSigningOut(false);
      navigate("/login", { replace: true });
    }
  };

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
        <button className="icon-button" type="button" aria-label="Notifications" style={{ color: "var(--theme-muted)" }}>
          <Bell size={19} aria-hidden="true" />
        </button>
        <div className="user-summary">
          <Avatar name={user.email.split("@")[0] ?? user.email} size="sm" />
          <span>
            <strong style={{ color: "var(--theme-text)" }}>{user.email}</strong>
            <small style={{ color: "var(--theme-muted)" }}>{user.role.toLowerCase()}</small>
          </span>
        </div>
        <button
          className="icon-button"
          type="button"
          aria-label="Sign out"
          disabled={signingOut}
          onClick={() => void handleSignOut()}
          style={{ color: "var(--theme-muted)" }}
        >
          <LogOut size={19} aria-hidden="true" />
        </button>
      </div>
    </header>
  );
}
