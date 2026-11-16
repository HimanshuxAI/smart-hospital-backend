import type {
  AdminDashboard,
  DoctorDashboard,
  LoginInput,
  PatientDashboard,
  RegisterInput,
  TokenBundleResponse,
} from "./types";

export interface AuthAdapter {
  login(input: LoginInput): Promise<TokenBundleResponse>;
  register(input: RegisterInput): Promise<TokenBundleResponse>;
  refresh(refreshToken: string): Promise<TokenBundleResponse>;
  logout(refreshToken: string): Promise<void>;
}

export interface DashboardAdapter {
  getPatientDashboard(): Promise<PatientDashboard>;
  getDoctorDashboard(): Promise<DoctorDashboard>;
  getAdminDashboard(): Promise<AdminDashboard>;
}

export type AppAdapter = AuthAdapter & DashboardAdapter;
