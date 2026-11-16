import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { useState } from "react";
import { describe, expect, it, vi } from "vitest";

import { Avatar } from "./avatar";
import { Button } from "./button";
import { Dialog } from "./dialog";
import { Tabs } from "./tabs";
import { TextField } from "./field";
import { ToastProvider, ToastViewport } from "./toast";

describe("clinical UI primitives", () => {
  it("renders a native disabled loading button", async () => {
    render(
      <Button loading loadingLabel="Saving appointment">
        Save
      </Button>,
    );

    const button = screen.getByRole("button", { name: "Saving appointment" });
    expect(button).toBeDisabled();
    expect(button).toHaveAttribute("aria-busy", "true");
  });

  it("associates a visible field label, description, and error", () => {
    render(
      <TextField
        label="Email address"
        error="Enter a valid email address"
        hint="Use your registered address"
      />,
    );

    const input = screen.getByLabelText("Email address");
    expect(input).toHaveAttribute("aria-invalid", "true");
    expect(input).toHaveAccessibleDescription(
      "Use your registered address Enter a valid email address",
    );
  });

  it("opens an accessible dialog, closes on Escape, and restores focus", async () => {
    const user = userEvent.setup();

    function Example() {
      const [open, setOpen] = useState(false);
      return (
        <>
          <button onClick={() => setOpen(true)}>Open details</button>
          <Dialog
            open={open}
            onClose={() => setOpen(false)}
            title="Appointment details"
          >
            <p>Tuesday at 10:30</p>
          </Dialog>
        </>
      );
    }

    render(<Example />);
    const trigger = screen.getByRole("button", { name: "Open details" });
    await user.click(trigger);

    const dialog = screen.getByRole("dialog", { name: "Appointment details" });
    expect(dialog).toBeInTheDocument();
    await user.keyboard("{Escape}");
    await waitFor(() => expect(dialog).not.toBeInTheDocument());
    expect(trigger).toHaveFocus();
  });

  it("moves between tabs with arrow keys", async () => {
    const user = userEvent.setup();
    render(
      <Tabs
        ariaLabel="Queue views"
        tabs={[
          { id: "waiting", label: "Waiting" },
          { id: "served", label: "Served" },
        ]}
      />,
    );

    const waiting = screen.getByRole("tab", { name: "Waiting" });
    waiting.focus();
    await user.keyboard("{ArrowRight}");

    expect(screen.getByRole("tab", { name: "Served" })).toHaveAttribute(
      "aria-selected",
      "true",
    );
  });

  it("provides an accessible avatar fallback", () => {
    render(<Avatar name="Maya Patel" />);
    expect(screen.getByRole("img", { name: "Maya Patel" })).toHaveTextContent("MP");
  });

  it("announces toast feedback politely", () => {
    render(
      <ToastProvider>
        <ToastViewport />
      </ToastProvider>,
    );

    expect(screen.getByRole("status")).toHaveAttribute("aria-live", "polite");
  });
});
