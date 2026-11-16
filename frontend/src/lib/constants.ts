import type { UserRole } from "../api/types";

export type NavigationItem = {
  label: string;
  path: string;
  icon: "overview" | "doctor" | "appointment" | "queue" | "schedule" | "patients" | "operations" | "users" | "departments";
};

export const navigationByRole: Record<UserRole, NavigationItem[]> = {
  PATIENT: [
    { label: "Overview", path: "/patient", icon: "overview" },
    { label: "Find a doctor", path: "/patient/doctors", icon: "doctor" },
    { label: "My appointments", path: "/patient/appointments", icon: "appointment" },
    { label: "Queue token", path: "/patient/queue", icon: "queue" },
  ],
  DOCTOR: [
    { label: "Today", path: "/doctor", icon: "overview" },
    { label: "Live queue", path: "/doctor/queue", icon: "queue" },
    { label: "My schedule", path: "/doctor/schedule", icon: "schedule" },
    { label: "Patients", path: "/doctor/patients", icon: "patients" },
  ],
  ADMIN: [
    { label: "Operations", path: "/admin", icon: "operations" },
    { label: "Users", path: "/admin/users", icon: "users" },
    { label: "Doctors", path: "/admin/doctors", icon: "doctor" },
    { label: "Schedules", path: "/admin/schedules", icon: "schedule" },
  ],
};

export const roleLabels: Record<UserRole, string> = {
  PATIENT: "Patient portal",
  DOCTOR: "Doctor workspace",
  ADMIN: "Administration",
};
