import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it } from "vitest";
import { MemoryRouter, Route, Routes, useLocation } from "react-router-dom";

import type { AuthUser, UserRole } from "../../api/types";
import { AuthProvider } from "../../auth/AuthProvider";
import { createDemoAdapter } from "../../mocks/adapter";
import { AppShell } from "./AppShell";
import { Sidebar } from "./Sidebar";

const navByRole = {
  PATIENT: ["Overview", "Find a doctor", "My appointments", "Queue token"],
  DOCTOR: ["Today", "Live queue", "My schedule", "Patients"],
  ADMIN: ["Operations", "Users", "Doctors", "Schedules"],
} satisfies Record<UserRole, string[]>;

const adminUser: AuthUser = { id: 1, email: "admin@hospital.local", role: "ADMIN" };

function LocationProbe() {
  const location = useLocation();
  return <p data-testid="location">{location.pathname}</p>;
}

describe("responsive clinical layout", () => {
  it.each(Object.entries(navByRole) as [UserRole, string[]][])(
    "shows distinct %s navigation destinations",
    (role, labels) => {
      render(
        <MemoryRouter>
          <Sidebar role={role} />
        </MemoryRouter>,
      );

      const nav = screen.getByRole("navigation", { name: `${role.toLowerCase()} navigation` });
      for (const label of labels) {
        expect(within(nav).getByRole("link", { name: label })).toBeInTheDocument();
      }
    },
  );

  it("marks the current page for assistive technology", () => {
    render(
      <MemoryRouter initialEntries={["/patient/appointments"]}>
        <Sidebar role="PATIENT" />
      </MemoryRouter>,
    );

    expect(screen.getByRole("link", { name: "My appointments" })).toHaveAttribute(
      "aria-current",
      "page",
    );
  });

  it("navigates to the selected destination from the sidebar", async () => {
    const user = userEvent.setup();

    render(
      <MemoryRouter initialEntries={["/admin"]}>
        <AuthProvider adapter={createDemoAdapter()}>
          <Routes>
            <Route
              path="*"
              element={
                <>
                  <AppShell role="ADMIN" user={adminUser} />
                  <LocationProbe />
                </>
              }
            />
          </Routes>
        </AuthProvider>
      </MemoryRouter>,
    );

    const desktopNav = screen.getAllByRole("navigation", { name: "admin navigation" })[0];
    await user.click(within(desktopNav as HTMLElement).getByRole("link", { name: "Users" }));

    expect(screen.getByTestId("location")).toHaveTextContent("/admin/users");
  });

  it("closes the mobile navigation drawer after choosing a destination", async () => {
    const user = userEvent.setup();

    render(
      <MemoryRouter initialEntries={["/admin"]}>
        <AuthProvider adapter={createDemoAdapter()}>
          <Routes>
            <Route
              path="*"
              element={
                <>
                  <AppShell role="ADMIN" user={adminUser} />
                  <LocationProbe />
                </>
              }
            />
          </Routes>
        </AuthProvider>
      </MemoryRouter>,
    );

    await user.click(screen.getByRole("button", { name: "Open navigation" }));
    expect(screen.getByRole("dialog", { name: "Navigation" })).toBeInTheDocument();

    const drawer = screen.getByRole("dialog", { name: "Navigation" });
    await user.click(within(drawer).getByRole("link", { name: "Schedules" }));

    expect(screen.queryByRole("dialog", { name: "Navigation" })).not.toBeInTheDocument();
    expect(screen.getByTestId("location")).toHaveTextContent("/admin/schedules");
  });
});
