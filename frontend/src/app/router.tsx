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
    path: "/403",
    element: <ForbiddenPage />,
  },
  {
    path: "*",
    element: <NotFoundPage />,
  },
]);
