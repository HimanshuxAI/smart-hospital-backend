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
          <HeartPulse size={20} />
        </span>
        <div>
          <strong>Aegle</strong>
          <small>{roleLabels[role]}</small>
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

      <div className="sidebar-foot">
        <button
          type="button"
          onClick={handleSignOut}
          className="sidebar-signout"
        >
          <LogOut size={16} aria-hidden="true" />
          <span>Sign out</span>
        </button>
      </div>
    </aside>
  );
}
