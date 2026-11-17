export type UserRole = "PATIENT" | "DOCTOR" | "ADMIN";

export type AuthUser = {
  id: number;
  email: string;
  role: UserRole;
};

export type TokenBundleResponse = {
  accessToken: string;
  refreshToken: string;
  tokenType: "Bearer";
  expiresIn: number;
  user: AuthUser;
};

export type LoginInput = {
  email: string;
  password: string;
};

export type RegisterInput = LoginInput & {
  fullName: string;
  phone: string;
  dateOfBirth: string;
  gender?: string;
  address?: string;
  emergencyContact?: string;
};

export type EmergencyTier = "NORMAL" | "MEDIUM" | "HIGH" | "CRITICAL";

export type QueueStatus =
  | "WAITING"
  | "CALLED"
  | "IN_CONSULTATION"
  | "COMPLETED"
  | "SKIPPED"
  | "CANCELLED";

export type PatientDashboard = {
  patientName: string;
  upcomingAppointment: {
    id: number;
    doctorName: string;
    departmentName: string;
    startsAt: string;
    status: string;
  } | null;
  activeQueueEntry: {
    id: number;
    tokenNumber: number;
    queueStatus: QueueStatus;
    triageLevel: EmergencyTier;
    position: number;
    estimatedWaitMinutes: number;
    arrivedAt: string;
  } | null;
};

export type DoctorDashboard = {
  doctorName: string;
  todayAppointmentCount: number;
  waitingCount: number;
  inConsultationCount: number;
};

export type AdminDashboard = {
  totalUsers: number;
  totalDoctors: number;
  totalDepartments: number;
  appointmentsToday: number;
  queueEntriesToday: number;
};

export type Department = {
  id: number;
  name: string;
  description: string | null;
  active: boolean;
};

export type DirectoryDoctor = {
  id: number;
  userId: number;
  fullName: string;
  departmentId: number | null;
  departmentName: string | null;
  specialty: string | null;
  averageConsultationMinutes: number;
};

export type ScheduleSlotStatus = "AVAILABLE" | "BOOKED" | "CANCELLED";

export type ScheduleSlot = {
  id: number;
  doctorId: number;
  startsAt: string;
  endsAt: string;
  reasonLabel: string | null;
  status: ScheduleSlotStatus | string;
};

export type QueueEntry = {
  queueEntryId: number;
  appointmentId: number | null;
  patientId: number;
  doctorId: number | null;
  queueDate: string;
  token: number;
  tokenNumber: number;
  queueStatus: QueueStatus;
  triageLevel: EmergencyTier;
  position: number;
  arrivedAt: string;
  calledAt: string | null;
  servedAt: string | null;
  completedAt: string | null;
};

export type AppointmentStatus =
  | "BOOKED"
  | "CONFIRMED"
  | "CHECKED_IN"
  | "COMPLETED"
  | "CANCELLED"
  | string;

export type Appointment = {
  id: number;
  patientId: number;
  doctorId: number;
  slotId: number | null;
  appointmentDate: string;
  startsAt: string;
  endsAt: string;
  visitReason: string | null;
  status: AppointmentStatus;
  checkedInAt: string | null;
  completedAt: string | null;
  cancelledAt: string | null;
  cancellationReason: string | null;
};

export type AdminUser = {
  id: number;
  email: string;
  fullName: string;
  role: UserRole;
  active: boolean;
  phone: string | null;
};

export type DoctorProfile = {
  id: number;
  userId: number;
  email: string;
  fullName: string;
  phone: string | null;
  role: UserRole;
  active: boolean;
  departmentId: number | null;
  departmentName: string | null;
  specialty: string | null;
  licenseNumber: string | null;
  averageConsultationMinutes: number;
  profileActive: boolean;
};

export type CreateDoctorInput = {
  email: string;
  password: string;
  fullName: string;
  phone?: string;
  departmentId: number;
  specialty?: string;
  licenseNumber?: string;
  averageConsultationMinutes?: number;
};

export type UpdateDoctorProfileInput = {
  fullName?: string;
  phone?: string;
  departmentId?: number;
  specialty?: string;
  licenseNumber?: string;
  averageConsultationMinutes?: number;
  active?: boolean;
};

export type CreateSlotInput = {
  doctorId: number;
  startsAt: string;
  endsAt: string;
  reasonLabel?: string;
};

export type BookAppointmentInput = {
  slotId: number;
  doctorId?: number | undefined;
  visitReason: string;
};

export type QueueQuery = {
  doctorId?: number | undefined;
  date?: string | undefined;
};

export type AppointmentQuery = {
  doctorId?: number | undefined;
  date?: string | undefined;
};
