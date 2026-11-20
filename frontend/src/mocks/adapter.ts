import type { AppAdapter } from "../api/adapter";
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
} from "../api/types";
import {
  buildMockSlots,
  mockAdminDashboard,
  mockAdminUsers,
  mockAppointments,
  mockDepartments,
  mockDoctorDashboard,
  mockDoctors,
  mockPatientDashboard,
  mockQueueEntries,
} from "./data";

const DEMO_PATIENT_ID = 3;
const DEMO_DOCTOR_ID = 11;
const DEMO_DOCTOR_USER_ID = 2;
const DEMO_SESSION_KEY = "smarthospital.demo-session";

type DemoRole = "PATIENT" | "DOCTOR" | "ADMIN";
type DemoSession = { id: number; role: DemoRole; email: string };

const DEFAULT_DEMO_SESSION: DemoSession = {
  id: DEMO_PATIENT_ID,
  role: "PATIENT",
  email: "patient@hospital.local",
};

function recallDemoSession(): DemoSession {
  try {
    const stored = sessionStorage.getItem(DEMO_SESSION_KEY);
    if (stored) {
      const parsed = JSON.parse(stored) as Partial<DemoSession>;
      if (
        (parsed.role === "PATIENT" ||
          parsed.role === "DOCTOR" ||
          parsed.role === "ADMIN") &&
        typeof parsed.id === "number" &&
        typeof parsed.email === "string"
      ) {
        return { id: parsed.id, role: parsed.role, email: parsed.email };
      }
    }
  } catch {
    // sessionStorage or JSON may be unavailable; fall back to the patient demo.
  }
  return DEFAULT_DEMO_SESSION;
}

function rememberDemoSession(session: DemoSession): void {
  try {
    sessionStorage.setItem(DEMO_SESSION_KEY, JSON.stringify(session));
  } catch {
    // The in-memory session remains usable without storage.
  }
}

function clearDemoSession(): void {
  try {
    sessionStorage.removeItem(DEMO_SESSION_KEY);
  } catch {
    // Nothing to clear.
  }
}

export function createDemoAdapter(): AppAdapter {
  const users: AdminUser[] = mockAdminUsers.map((user) => ({ ...user }));
  const doctors: DirectoryDoctor[] = mockDoctors.map((doctor) => ({ ...doctor }));
  const appointments: Appointment[] = mockAppointments.map((appointment) => ({
    ...appointment,
  }));
  const queueEntries: QueueEntry[] = mockQueueEntries.map((entry) => ({ ...entry }));
  const slots: ScheduleSlot[] = [];

  let session: DemoSession = recallDemoSession();

  const now = () => new Date().toISOString();

  const findQueueEntry = (entryId: number): QueueEntry => {
    const entry = queueEntries.find((candidate) => candidate.queueEntryId === entryId);
    if (!entry) {
      throw new Error(`Queue entry ${entryId} was not found`);
    }
    return entry;
  };

  const repositionQueue = (doctorId: number | null) => {
    const waiting = queueEntries
      .filter((entry) => entry.queueStatus === "WAITING" && entry.doctorId === doctorId)
      .sort((left, right) => left.arrivedAt.localeCompare(right.arrivedAt));
    waiting.forEach((entry, index) => {
      entry.position = index + 1;
    });
  };

  const profileFor = async (doctorId: number): Promise<DoctorProfile> => {
    const doctor = doctors.find((candidate) => candidate.id === doctorId);
    if (!doctor) {
      throw new Error(`Doctor ${doctorId} was not found`);
    }
    return {
      id: doctor.id,
      userId: doctor.userId,
      email: `${doctor.fullName.toLowerCase().replace(/[^a-z]+/g, ".")}@hospital.local`,
      fullName: doctor.fullName,
      phone: null,
      role: "DOCTOR",
      active: true,
      departmentId: doctor.departmentId,
      departmentName: doctor.departmentName,
      specialty: doctor.specialty,
      licenseNumber: `LIC-${doctor.id}`,
      averageConsultationMinutes: doctor.averageConsultationMinutes,
      profileActive: true,
    };
  };

  return {
    async login(input: LoginInput): Promise<TokenBundleResponse> {
      let role: DemoRole = "PATIENT";
      if (input.email.includes("doctor")) role = "DOCTOR";
      if (input.email.includes("admin")) role = "ADMIN";
      session = {
        id: role === "ADMIN" ? 1 : role === "DOCTOR" ? DEMO_DOCTOR_USER_ID : DEMO_PATIENT_ID,
        role,
        email: input.email,
      };
      rememberDemoSession(session);

      return {
        accessToken: "demo-access-token",
        refreshToken: "demo-refresh-token",
        tokenType: "Bearer",
        expiresIn: 900,
        user: {
          id: session.id,
          email: session.email,
          role: session.role,
        },
      };
    },

    async register(input: RegisterInput): Promise<TokenBundleResponse> {
      session = { id: DEMO_PATIENT_ID, role: "PATIENT", email: input.email };
      rememberDemoSession(session);
      return {
        accessToken: "demo-access-token",
        refreshToken: "demo-refresh-token",
        tokenType: "Bearer",
        expiresIn: 900,
        user: {
          id: DEMO_PATIENT_ID,
          email: input.email,
          role: "PATIENT",
        },
      };
    },

    async refresh(): Promise<TokenBundleResponse> {
      session = recallDemoSession();
      return {
        accessToken: "demo-access-token",
        refreshToken: "demo-refresh-token",
        tokenType: "Bearer",
        expiresIn: 900,
        user: {
          id: session.id,
          email: session.email,
          role: session.role,
        },
      };
    },

    async logout(): Promise<void> {
      clearDemoSession();
      session = DEFAULT_DEMO_SESSION;
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

    async getDepartments(): Promise<Department[]> {
      return mockDepartments.map((department) => ({ ...department }));
    },

    async getDoctors(): Promise<DirectoryDoctor[]> {
      return doctors.map((doctor) => ({ ...doctor }));
    },

    async getDoctor(doctorId: number): Promise<DirectoryDoctor> {
      const doctor = doctors.find((candidate) => candidate.id === doctorId);
      if (!doctor) {
        throw new Error(`Doctor ${doctorId} was not found`);
      }
      return { ...doctor };
    },

    async getSlots(doctorId: number, date: string): Promise<ScheduleSlot[]> {
      const existing = slots.filter((slot) => slot.doctorId === doctorId && slot.startsAt.startsWith(date));
      if (existing.length > 0) {
        return existing.map((slot) => ({ ...slot }));
      }
      return buildMockSlots(doctorId, date);
    },

    async getAdminSlots(doctorId: number, date: string): Promise<ScheduleSlot[]> {
      const existing = slots.filter(
        (slot) => slot.doctorId === doctorId && slot.startsAt.startsWith(date),
      );
      if (existing.length > 0) {
        return existing.map((slot) => ({ ...slot }));
      }
      return buildMockSlots(doctorId, date);
    },

    async getQueue(query?: QueueQuery): Promise<QueueEntry[]> {
      const scoped =
        session.role === "PATIENT"
          ? queueEntries.filter((entry) => entry.patientId === session.id)
          : session.role === "DOCTOR"
            ? queueEntries.filter((entry) => entry.doctorId === DEMO_DOCTOR_ID)
            : queueEntries;

      return scoped
        .filter((entry) => (query?.doctorId === undefined ? true : entry.doctorId === query.doctorId))
        .filter((entry) => (query?.date === undefined ? true : entry.queueDate === query.date))
        .map((entry) => ({ ...entry }));
    },

    async getQueueEntry(entryId: number): Promise<QueueEntry> {
      return { ...findQueueEntry(entryId) };
    },

    async getAppointments(query?: AppointmentQuery): Promise<Appointment[]> {
      const scoped =
        session.role === "PATIENT"
          ? appointments.filter((appointment) => appointment.patientId === session.id)
          : session.role === "DOCTOR"
            ? appointments.filter((appointment) => appointment.doctorId === DEMO_DOCTOR_ID)
            : appointments;

      return scoped
        .filter((appointment) =>
          query?.doctorId === undefined ? true : appointment.doctorId === query.doctorId,
        )
        .filter((appointment) =>
          query?.date === undefined ? true : appointment.appointmentDate === query.date,
        )
        .map((appointment) => ({ ...appointment }));
    },

    async getAdminUsers(): Promise<AdminUser[]> {
      return users.map((user) => ({ ...user }));
    },

    async setUserActivation(userId: number, active: boolean): Promise<AdminUser> {
      const user = users.find((candidate) => candidate.id === userId);
      if (!user) {
        throw new Error(`User ${userId} was not found`);
      }
      user.active = active;
      return { ...user };
    },

    async getOwnDoctorProfile(): Promise<DoctorProfile> {
      return profileFor(DEMO_DOCTOR_ID);
    },

    async getDoctorProfile(doctorId: number): Promise<DoctorProfile> {
      return profileFor(doctorId);
    },

    async updateDoctorProfile(
      doctorId: number,
      input: UpdateDoctorProfileInput,
    ): Promise<DoctorProfile> {
      const doctor = doctors.find((candidate) => candidate.id === doctorId);
      if (!doctor) {
        throw new Error(`Doctor ${doctorId} was not found`);
      }
      if (input.fullName !== undefined) doctor.fullName = input.fullName;
      if (input.specialty !== undefined) doctor.specialty = input.specialty;
      if (input.averageConsultationMinutes !== undefined) {
        doctor.averageConsultationMinutes = input.averageConsultationMinutes;
      }
      if (input.departmentId !== undefined) {
        const department = mockDepartments.find(
          (candidate) => candidate.id === input.departmentId,
        );
        doctor.departmentId = department?.id ?? doctor.departmentId;
        doctor.departmentName = department?.name ?? doctor.departmentName;
      }
      return profileFor(doctorId);
    },

    async createDoctor(input: CreateDoctorInput): Promise<DoctorProfile> {
      const department = mockDepartments.find(
        (candidate) => candidate.id === input.departmentId,
      );
      const doctor: DirectoryDoctor = {
        id: Math.max(...doctors.map((candidate) => candidate.id)) + 1,
        userId: Math.max(...users.map((candidate) => candidate.id)) + 1,
        fullName: input.fullName,
        departmentId: input.departmentId,
        departmentName: department?.name ?? null,
        specialty: input.specialty ?? null,
        averageConsultationMinutes: input.averageConsultationMinutes ?? 30,
      };
      doctors.push(doctor);
      users.push({
        id: doctor.userId,
        email: input.email,
        fullName: input.fullName,
        role: "DOCTOR",
        active: true,
        phone: input.phone ?? null,
      });
      return profileFor(doctor.id);
    },

    async createSlot(input: CreateSlotInput): Promise<ScheduleSlot> {
      const slot: ScheduleSlot = {
        id: slots.length + 9100,
        doctorId: input.doctorId,
        startsAt: input.startsAt,
        endsAt: input.endsAt,
        reasonLabel: input.reasonLabel ?? null,
        status: "OPEN",
      };
      slots.push(slot);
      return { ...slot };
    },

    async cancelSlot(slotId: number): Promise<ScheduleSlot> {
      const slot = slots.find((candidate) => candidate.id === slotId);
      if (!slot) {
        throw new Error(`Slot ${slotId} was not found`);
      }
      slot.status = "CANCELLED";
      return { ...slot };
    },

    async bookAppointment(input: BookAppointmentInput): Promise<Appointment> {
      const slot = slots.find((candidate) => candidate.id === input.slotId);
      const doctor = doctors.find((candidate) => candidate.id === input.doctorId);
      const startsAt = slot?.startsAt ?? new Date(Date.now() + 86400000).toISOString();
      const endsAt = slot?.endsAt ?? new Date(Date.now() + 86400000 + 1800000).toISOString();
      const appointment: Appointment = {
        id: Math.max(...appointments.map((entry) => entry.id)) + 1,
        patientId: session.id,
        doctorId: input.doctorId ?? doctor?.id ?? DEMO_DOCTOR_ID,
        slotId: input.slotId,
        appointmentDate: startsAt.slice(0, 10),
        startsAt,
        endsAt,
        visitReason: input.visitReason,
        status: "BOOKED",
        checkedInAt: null,
        completedAt: null,
        cancelledAt: null,
        cancellationReason: null,
      };
      appointments.push(appointment);
      if (slot) {
        slot.status = "BOOKED";
      }
      return { ...appointment };
    },

    async cancelAppointment(appointmentId: number, reason: string): Promise<Appointment> {
      const appointment = appointments.find((candidate) => candidate.id === appointmentId);
      if (!appointment) {
        throw new Error(`Appointment ${appointmentId} was not found`);
      }
      appointment.status = "CANCELLED";
      appointment.cancelledAt = now();
      appointment.cancellationReason = reason;
      return { ...appointment };
    },

    async checkInAppointment(
      appointmentId: number,
    ): Promise<{ queueEntryId: number; tokenNumber: number }> {
      const appointment = appointments.find((candidate) => candidate.id === appointmentId);
      if (!appointment) {
        throw new Error(`Appointment ${appointmentId} was not found`);
      }
      appointment.status = "CHECKED_IN";
      appointment.checkedInAt = now();

      const existing = queueEntries.find(
        (entry) => entry.appointmentId === appointmentId,
      );
      if (existing) {
        return { queueEntryId: existing.queueEntryId, tokenNumber: existing.tokenNumber };
      }

      const tokenNumber = Math.max(...queueEntries.map((entry) => entry.tokenNumber)) + 1;
      const entry: QueueEntry = {
        queueEntryId: Math.max(...queueEntries.map((item) => item.queueEntryId)) + 1,
        appointmentId: appointment.id,
        patientId: appointment.patientId,
        doctorId: appointment.doctorId,
        queueDate: appointment.appointmentDate,
        token: tokenNumber,
        tokenNumber,
        queueStatus: "WAITING",
        triageLevel: "NORMAL",
        position: 0,
        arrivedAt: now(),
        calledAt: null,
        servedAt: null,
        completedAt: null,
      };
      queueEntries.push(entry);
      repositionQueue(entry.doctorId);
      return { queueEntryId: entry.queueEntryId, tokenNumber: entry.tokenNumber };
    },

    async classifyQueueEntry(
      entryId: number,
      triageLevel: EmergencyTier,
    ): Promise<QueueEntry> {
      const entry = findQueueEntry(entryId);
      entry.triageLevel = triageLevel;
      repositionQueue(entry.doctorId);
      return { ...entry };
    },

    async overrideQueueEntry(
      entryId: number,
      triageLevel: EmergencyTier,
      reason: string,
    ): Promise<QueueEntry> {
      const entry = findQueueEntry(entryId);
      entry.triageLevel = triageLevel;
      void reason;
      repositionQueue(entry.doctorId);
      return { ...entry };
    },

    async callQueueEntry(entryId: number): Promise<QueueEntry> {
      const entry = findQueueEntry(entryId);
      entry.queueStatus = "CALLED";
      entry.calledAt = now();
      repositionQueue(entry.doctorId);
      return { ...entry };
    },

    async serveQueueEntry(entryId: number): Promise<QueueEntry> {
      const entry = findQueueEntry(entryId);
      entry.queueStatus = "IN_CONSULTATION";
      entry.servedAt = now();
      repositionQueue(entry.doctorId);
      return { ...entry };
    },

    async completeQueueEntry(entryId: number): Promise<QueueEntry> {
      const entry = findQueueEntry(entryId);
      entry.queueStatus = "COMPLETED";
      entry.completedAt = now();
      repositionQueue(entry.doctorId);
      return { ...entry };
    },

    async skipQueueEntry(entryId: number, reason: string): Promise<QueueEntry> {
      const entry = findQueueEntry(entryId);
      entry.queueStatus = "SKIPPED";
      void reason;
      repositionQueue(entry.doctorId);
      return { ...entry };
    },
  };
}

export const DEMO_IDENTIFIERS = {
  patientId: DEMO_PATIENT_ID,
  doctorId: DEMO_DOCTOR_ID,
} as const;
