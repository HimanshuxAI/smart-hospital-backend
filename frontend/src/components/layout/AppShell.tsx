import { useEffect, useState, type ReactNode } from "react";
import { useLocation } from "react-router-dom";

import type { AuthUser, UserRole } from "../../api/types";
import { Drawer } from "../ui/drawer";
import { MobileNavigation } from "./MobileNavigation";
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
  children?: ReactNode;
}) {
  const [navigationOpen, setNavigationOpen] = useState(false);
  const location = useLocation();

  useEffect(() => {
    setNavigationOpen(false);
  }, [location.pathname]);

  return (
    <div className="app-shell">
      <a className="skip-link" href="#main-content">
        Skip to main content
      </a>
      <div className="desktop-sidebar">
        <Sidebar role={role} />
      </div>
      <Drawer
        open={navigationOpen}
        title="Navigation"
        onClose={() => setNavigationOpen(false)}
      >
        <Sidebar role={role} />
      </Drawer>
      <div className="app-column">
        <TopBar
          user={user}
          onOpenNavigation={() => setNavigationOpen(true)}
          demoMode={demoMode}
        />
        <main id="main-content" className="app-main">
          {children}
        </main>
        <MobileNavigation role={role} />
      </div>
    </div>
  );
}
