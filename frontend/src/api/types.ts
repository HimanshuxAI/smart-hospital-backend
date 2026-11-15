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
