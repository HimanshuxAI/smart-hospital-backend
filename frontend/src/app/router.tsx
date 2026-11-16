import { lazy, Suspense } from "react";
import { createBrowserRouter, Navigate } from "react-router-dom";
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
  import("../features/patient/PatientAppointmentsPage").then((m) => ({ default: m.PatientAppointmentsPage }))
);
const PatientQueue = lazy(() =>
  import("../features/patient/PatientQueuePage").then((m) => ({ default: m.PatientQueuePage }))
);

const DoctorDashboard = lazy(() =>
  import("../features/doctor/DoctorDashboardPage").then((m) => ({ default: m.DoctorDashboardPage }))
);
const AdminDashboard = lazy(() =>
  import("../features/admin/AdminDashboardPage").then((m) => ({ default: m.AdminDashboardPage }))
);

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
  
  // Patient subroutes
  {
    path: "/patient",
    element: (
      <RequireAuth>
        <RequireRole role="PATIENT">
          <Suspense fallback={<div className="page-state">Loading dashboard...</div>}>
            <PatientDashboard />
          </Suspense>
        </RequireRole>
      </RequireAuth>
    ),
  },
  {
    path: "/patient/doctors",
    element: (
      <RequireAuth>
        <RequireRole role="PATIENT">
          <Suspense fallback={<div className="page-state">Loading doctors...</div>}>
            <PatientDoctors />
          </Suspense>
        </RequireRole>
      </RequireAuth>
    ),
  },
  {
    path: "/patient/appointments",
    element: (
      <RequireAuth>
        <RequireRole role="PATIENT">
          <Suspense fallback={<div className="page-state">Loading appointments...</div>}>
            <PatientAppointments />
          </Suspense>
        </RequireRole>
      </RequireAuth>
    ),
  },
  {
    path: "/patient/queue",
    element: (
      <RequireAuth>
        <RequireRole role="PATIENT">
          <Suspense fallback={<div className="page-state">Loading queue...</div>}>
            <PatientQueue />
          </Suspense>
        </RequireRole>
      </RequireAuth>
    ),
  },

  // Doctor route & subroutes
  {
    path: "/doctor",
    element: (
      <RequireAuth>
        <RequireRole role="DOCTOR">
          <Suspense fallback={<div className="page-state">Loading station...</div>}>
            <DoctorDashboard />
          </Suspense>
        </RequireRole>
      </RequireAuth>
    ),
  },
  {
    path: "/doctor/*",
    element: (
      <RequireAuth>
        <RequireRole role="DOCTOR">
          <Suspense fallback={<div className="page-state">Loading station...</div>}>
            <DoctorDashboard />
          </Suspense>
        </RequireRole>
      </RequireAuth>
    ),
  },

  // Admin route & subroutes
  {
    path: "/admin",
    element: (
      <RequireAuth>
        <RequireRole role="ADMIN">
          <Suspense fallback={<div className="page-state">Loading admin console...</div>}>
            <AdminDashboard />
          </Suspense>
        </RequireRole>
      </RequireAuth>
    ),
  },
  {
    path: "/admin/*",
    element: (
      <RequireAuth>
        <RequireRole role="ADMIN">
          <Suspense fallback={<div className="page-state">Loading admin console...</div>}>
            <AdminDashboard />
          </Suspense>
        </RequireRole>
      </RequireAuth>
    ),
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
