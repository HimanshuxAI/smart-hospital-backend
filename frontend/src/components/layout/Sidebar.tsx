import {
  Activity,
  CalendarClock,
  CalendarDays,
  Gauge,
  HeartPulse,
  LayoutDashboard,
  ListChecks,
  Stethoscope,
  TicketCheck,
  UserRoundCog,
  UsersRound,
  LogOut,
  type LucideIcon,
} from "lucide-react";
import { NavLink, useNavigate } from "react-router-dom";

import type { UserRole } from "../../api/types";
import { navigationByRole, roleLabels } from "../../lib/constants";
import { useContext } from "react";
import { AuthContext } from "../../auth/auth-context";

const icons: Record<string, LucideIcon> = {
  overview: LayoutDashboard,
  doctor: Stethoscope,
  appointment: CalendarDays,
  queue: TicketCheck,
  schedule: CalendarClock,
  patients: UsersRound,
  operations: Gauge,
  users: UserRoundCog,
  departments: Activity,
};

export function Sidebar({ role, onNavigate }: { role: UserRole; onNavigate?: () => void }) {
  const auth = useContext(AuthContext);
  const navigate = useNavigate();

  const handleSignOut = async () => {
    if (auth?.signOut) {
      await auth.signOut();
    }
    navigate("/login", { replace: true });
  };

  return (
    <aside className="sidebar">
      <div className="brand">
        <span className="brand-mark" aria-hidden="true">
          <HeartPulse size={21} />
        </span>
        <div>
          <strong style={{ color: "var(--theme-text)" }}>Smart Hospital</strong>
          <small style={{ color: "var(--theme-muted)" }}>{roleLabels[role]}</small>
        </div>
      </div>

      <nav className="sidebar-nav" aria-label={`${role.toLowerCase()} navigation`}>
        {navigationByRole[role].map((item) => {
          const Icon = icons[item.icon] ?? ListChecks;
          return (
            <NavLink
              key={item.path}
              to={item.path}
              onClick={onNavigate}
              end={item.path === `/${role.toLowerCase()}`}
              className={({ isActive }) =>
                `sidebar-link ${isActive ? "sidebar-link-active" : ""}`.trim()
              }
            >
              <Icon size={18} aria-hidden="true" />
              <span>{item.label}</span>
            </NavLink>
          );
        })}
      </nav>

      <div style={{ marginTop: "auto", paddingTop: "1rem", borderTop: "1.5px dashed var(--theme-border)" }}>
        <button
          type="button"
          onClick={handleSignOut}
          className="button button-secondary"
          style={{
            width: "100%",
            justifyContent: "flex-start",
            gap: "0.6rem",
            color: "var(--theme-emergency)",
            borderColor: "var(--theme-border)",
            fontSize: "0.82rem"
          }}
        >
          <LogOut size={16} color="var(--theme-emergency)" />
          <span>Sign Out</span>
        </button>
      </div>
    </aside>
  );
}
