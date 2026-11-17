import { beforeEach, describe, expect, it } from "vitest";

import { createDemoAdapter } from "./adapter";

describe("demo adapter session persistence", () => {
  beforeEach(() => {
    sessionStorage.clear();
  });

  it("restores the selected role and email on a fresh adapter", async () => {
    const firstVisit = createDemoAdapter();
    await firstVisit.login({ email: "admin@hospital.local", password: "Admin123!" });

    const afterReload = createDemoAdapter();
    const restored = await afterReload.refresh("demo-refresh-token");

    expect(restored.user.role).toBe("ADMIN");
    expect(restored.user.email).toBe("admin@hospital.local");
  });

  it("returns to the patient demo after sign out", async () => {
    const adapter = createDemoAdapter();
    await adapter.login({ email: "doctor@hospital.local", password: "Doctor123!" });

    await adapter.logout("demo-refresh-token");

    const afterReload = createDemoAdapter();
    const restored = await afterReload.refresh("demo-refresh-token");

    expect(restored.user.role).toBe("PATIENT");
    expect(restored.user.email).toBe("patient@hospital.local");
  });
});
