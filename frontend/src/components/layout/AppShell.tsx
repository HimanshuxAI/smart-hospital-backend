import { useState, type ReactNode } from "react";
import type { AuthUser, UserRole } from "../../api/types";
import { Sidebar } from "./Sidebar";
import { TopBar } from "./TopBar";

export function AppShell({
  role,
  user,
  demoMode = false,
  children,
}: {
  role: UserRole;
  user: AuthUser;
  demoMode?: boolean;
  children: ReactNode;
}) {
  const [navigationOpen, setNavigationOpen] = useState(false);

  return (
    <div className="app-shell">
      <aside className="desktop-sidebar">
        <Sidebar role={role} />
      </aside>

      <div className="app-column">
        <TopBar
          user={user}
          onOpenNavigation={() => setNavigationOpen(true)}
          demoMode={demoMode}
        />
        <main id="main-content" className="app-main">
          {children}
        </main>
      </div>
    </div>
  );
}
