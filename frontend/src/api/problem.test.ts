import { describe, expect, it } from "vitest";

import { parseProblem } from "./problem";

describe("parseProblem", () => {
  it("parses a complete RFC 9457 problem response", () => {
    const problem = parseProblem(422, {
      status: 422,
      code: "VALIDATION_FAILED",
      title: "Validation failed",
      detail: "Two fields are invalid",
      path: "/api/v1/auth/register",
      timestamp: "2026-11-15T10:30:00Z",
      fieldErrors: { email: "Email is required" },
    });

    expect(problem).toEqual({
      status: 422,
      code: "VALIDATION_FAILED",
      title: "Validation failed",
      detail: "Two fields are invalid",
      path: "/api/v1/auth/register",
      timestamp: "2026-11-15T10:30:00Z",
      fieldErrors: { email: "Email is required" },
    });
  });

  it("supports the backend message alias and errors field", () => {
    const problem = parseProblem(409, {
      code: "SLOT_ALREADY_BOOKED",
      title: "Conflict",
      message: "That slot is already booked",
      errors: { slotId: "Slot is unavailable" },
    });

    expect(problem.detail).toBe("That slot is already booked");
    expect(problem.fieldErrors).toEqual({ slotId: "Slot is unavailable" });
  });

  it("uses stable fallbacks and never exposes a non-string raw body", () => {
    const problem = parseProblem(500, { unexpected: { secret: "value" } });

    expect(problem.code).toBe("HTTP_500");
    expect(problem.title).toBe("Internal Server Error");
    expect(problem.detail).toBe("Internal Server Error");
    expect(problem.fieldErrors).toEqual({});
    expect(JSON.stringify(problem)).not.toContain("secret");
  });

  it("parses Retry-After seconds", () => {
    const problem = parseProblem(
      429,
      { code: "RATE_LIMITED", detail: "Try again later" },
      new Headers({ "Retry-After": "45" }),
    );

    expect(problem.retryAfterSeconds).toBe(45);
  });
});
