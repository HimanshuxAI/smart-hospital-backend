import { screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import { createDemoAdapter } from "../../mocks/adapter";
import { renderApp } from "../../test/render";
import { PatientQueuePage } from "./PatientQueuePage";

describe("patient queue token", () => {
  it("shows the active token, triage, and position for the signed-in patient", async () => {
    renderApp(<PatientQueuePage />, {
      adapter: createDemoAdapter(),
      email: "patient@hospital.local",
    });

    expect(await screen.findByText("#15")).toBeInTheDocument();
    expect(screen.getByText("Normal")).toBeInTheDocument();
    expect(screen.getByText(/ahead of you/)).toBeInTheDocument();
  });
});
