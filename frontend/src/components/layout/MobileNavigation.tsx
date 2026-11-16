import { NavLink } from "react-router-dom";

import type { UserRole } from "../../api/types";
import { navigationByRole } from "../../lib/constants";

export function MobileNavigation({ role }: { role: UserRole }) {
  const items = navigationByRole[role].slice(0, 4);

  return (
    <nav className="mobile-navigation" aria-label={`${role.toLowerCase()} mobile navigation`}>
      {items.map((item) => (
        <NavLink
          key={item.path}
          to={item.path}
          end={item.path === `/${role.toLowerCase()}`}
          className={({ isActive }) => (isActive ? "active" : undefined)}
          aria-label={item.label}
        >
          <span>{item.label}</span>
        </NavLink>
      ))}
    </nav>
  );
}
