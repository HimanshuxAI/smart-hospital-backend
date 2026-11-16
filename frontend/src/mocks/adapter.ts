import type { AppAdapter } from "../api/adapter";
import type {
  AdminDashboard,
  
  DoctorDashboard,
  LoginInput,
  PatientDashboard,
  RegisterInput,
  TokenBundleResponse,
} from "../api/types";
import { mockAdminDashboard, mockDoctorDashboard, mockPatientDashboard } from "./data";

export function createDemoAdapter(): AppAdapter {
  return {
    async login(input: LoginInput): Promise<TokenBundleResponse> {
      let role: "PATIENT" | "DOCTOR" | "ADMIN" = "PATIENT";
      if (input.email.includes("doctor")) role = "DOCTOR";
      if (input.email.includes("admin")) role = "ADMIN";

      return {
        accessToken: "demo-access-token",
        refreshToken: "demo-refresh-token",
        tokenType: "Bearer",
        expiresIn: 900,
        user: {
          id: 1,
          email: input.email,
          role,
        },
      };
    },
    async register(input: RegisterInput): Promise<TokenBundleResponse> {
      return {
        accessToken: "demo-access-token",
        refreshToken: "demo-refresh-token",
        tokenType: "Bearer",
        expiresIn: 900,
        user: {
          id: 2,
          email: input.email,
          role: "PATIENT",
        },
      };
    },
    async refresh(): Promise<TokenBundleResponse> {
      return {
        accessToken: "demo-access-token",
        refreshToken: "demo-refresh-token",
        tokenType: "Bearer",
        expiresIn: 900,
        user: {
          id: 1,
          email: "demo@hospital.local",
          role: "PATIENT",
        },
      };
    },
    async logout(): Promise<void> {},
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
