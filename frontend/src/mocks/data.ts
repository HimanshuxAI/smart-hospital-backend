import type {
  AdminDashboard,
  AdminUser,
  Appointment,
  Department,
  DirectoryDoctor,
  DoctorDashboard,
  PatientDashboard,
  QueueEntry,
  ScheduleSlot,
} from "../api/types";

const DAY = 86400000;

function startOfToday(): Date {
  const now = new Date();
  return new Date(now.getFullYear(), now.getMonth(), now.getDate());
}

export function isoDate(date: Date): string {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

export const mockPatientDashboard: PatientDashboard = {
  patientName: "John Doe",
  upcomingAppointment: {
    id: 101,
    doctorName: "Dr. Sarah Smith",
    departmentName: "Cardiology",
    startsAt: new Date(Date.now() + DAY).toISOString(),
    status: "CONFIRMED",
  },
  activeQueueEntry: {
    id: 501,
    tokenNumber: 14,
    queueStatus: "WAITING",
    triageLevel: "HIGH",
    position: 3,
    estimatedWaitMinutes: 18,
    arrivedAt: new Date(Date.now() - 1200000).toISOString(),
  },
};

export const mockDoctorDashboard: DoctorDashboard = {
  doctorName: "Dr. Sarah Smith",
  todayAppointmentCount: 12,
  waitingCount: 5,
  inConsultationCount: 1,
};

export const mockAdminDashboard: AdminDashboard = {
  totalUsers: 142,
  totalDoctors: 18,
  totalDepartments: 6,
  appointmentsToday: 45,
  queueEntriesToday: 38,
};

export const mockDepartments: Department[] = [
  {
    id: 1,
    name: "Cardiology",
    description: "Heart and vascular care, including hypertension and arrhythmia clinics.",
    active: true,
  },
  {
    id: 2,
    name: "Orthopaedics",
    description: "Bone, joint, and sports injury services.",
    active: true,
  },
  {
    id: 3,
    name: "Paediatrics",
    description: "Child health, immunisation, and growth monitoring.",
    active: true,
  },
  {
    id: 4,
    name: "Dermatology",
    description: "Skin, hair, and nail conditions.",
    active: true,
  },
  {
    id: 5,
    name: "Neurology",
    description: "Brain, spine, and nervous system disorders.",
    active: true,
  },
  {
    id: 6,
    name: "General Medicine",
    description: "First-line adult care and chronic disease follow-up.",
    active: true,
  },
];

export const mockDoctors: DirectoryDoctor[] = [
  {
    id: 11,
    userId: 101,
    fullName: "Dr. Sarah Smith",
    departmentId: 1,
    departmentName: "Cardiology",
    specialty: "Interventional Cardiology",
    averageConsultationMinutes: 30,
  },
  {
    id: 12,
    userId: 102,
    fullName: "Dr. Michael Chen",
    departmentId: 1,
    departmentName: "Cardiology",
    specialty: "Electrophysiology",
    averageConsultationMinutes: 30,
  },
  {
    id: 13,
    userId: 103,
    fullName: "Dr. Aisha Rahman",
    departmentId: 2,
    departmentName: "Orthopaedics",
    specialty: "Sports Injuries",
    averageConsultationMinutes: 25,
  },
  {
    id: 14,
    userId: 104,
    fullName: "Dr. Peter Novak",
    departmentId: 3,
    departmentName: "Paediatrics",
    specialty: "Neonatal Care",
    averageConsultationMinutes: 20,
  },
  {
    id: 15,
    userId: 105,
    fullName: "Dr. Grace Mbeki",
    departmentId: 4,
    departmentName: "Dermatology",
    specialty: "Cosmetic Dermatology",
    averageConsultationMinutes: 20,
  },
  {
    id: 16,
    userId: 106,
    fullName: "Dr. Luis Ortega",
    departmentId: 5,
    departmentName: "Neurology",
    specialty: "Stroke Medicine",
    averageConsultationMinutes: 40,
  },
  {
    id: 17,
    userId: 107,
    fullName: "Dr. Hana Ito",
    departmentId: 6,
    departmentName: "General Medicine",
    specialty: "Chronic Disease Care",
    averageConsultationMinutes: 15,
  },
  {
    id: 18,
    userId: 108,
    fullName: "Dr. Oliver Grant",
    departmentId: 6,
    departmentName: "General Medicine",
    specialty: "Preventive Screening",
    averageConsultationMinutes: 15,
  },
];

export const mockAdminUsers: AdminUser[] = [
  {
    id: 1,
    email: "admin@hospital.local",
    fullName: "Avery Admin",
    role: "ADMIN",
    active: true,
    phone: "+1 555 0100",
  },
  {
    id: 2,
    email: "doctor@hospital.local",
    fullName: "Sarah Smith",
    role: "DOCTOR",
    active: true,
    phone: "+1 555 0101",
  },
  {
    id: 3,
    email: "patient@hospital.local",
    fullName: "John Doe",
    role: "PATIENT",
    active: true,
    phone: "+1 555 0102",
  },
  {
    id: 4,
    email: "m.chen@hospital.local",
    fullName: "Michael Chen",
    role: "DOCTOR",
    active: true,
    phone: "+1 555 0103",
  },
  {
    id: 5,
    email: "a.rahman@hospital.local",
    fullName: "Aisha Rahman",
    role: "DOCTOR",
    active: false,
    phone: "+1 555 0104",
  },
  {
    id: 6,
    email: "p.novak@hospital.local",
    fullName: "Peter Novak",
    role: "DOCTOR",
    active: true,
    phone: "+1 555 0105",
  },
  {
    id: 7,
    email: "g.mbeki@hospital.local",
    fullName: "Grace Mbeki",
    role: "DOCTOR",
    active: true,
    phone: "+1 555 0106",
  },
  {
    id: 8,
    email: "l.ortega@hospital.local",
    fullName: "Luis Ortega",
    role: "DOCTOR",
    active: true,
    phone: "+1 555 0107",
  },
];

export const mockAppointments: Appointment[] = [
  {
    id: 101,
    patientId: 3,
    doctorId: 11,
    slotId: 9001,
    appointmentDate: isoDate(new Date(startOfToday().getTime() + DAY)),
    startsAt: new Date(startOfToday().getTime() + DAY + 9 * 3600000).toISOString(),
    endsAt: new Date(startOfToday().getTime() + DAY + 9.5 * 3600000).toISOString(),
    visitReason: "Routine hypertension review",
    status: "CONFIRMED",
    checkedInAt: null,
    completedAt: null,
    cancelledAt: null,
    cancellationReason: null,
  },
  {
    id: 102,
    patientId: 3,
    doctorId: 17,
    slotId: 9002,
    appointmentDate: isoDate(new Date(startOfToday().getTime() + 3 * DAY)),
    startsAt: new Date(startOfToday().getTime() + 3 * DAY + 11 * 3600000).toISOString(),
    endsAt: new Date(startOfToday().getTime() + 3 * DAY + 11.25 * 3600000).toISOString(),
    visitReason: "Follow-up after chest pain",
    status: "BOOKED",
    checkedInAt: null,
    completedAt: null,
    cancelledAt: null,
    cancellationReason: null,
  },
  {
    id: 103,
    patientId: 3,
    doctorId: 11,
    slotId: 9003,
    appointmentDate: isoDate(new Date(startOfToday().getTime() - 6 * DAY)),
    startsAt: new Date(startOfToday().getTime() - 6 * DAY + 10 * 3600000).toISOString(),
    endsAt: new Date(startOfToday().getTime() - 6 * DAY + 10.5 * 3600000).toISOString(),
    visitReason: "ECG review",
    status: "COMPLETED",
    checkedInAt: new Date(startOfToday().getTime() - 6 * DAY + 9.5 * 3600000).toISOString(),
    completedAt: new Date(startOfToday().getTime() - 6 * DAY + 10.5 * 3600000).toISOString(),
    cancelledAt: null,
    cancellationReason: null,
  },
  {
    id: 201,
    patientId: 4,
    doctorId: 11,
    slotId: 9010,
    appointmentDate: isoDate(startOfToday()),
    startsAt: new Date(startOfToday().getTime() + 8 * 3600000).toISOString(),
    endsAt: new Date(startOfToday().getTime() + 8.5 * 3600000).toISOString(),
    visitReason: "Palpitations",
    status: "CONFIRMED",
    checkedInAt: null,
    completedAt: null,
    cancelledAt: null,
    cancellationReason: null,
  },
  {
    id: 202,
    patientId: 5,
    doctorId: 11,
    slotId: 9011,
    appointmentDate: isoDate(startOfToday()),
    startsAt: new Date(startOfToday().getTime() + 9 * 3600000).toISOString(),
    endsAt: new Date(startOfToday().getTime() + 9.5 * 3600000).toISOString(),
    visitReason: "Post-discharge follow-up",
    status: "BOOKED",
    checkedInAt: null,
    completedAt: null,
    cancelledAt: null,
    cancellationReason: null,
  },
  {
    id: 203,
    patientId: 6,
    doctorId: 11,
    slotId: 9012,
    appointmentDate: isoDate(startOfToday()),
    startsAt: new Date(startOfToday().getTime() + 13 * 3600000).toISOString(),
    endsAt: new Date(startOfToday().getTime() + 13.5 * 3600000).toISOString(),
    visitReason: "Medication review",
    status: "CONFIRMED",
    checkedInAt: null,
    completedAt: null,
    cancelledAt: null,
    cancellationReason: null,
  },
];

export const mockQueueEntries: QueueEntry[] = [
  {
    queueEntryId: 501,
    appointmentId: 201,
    patientId: 4,
    doctorId: 11,
    queueDate: isoDate(startOfToday()),
    token: 501,
    tokenNumber: 12,
    queueStatus: "WAITING",
    triageLevel: "CRITICAL",
    position: 1,
    arrivedAt: new Date(Date.now() - 3600000).toISOString(),
    calledAt: null,
    servedAt: null,
    completedAt: null,
  },
  {
    queueEntryId: 502,
    appointmentId: 202,
    patientId: 5,
    doctorId: 11,
    queueDate: isoDate(startOfToday()),
    token: 502,
    tokenNumber: 13,
    queueStatus: "WAITING",
    triageLevel: "HIGH",
    position: 2,
    arrivedAt: new Date(Date.now() - 2400000).toISOString(),
    calledAt: null,
    servedAt: null,
    completedAt: null,
  },
  {
    queueEntryId: 503,
    appointmentId: 203,
    patientId: 6,
    doctorId: 11,
    queueDate: isoDate(startOfToday()),
    token: 503,
    tokenNumber: 14,
    queueStatus: "CALLED",
    triageLevel: "MEDIUM",
    position: 3,
    arrivedAt: new Date(Date.now() - 1800000).toISOString(),
    calledAt: new Date(Date.now() - 300000).toISOString(),
    servedAt: null,
    completedAt: null,
  },
  {
    queueEntryId: 504,
    appointmentId: null,
    patientId: 3,
    doctorId: null,
    queueDate: isoDate(startOfToday()),
    token: 504,
    tokenNumber: 15,
    queueStatus: "WAITING",
    triageLevel: "NORMAL",
    position: 4,
    arrivedAt: new Date(Date.now() - 900000).toISOString(),
    calledAt: null,
    servedAt: null,
    completedAt: null,
  },
  {
    queueEntryId: 505,
    appointmentId: null,
    patientId: 8,
    doctorId: 11,
    queueDate: isoDate(startOfToday()),
    token: 505,
    tokenNumber: 16,
    queueStatus: "IN_CONSULTATION",
    triageLevel: "NORMAL",
    position: 0,
    arrivedAt: new Date(Date.now() - 4200000).toISOString(),
    calledAt: new Date(Date.now() - 3600000).toISOString(),
    servedAt: new Date(Date.now() - 3300000).toISOString(),
    completedAt: null,
  },
];

export function buildMockSlots(doctorId: number, date: string): ScheduleSlot[] {
  const doctor = mockDoctors.find((entry) => entry.id === doctorId);
  const duration = doctor?.averageConsultationMinutes ?? 30;
  const dayStart = new Date(`${date}T00:00:00`);
  const slots: ScheduleSlot[] = [];

  for (let hour = 9; hour < 17; hour += 1) {
    const startsAt = new Date(dayStart.getTime() + hour * 3600000);
    const endsAt = new Date(startsAt.getTime() + duration * 60000);
    slots.push({
      id: doctorId * 100 + hour,
      doctorId,
      startsAt: startsAt.toISOString(),
      endsAt: endsAt.toISOString(),
      reasonLabel: hour % 2 === 0 ? "Morning clinic" : "Afternoon clinic",
      status: (hour + doctorId) % 3 === 0 ? "BOOKED" : "AVAILABLE",
    });
  }

  return slots;
}
