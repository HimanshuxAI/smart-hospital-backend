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

export interface ClinicalAdapter {
  getDepartments(): Promise<Department[]>;
  getDoctors(): Promise<DirectoryDoctor[]>;
  getDoctor(doctorId: number): Promise<DirectoryDoctor>;
  getSlots(doctorId: number, date: string): Promise<ScheduleSlot[]>;
  getAdminSlots(doctorId: number, date: string): Promise<ScheduleSlot[]>;
  getQueue(query?: QueueQuery): Promise<QueueEntry[]>;
  getQueueEntry(entryId: number): Promise<QueueEntry>;
  getAppointments(query?: AppointmentQuery): Promise<Appointment[]>;
  getAdminUsers(): Promise<AdminUser[]>;
  setUserActivation(userId: number, active: boolean): Promise<AdminUser>;
  getOwnDoctorProfile(): Promise<DoctorProfile>;
  getDoctorProfile(doctorId: number): Promise<DoctorProfile>;
  updateDoctorProfile(
    doctorId: number,
    input: UpdateDoctorProfileInput,
  ): Promise<DoctorProfile>;
  createDoctor(input: CreateDoctorInput): Promise<DoctorProfile>;
  createSlot(input: CreateSlotInput): Promise<ScheduleSlot>;
  cancelSlot(slotId: number): Promise<ScheduleSlot>;
  bookAppointment(input: BookAppointmentInput): Promise<Appointment>;
  cancelAppointment(appointmentId: number, reason: string): Promise<Appointment>;
  checkInAppointment(appointmentId: number): Promise<{ queueEntryId: number; tokenNumber: number }>;
  classifyQueueEntry(entryId: number, triageLevel: EmergencyTier): Promise<QueueEntry>;
  overrideQueueEntry(
    entryId: number,
    triageLevel: EmergencyTier,
    reason: string,
  ): Promise<QueueEntry>;
  callQueueEntry(entryId: number): Promise<QueueEntry>;
  serveQueueEntry(entryId: number): Promise<QueueEntry>;
  completeQueueEntry(entryId: number): Promise<QueueEntry>;
  skipQueueEntry(entryId: number, reason: string): Promise<QueueEntry>;
}

export type AppAdapter = AuthAdapter & DashboardAdapter & ClinicalAdapter;
