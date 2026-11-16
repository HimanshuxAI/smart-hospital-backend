import type { AdminDashboard, DoctorDashboard, PatientDashboard } from "../api/types";

export const mockPatientDashboard: PatientDashboard = {
  patientName: "John Doe",
  upcomingAppointment: {
    id: 101,
    doctorName: "Dr. Sarah Smith",
    departmentName: "Cardiology",
    startsAt: new Date(Date.now() + 86400000).toISOString(),
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
