import { useState, type ReactNode } from "react";
import type { AuthUser, UserRole } from "../../api/types";
import { Sidebar } from "./Sidebar";
import { TopBar } from "./TopBar";
import { X } from "lucide-react";

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
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false);
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  return (
    <div className="app-shell">
      {/* Desktop Persistent Sidebar (collapsible via hamburger button) */}
      {!sidebarCollapsed && (
        <aside className="desktop-sidebar">
          <Sidebar role={role} />
        </aside>
      )}

      {/* Mobile Drawer Overlay when hamburger is clicked on smaller screens */}
      {mobileMenuOpen && (
        <div
          style={{
            position: "fixed",
            inset: 0,
            background: "rgba(17, 19, 21, 0.4)",
            backdropFilter: "blur(4px)",
            zIndex: 100,
            display: "flex"
          }}
          onClick={() => setMobileMenuOpen(false)}
        >
          <div
            style={{
              width: "280px",
              height: "100%",
              background: "#fbf9f4",
              borderRight: "2px solid var(--color-ink)",
              boxShadow: "var(--shadow-hard-lg)",
              position: "relative"
            }}
            onClick={(e) => e.stopPropagation()}
          >
            <button
              onClick={() => setMobileMenuOpen(false)}
              className="icon-button"
              style={{ position: "absolute", top: "1rem", right: "1rem", zIndex: 10 }}
            >
              <X size={16} />
            </button>
            <Sidebar role={role} onNavigate={() => setMobileMenuOpen(false)} />
          </div>
        </div>
      )}

      {/* Right Content Area */}
      <div className="app-column">
        <TopBar
          user={user}
          onOpenNavigation={() => {
            // Toggles sidebar collapse on desktop or opens drawer on mobile
            if (window.innerWidth <= 900) {
              setMobileMenuOpen(prev => !prev);
            } else {
              setSidebarCollapsed(prev => !prev);
            }
          }}
          demoMode={demoMode}
        />
        <main id="main-content" className="app-main">
          {children}
        </main>
      </div>
    </div>
  );
}
