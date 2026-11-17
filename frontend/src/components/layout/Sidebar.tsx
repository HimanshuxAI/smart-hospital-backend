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
  type LucideIcon,
} from "lucide-react";
import { NavLink } from "react-router-dom";

import type { UserRole } from "../../api/types";
import { navigationByRole, roleLabels } from "../../lib/constants";

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

export function Sidebar({ role }: { role: UserRole }) {
  return (
    <aside className="sidebar">
      <div className="brand">
        <span className="brand-mark" aria-hidden="true">
          <HeartPulse size={21} />
        </span>
        <span>
          <strong>Smart Hospital</strong>
          <small>{roleLabels[role]}</small>
        </span>
      </div>
      <nav className="sidebar-nav" aria-label={`${role.toLowerCase()} navigation`}>
        {navigationByRole[role].map((item) => {
          const Icon = icons[item.icon] ?? ListChecks;
          return (
            <NavLink
              key={item.path}
              to={item.path}
              end={item.path === `/${role.toLowerCase()}`}
              className={({ isActive }) =>
                `sidebar-link ${isActive ? "sidebar-link-active" : ""}`.trim()
              }
            >
              <Icon size={19} aria-hidden="true" />
              <span>{item.label}</span>
            </NavLink>
          );
        })}
      </nav>
      <div className="sidebar-footer">
        <span className="status-dot" aria-hidden="true" />
        All systems operational
      </div>
    </aside>
  );
}
