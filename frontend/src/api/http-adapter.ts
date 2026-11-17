import type { AppAdapter } from "./adapter";
import { ApiRequestError, createApiClient, type ApiClient } from "./client";
import { parseProblem } from "./problem";
import { createTokenStore, type TokenStore } from "./token-store";
import type {
  AdminDashboard,
  AdminUser,
  Appointment,
  AppointmentQuery,
  BookAppointmentInput,
  CreateDoctorInput,
  CreateSlotInput,
  Department,
  DirectoryDoctor,
  DoctorDashboard,
  DoctorProfile,
  EmergencyTier,
  LoginInput,
  PatientDashboard,
  QueueEntry,
  QueueQuery,
  RegisterInput,
  ScheduleSlot,
  TokenBundleResponse,
  UpdateDoctorProfileInput,
} from "./types";

export type HttpAdapterOptions = {
  baseUrl: string;
  tokens?: TokenStore;
  fetch?: typeof fetch;
  onSessionExpired?: () => void;
};

const jsonHeaders = {
  "Content-Type": "application/json",
  Accept: "application/json, application/problem+json",
};

function queryString(params: Record<string, string | number | undefined>): string {
  const search = new URLSearchParams();
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined && value !== "") {
      search.set(key, String(value));
    }
  }
  const serialized = search.toString();
  return serialized ? `?${serialized}` : "";
}

export function createHttpAdapter(options: HttpAdapterOptions): AppAdapter {
  const tokens = options.tokens ?? createTokenStore();
  const requestFetch = options.fetch ?? fetch;
  const baseUrl = options.baseUrl.replace(/\/$/, "");

  const postRaw = async <T>(path: string, body: unknown): Promise<T> => {
    const response = await requestFetch(`${baseUrl}${path}`, {
      method: "POST",
      headers: jsonHeaders,
      body: JSON.stringify(body),
    });

    if (!response.ok) {
      let payload: unknown;
      try {
        payload = await response.json();
      } catch {
        payload = undefined;
      }
      throw new ApiRequestError(parseProblem(response.status, payload, response.headers));
    }

    if (response.status === 204) {
      return undefined as T;
    }

    try {
      return (await response.json()) as T;
    } catch {
      return undefined as T;
    }
  };

  const refreshSession = async (refreshToken: string): Promise<TokenBundleResponse> => {
    const bundle = await postRaw<TokenBundleResponse>("/auth/refresh", { refreshToken });
    tokens.setAccessToken(bundle.accessToken);
    tokens.setRefreshToken(bundle.refreshToken);
    return bundle;
  };

  const client: ApiClient = createApiClient({
    baseUrl,
    tokens,
    fetch: requestFetch,
    refresh: () => {
      const refreshToken = tokens.getRefreshToken();
      if (!refreshToken) {
        return Promise.reject(new Error("No refresh token available"));
      }
      return refreshSession(refreshToken);
    },
    onSessionExpired: () => {
      tokens.clear();
      options.onSessionExpired?.();
    },
  });

  return {
    async login(input: LoginInput): Promise<TokenBundleResponse> {
      const bundle = await postRaw<TokenBundleResponse>("/auth/login", input);
      tokens.setAccessToken(bundle.accessToken);
      tokens.setRefreshToken(bundle.refreshToken);
      return bundle;
    },

    async register(input: RegisterInput): Promise<TokenBundleResponse> {
      const bundle = await postRaw<TokenBundleResponse>("/auth/register", input);
      tokens.setAccessToken(bundle.accessToken);
      tokens.setRefreshToken(bundle.refreshToken);
      return bundle;
    },

    async refresh(refreshToken: string): Promise<TokenBundleResponse> {
      return refreshSession(refreshToken);
    },

    async logout(refreshToken: string): Promise<void> {
      try {
        await postRaw<void>("/auth/logout", { refreshToken });
      } finally {
        tokens.clear();
      }
    },

    getPatientDashboard: () => client.request<PatientDashboard>("/dashboard/patient"),
    getDoctorDashboard: () => client.request<DoctorDashboard>("/dashboard/doctor"),
    getAdminDashboard: () => client.request<AdminDashboard>("/dashboard/admin"),

    getDepartments: () => client.request<Department[]>("/departments"),
    getDoctors: () => client.request<DirectoryDoctor[]>("/doctors"),
    getDoctor: (doctorId: number) => client.request<DirectoryDoctor>(`/doctors/${doctorId}`),

    getSlots: (doctorId: number, date: string) =>
      client.request<ScheduleSlot[]>(`/doctors/${doctorId}/slots${queryString({ date })}`),

    getAdminSlots: (doctorId: number, date: string) =>
      client.request<ScheduleSlot[]>(`/admin/slots${queryString({ doctorId, date })}`),

    getQueue: (query?: QueueQuery) =>
      client.request<QueueEntry[]>(
        `/queue${queryString({ doctorId: query?.doctorId, date: query?.date })}`,
      ),

    getQueueEntry: (entryId: number) => client.request<QueueEntry>(`/queue/${entryId}`),

    getAppointments: (query?: AppointmentQuery) =>
      client.request<Appointment[]>(
        `/appointments${queryString({ doctorId: query?.doctorId, date: query?.date })}`,
      ),

    getAdminUsers: () => client.request<AdminUser[]>("/admin/users"),

    setUserActivation: (userId: number, active: boolean) =>
      client.request<AdminUser>(`/admin/users/${userId}/activation`, {
        method: "PATCH",
        headers: jsonHeaders,
        body: JSON.stringify({ active }),
      }),

    getOwnDoctorProfile: () => client.request<DoctorProfile>("/doctors/profile"),

    getDoctorProfile: (doctorId: number) =>
      client.request<DoctorProfile>(`/admin/doctors/${doctorId}/profile`),

    updateDoctorProfile: (doctorId: number, input: UpdateDoctorProfileInput) =>
      client.request<DoctorProfile>(`/admin/doctors/${doctorId}/profile`, {
        method: "PUT",
        headers: jsonHeaders,
        body: JSON.stringify(input),
      }),

    createDoctor: (input: CreateDoctorInput) =>
      client.request<DoctorProfile>("/admin/doctors", {
        method: "POST",
        headers: jsonHeaders,
        body: JSON.stringify(input),
      }),

    createSlot: (input: CreateSlotInput) =>
      client.request<ScheduleSlot>("/admin/slots", {
        method: "POST",
        headers: jsonHeaders,
        body: JSON.stringify(input),
      }),

    cancelSlot: (slotId: number) =>
      client.request<ScheduleSlot>(`/admin/slots/${slotId}/cancel`, { method: "POST" }),

    bookAppointment: (input: BookAppointmentInput) =>
      client.request<Appointment>("/appointments", {
        method: "POST",
        headers: jsonHeaders,
        body: JSON.stringify(input),
      }),

    cancelAppointment: (appointmentId: number, reason: string) =>
      client.request<Appointment>(`/appointments/${appointmentId}/cancel`, {
        method: "POST",
        headers: jsonHeaders,
        body: JSON.stringify({ reason }),
      }),

    checkInAppointment: (appointmentId: number) =>
      client.request<{ queueEntryId: number; tokenNumber: number }>(
        `/appointments/${appointmentId}/check-in`,
        { method: "POST" },
      ),

    classifyQueueEntry: (entryId: number, triageLevel: EmergencyTier) =>
      client.request<QueueEntry>(`/queue/${entryId}/classify`, {
        method: "POST",
        headers: jsonHeaders,
        body: JSON.stringify({ triageLevel }),
      }),

    overrideQueueEntry: (entryId: number, triageLevel: EmergencyTier, reason: string) =>
      client.request<QueueEntry>(`/queue/${entryId}/override`, {
        method: "POST",
        headers: jsonHeaders,
        body: JSON.stringify({ triageLevel, reason }),
      }),

    callQueueEntry: (entryId: number) =>
      client.request<QueueEntry>(`/queue/${entryId}/call`, { method: "POST" }),

    serveQueueEntry: (entryId: number) =>
      client.request<QueueEntry>(`/queue/${entryId}/serve`, { method: "POST" }),

    completeQueueEntry: (entryId: number) =>
      client.request<QueueEntry>(`/queue/${entryId}/complete`, { method: "POST" }),

    skipQueueEntry: (entryId: number, reason: string) =>
      client.request<QueueEntry>(`/queue/${entryId}/skip`, {
        method: "POST",
        headers: jsonHeaders,
        body: JSON.stringify({ reason }),
      }),
  };
}
