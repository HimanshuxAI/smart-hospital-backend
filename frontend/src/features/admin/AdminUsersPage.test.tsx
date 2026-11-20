import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it } from "vitest";

import { createDemoAdapter } from "../../mocks/adapter";
import { renderApp } from "../../test/render";
import { AdminUsersPage } from "./AdminUsersPage";

describe("administrator user management", () => {
  it("lists accounts with their role and activation state", async () => {
    renderApp(<AdminUsersPage />, { adapter: createDemoAdapter() });

    expect(await screen.findByText("Avery Admin")).toBeInTheDocument();
    expect(screen.getAllByText("admin@hospital.local").length).toBeGreaterThan(0);
    expect(screen.getAllByText("Deactivated").length).toBeGreaterThan(0);
  });

  it("asks for confirmation before deactivating an account", async () => {
    const user = userEvent.setup();
    renderApp(<AdminUsersPage />, { adapter: createDemoAdapter() });

    const deactivateButtons = await screen.findAllByRole("button", { name: "Deactivate" });
    await user.click(deactivateButtons[0]!);

    const dialog = screen.getByRole("dialog", { name: "Deactivate account" });
    expect(dialog).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Deactivate account" })).toBeInTheDocument();
  });

  it("activates a deactivated account after confirmation", async () => {
    const user = userEvent.setup();
    const adapter = createDemoAdapter();
    renderApp(<AdminUsersPage />, { adapter });

    const activateButtons = await screen.findAllByRole("button", { name: "Activate" });
    await user.click(activateButtons[0]!);
    await user.click(screen.getByRole("button", { name: "Activate account" }));

    await waitFor(() => {
      expect(screen.queryAllByRole("button", { name: "Activate" })).toHaveLength(0);
    });
  });
});
