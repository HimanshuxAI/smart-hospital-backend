import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { MemoryRouter, Route, Routes } from "react-router-dom";

import type { AuthUser, UserRole } from "../api/types";
import { AuthContext, type AuthStatus } from "./auth-context";
import { RequireAuth, RequireRole } from "./guards";

function renderAt(path: string, user: AuthUser | null, status: AuthStatus) {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <AuthContext.Provider
        value={{
          user,
          status,
          signIn: async () => {
            throw new Error("not used");
          },
          register: async () => {
            throw new Error("not used");
          },
          signOut: async () => undefined,
        }}
      >
        <Routes>
          <Route path="/login" element={<div>Login page</div>} />
          <Route path="/patient" element={<RequireAuth><RequireRole role="PATIENT"><div>Patient dashboard</div></RequireRole></RequireAuth>} />
          <Route path="/doctor" element={<RequireAuth><RequireRole role="DOCTOR"><div>Doctor dashboard</div></RequireRole></RequireAuth>} />
          <Route path="/admin" element={<RequireAuth><RequireRole role="ADMIN"><div>Admin dashboard</div></RequireRole></RequireAuth>} />
        </Routes>
      </AuthContext.Provider>
    </MemoryRouter>,
  );
}

const user = (role: UserRole): AuthUser => ({
  id: 1,
  email: `${role.toLowerCase()}@example.com`,
  role,
});

describe("route guards", () => {
  it("redirects an anonymous user to login", () => {
    renderAt("/patient", null, "anonymous");
    expect(screen.getByText("Login page")).toBeInTheDocument();
  });

  it("waits while the session is restoring", () => {
    renderAt("/patient", null, "restoring");
    expect(screen.getByText("Restoring your session")).toBeInTheDocument();
  });

  it("allows each role into its own dashboard", () => {
    renderAt("/patient", user("PATIENT"), "authenticated");
    expect(screen.getByText("Patient dashboard")).toBeInTheDocument();
  });

  it("redirects a patient away from doctor and admin routes", () => {
    renderAt("/doctor", user("PATIENT"), "authenticated");
    expect(screen.getByText("Patient dashboard")).toBeInTheDocument();
  });

  it("redirects a doctor away from admin routes", () => {
    renderAt("/admin", user("DOCTOR"), "authenticated");
    expect(screen.getByText("Doctor dashboard")).toBeInTheDocument();
  });
});
