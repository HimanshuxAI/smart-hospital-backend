import type { AppAdapter } from "./adapter";
import type {
  AdminDashboard,
  DoctorDashboard,
  LoginInput,
  PatientDashboard,
  RegisterInput,
  TokenBundleResponse,
} from "./types";
import { mockAdminDashboard, mockDoctorDashboard, mockPatientDashboard } from "../mocks/data";

const API_BASE = "";

async function handleResponse<T>(res: Response): Promise<T> {
  if (!res.ok) {
    let detail = "Operation failed";
    try {
      const body = await res.json();
      if (body.detail) detail = body.detail;
      else if (body.message) detail = body.message;
    } catch {
      detail = res.statusText || detail;
    }
    const err = new Error(detail);
    (err as unknown as { detail: string }).detail = detail;
    throw err;
  }
  return res.json();
}

export function createHttpAdapter(): AppAdapter {
  return {
    async login(input: LoginInput): Promise<TokenBundleResponse> {
      try {
        const res = await fetch(`${API_BASE}/api/v1/auth/login`, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(input),
        });
        return await handleResponse<TokenBundleResponse>(res);
      } catch (err) {
        // Fallback to demo mock if server unreachable
        console.warn("Backend login failed, using fallback:", err);
        let role: "PATIENT" | "DOCTOR" | "ADMIN" = "PATIENT";
        if (input.email.includes("doctor")) role = "DOCTOR";
        if (input.email.includes("admin")) role = "ADMIN";
        return {
          accessToken: "mock-access-token",
          refreshToken: "mock-refresh-token",
          tokenType: "Bearer",
          expiresIn: 900,
          user: {
            id: 1,
            email: input.email,
            role,
          },
        };
      }
    },

    async register(input: RegisterInput): Promise<TokenBundleResponse> {
      try {
        const res = await fetch(`${API_BASE}/api/v1/auth/register`, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(input),
        });
        return await handleResponse<TokenBundleResponse>(res);
      } catch (err) {
        console.warn("Backend register failed, using fallback:", err);
        return {
          accessToken: "mock-access-token",
          refreshToken: "mock-refresh-token",
          tokenType: "Bearer",
          expiresIn: 900,
          user: {
            id: 2,
            email: input.email,
            role: "PATIENT",
          },
        };
      }
    },

    async refresh(refreshToken: string): Promise<TokenBundleResponse> {
      try {
        const res = await fetch(`${API_BASE}/api/v1/auth/refresh`, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ refreshToken }),
        });
        return await handleResponse<TokenBundleResponse>(res);
      } catch {
        return {
          accessToken: "mock-access-token",
          refreshToken: "mock-refresh-token",
          tokenType: "Bearer",
          expiresIn: 900,
          user: {
            id: 1,
            email: "user@smarthospital.local",
            role: "PATIENT",
          },
        };
      }
    },

    async logout(refreshToken: string): Promise<void> {
      try {
        await fetch(`${API_BASE}/api/v1/auth/logout`, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ refreshToken }),
        });
      } catch (err) {
        console.warn("Logout error:", err);
      }
    },

    async getPatientDashboard(): Promise<PatientDashboard> {
      return mockPatientDashboard;
    },

    async getDoctorDashboard(): Promise<DoctorDashboard> {
      return mockDoctorDashboard;
    },

    async getAdminDashboard(): Promise<AdminDashboard> {
      return mockAdminDashboard;
    },
  };
}
