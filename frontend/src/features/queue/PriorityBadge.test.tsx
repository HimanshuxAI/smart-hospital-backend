import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import { PriorityBadge } from "./PriorityBadge";

describe("PriorityBadge", () => {
  it.each([
    ["CRITICAL", "Critical"],
    ["HIGH", "High"],
    ["MEDIUM", "Medium"],
    ["NORMAL", "Normal"],
  ] as const)("renders the %s priority accessibly", (tier, label) => {
    render(<PriorityBadge tier={tier} />);

    expect(screen.getByText(label)).toBeInTheDocument();
    expect(screen.getByText(label).closest("span")).toHaveAttribute(
      "data-priority",
      tier,
    );
  });
});
