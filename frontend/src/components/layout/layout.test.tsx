import { render, screen, within } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { MemoryRouter } from "react-router-dom";

import type { UserRole } from "../../api/types";
import { Sidebar } from "./Sidebar";

const navByRole = {
  PATIENT: ["Overview", "Find a doctor", "My appointments", "Queue token"],
  DOCTOR: ["Today", "Live queue", "My schedule", "Patients"],
  ADMIN: ["Operations", "Users", "Doctors", "Schedules"],
} satisfies Record<UserRole, string[]>;

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
});
