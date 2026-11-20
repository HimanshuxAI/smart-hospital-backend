import { lazy, Suspense, type ReactNode } from "react";
import { createBrowserRouter, Navigate } from "react-router-dom";
import type { UserRole } from "../api/types";
import { RequireAuth, RequireRole } from "../auth/guards";
import { LoginPage } from "../features/auth/LoginPage";
import { RegisterPage } from "../features/auth/RegisterPage";
import { ForbiddenPage } from "../features/shared/ForbiddenPage";
import { NotFoundPage } from "../features/shared/NotFoundPage";

const PatientDashboard = lazy(() =>
  import("../features/patient/PatientDashboardPage").then((m) => ({ default: m.PatientDashboardPage }))
);
const PatientDoctors = lazy(() =>
  import("../features/patient/PatientDoctorsPage").then((m) => ({ default: m.PatientDoctorsPage }))
);
const PatientAppointments = lazy(() =>
  import("../features/patient/PatientAppointmentsPage").then((m) => ({
    default: m.PatientAppointmentsPage,
  }))
);
const PatientQueue = lazy(() =>
  import("../features/patient/PatientQueuePage").then((m) => ({ default: m.PatientQueuePage }))
);
const DoctorDashboard = lazy(() =>
  import("../features/doctor/DoctorDashboardPage").then((m) => ({ default: m.DoctorDashboardPage }))
);
const DoctorQueue = lazy(() =>
  import("../features/doctor/DoctorQueuePage").then((m) => ({ default: m.DoctorQueuePage }))
);
const DoctorSchedule = lazy(() =>
  import("../features/doctor/DoctorSchedulePage").then((m) => ({ default: m.DoctorSchedulePage }))
);
const DoctorPatients = lazy(() =>
  import("../features/doctor/DoctorPatientsPage").then((m) => ({ default: m.DoctorPatientsPage }))
);
const AdminDashboard = lazy(() =>
  import("../features/admin/AdminDashboardPage").then((m) => ({ default: m.AdminDashboardPage }))
);
const AdminUsers = lazy(() =>
  import("../features/admin/AdminUsersPage").then((m) => ({ default: m.AdminUsersPage }))
);
const AdminDoctors = lazy(() =>
  import("../features/admin/AdminDoctorsPage").then((m) => ({ default: m.AdminDoctorsPage }))
);
const AdminSchedules = lazy(() =>
  import("../features/admin/AdminSchedulesPage").then((m) => ({ default: m.AdminSchedulesPage }))
);

function protectedRoute(role: UserRole, element: ReactNode, loadingLabel: string) {
  return (
    <RequireAuth>
      <RequireRole role={role}>
        <Suspense fallback={<div className="page-state">Loading {loadingLabel}...</div>}>
          {element}
        </Suspense>
      </RequireRole>
    </RequireAuth>
  );
}

export const router = createBrowserRouter([
  {
    path: "/",
    element: <Navigate to="/login" replace />,
  },
  {
    path: "/login",
    element: <LoginPage />,
  },
  {
    path: "/register",
    element: <RegisterPage />,
  },
  {
    path: "/patient",
    element: protectedRoute("PATIENT", <PatientDashboard />, "dashboard"),
  },
  {
    path: "/patient/doctors",
    element: protectedRoute("PATIENT", <PatientDoctors />, "doctors"),
  },
  {
    path: "/patient/appointments",
    element: protectedRoute("PATIENT", <PatientAppointments />, "appointments"),
  },
  {
    path: "/patient/queue",
    element: protectedRoute("PATIENT", <PatientQueue />, "queue"),
  },
  {
    path: "/doctor",
    element: protectedRoute("DOCTOR", <DoctorDashboard />, "station"),
  },
  {
    path: "/doctor/queue",
    element: protectedRoute("DOCTOR", <DoctorQueue />, "queue"),
  },
  {
    path: "/doctor/schedule",
    element: protectedRoute("DOCTOR", <DoctorSchedule />, "schedule"),
  },
  {
    path: "/doctor/patients",
    element: protectedRoute("DOCTOR", <DoctorPatients />, "patients"),
  },
  {
    path: "/admin",
    element: protectedRoute("ADMIN", <AdminDashboard />, "admin console"),
  },
  {
    path: "/admin/users",
    element: protectedRoute("ADMIN", <AdminUsers />, "users"),
  },
  {
    path: "/admin/doctors",
    element: protectedRoute("ADMIN", <AdminDoctors />, "doctors"),
  },
  {
    path: "/admin/schedules",
    element: protectedRoute("ADMIN", <AdminSchedules />, "schedules"),
  },
  {
    path: "/403",
    element: <ForbiddenPage />,
  },
  {
    path: "*",
    element: <NotFoundPage />,
  },
]);
