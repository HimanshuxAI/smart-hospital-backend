import { useAuth } from "../../auth/auth-context";
import { AppShell } from "../../components/layout/AppShell";
import { mockAdminDashboard } from "../../mocks/data";
import { 
  Users, 
  Stethoscope, 
  Building2, 
  CalendarDays, 
  Activity, 
  TrendingUp, 
  ShieldCheck,
  Server,
  Database
} from "lucide-react";

export function AdminDashboardPage() {
  const { user } = useAuth();
  const data = mockAdminDashboard;
  const isDemo = import.meta.env.VITE_API_MODE === "demo";

  if (!user) return null;

  return (
    <AppShell role="ADMIN" user={user} demoMode={isDemo}>
      <div style={{ display: "flex", flexDirection: "column", gap: "2rem" }}>
        
        {/* Admin Header */}
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-end", flexWrap: "wrap", gap: "1rem" }}>
          <div>
            <div style={{
              display: "inline-flex",
              alignItems: "center",
              gap: "0.45rem",
              padding: "0.25rem 0.75rem",
              borderRadius: "9999px",
              background: "#ede9fe",
              color: "#6d28d9",
              fontSize: "0.75rem",
              fontWeight: 700,
              letterSpacing: "0.06em",
              textTransform: "uppercase",
              marginBottom: "0.5rem"
            }}>
              <ShieldCheck size={14} /> Root Clinical Command
            </div>
            <h1 style={{ fontSize: "2rem", fontWeight: 800, letterSpacing: "-0.03em", margin: 0, color: "#0f172a" }}>
              Hospital Operations Overview
            </h1>
            <p style={{ margin: "0.35rem 0 0", color: "#64748b", fontSize: "0.95rem" }}>
              Facility health, staffing availability, concurrency locks, and department throughput.
            </p>
          </div>

          <div style={{ display: "flex", gap: "0.75rem" }}>
            <span style={{
              display: "inline-flex",
              alignItems: "center",
              gap: "0.5rem",
              padding: "0.5rem 1rem",
              borderRadius: "0.75rem",
              background: "#ffffff",
              border: "1px solid #e2e8f0",
              fontSize: "0.82rem",
              fontWeight: 600,
              color: "#334155"
            }}>
              <Database size={15} color="#0284c7" />
              MySQL 8.4 Connected
            </span>
          </div>
        </div>

        {/* High-level system KPI cards */}
        <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(200px, 1fr))", gap: "1.25rem" }}>
          
          <div style={{ background: "#ffffff", borderRadius: "1.25rem", padding: "1.5rem", border: "1px solid #e2e8f0", boxShadow: "var(--shadow-card)" }}>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
              <span style={{ fontSize: "0.82rem", fontWeight: 700, color: "#64748b" }}>Total Users</span>
              <div style={{ width: "2rem", height: "2rem", borderRadius: "0.5rem", background: "#eff6ff", color: "#2563eb", display: "grid", placeItems: "center" }}>
                <Users size={16} />
              </div>
            </div>
            <div style={{ fontSize: "2.25rem", fontWeight: 800, color: "#0f172a", marginTop: "0.75rem", letterSpacing: "-0.04em" }}>
              {data.totalUsers}
            </div>
            <div style={{ fontSize: "0.75rem", color: "#64748b", marginTop: "0.25rem" }}>Registered across facility</div>
          </div>

          <div style={{ background: "#ffffff", borderRadius: "1.25rem", padding: "1.5rem", border: "1px solid #e2e8f0", boxShadow: "var(--shadow-card)" }}>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
              <span style={{ fontSize: "0.82rem", fontWeight: 700, color: "#64748b" }}>Active Doctors</span>
              <div style={{ width: "2rem", height: "2rem", borderRadius: "0.5rem", background: "#f0fdf4", color: "#16a34a", display: "grid", placeItems: "center" }}>
                <Stethoscope size={16} />
              </div>
            </div>
            <div style={{ fontSize: "2.25rem", fontWeight: 800, color: "#0f172a", marginTop: "0.75rem", letterSpacing: "-0.04em" }}>
              {data.totalDoctors}
            </div>
            <div style={{ fontSize: "0.75rem", color: "#16a34a", fontWeight: 600, marginTop: "0.25rem" }}>● All stations active</div>
          </div>

          <div style={{ background: "#ffffff", borderRadius: "1.25rem", padding: "1.5rem", border: "1px solid #e2e8f0", boxShadow: "var(--shadow-card)" }}>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
              <span style={{ fontSize: "0.82rem", fontWeight: 700, color: "#64748b" }}>Departments</span>
              <div style={{ width: "2rem", height: "2rem", borderRadius: "0.5rem", background: "#faf5ff", color: "#9333ea", display: "grid", placeItems: "center" }}>
                <Building2 size={16} />
              </div>
            </div>
            <div style={{ fontSize: "2.25rem", fontWeight: 800, color: "#0f172a", marginTop: "0.75rem", letterSpacing: "-0.04em" }}>
              {data.totalDepartments}
            </div>
            <div style={{ fontSize: "0.75rem", color: "#64748b", marginTop: "0.25rem" }}>Medicine, Cardiology, etc.</div>
          </div>

          <div style={{ background: "#ffffff", borderRadius: "1.25rem", padding: "1.5rem", border: "1px solid #e2e8f0", boxShadow: "var(--shadow-card)" }}>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
              <span style={{ fontSize: "0.82rem", fontWeight: 700, color: "#64748b" }}>Appointments Today</span>
              <div style={{ width: "2rem", height: "2rem", borderRadius: "0.5rem", background: "#e0f2fe", color: "#0284c7", display: "grid", placeItems: "center" }}>
                <CalendarDays size={16} />
              </div>
            </div>
            <div style={{ fontSize: "2.25rem", fontWeight: 800, color: "#0284c7", marginTop: "0.75rem", letterSpacing: "-0.04em" }}>
              {data.appointmentsToday}
            </div>
            <div style={{ fontSize: "0.75rem", color: "#0284c7", fontWeight: 600, marginTop: "0.25rem" }}>Booking rate normal</div>
          </div>

          <div style={{ background: "#ffffff", borderRadius: "1.25rem", padding: "1.5rem", border: "1px solid #fed7aa", boxShadow: "var(--shadow-card)" }}>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
              <span style={{ fontSize: "0.82rem", fontWeight: 700, color: "#9a3412" }}>Queued Today</span>
              <div style={{ width: "2rem", height: "2rem", borderRadius: "0.5rem", background: "#fff7ed", color: "#ea580c", display: "grid", placeItems: "center" }}>
                <Activity size={16} />
              </div>
            </div>
            <div style={{ fontSize: "2.25rem", fontWeight: 800, color: "#c2410c", marginTop: "0.75rem", letterSpacing: "-0.04em" }}>
              {data.queueEntriesToday}
            </div>
            <div style={{ fontSize: "0.75rem", color: "#9a3412", fontWeight: 600, marginTop: "0.25rem" }}>Peak triage load managed</div>
          </div>
        </div>

        {/* Infrastructure & Pipeline Telemetry */}
        <div style={{
          background: "#ffffff",
          borderRadius: "1.25rem",
          padding: "1.75rem",
          border: "1px solid #e2e8f0",
          boxShadow: "var(--shadow-card)"
        }}>
          <h2 style={{ fontSize: "1.1rem", fontWeight: 700, margin: "0 0 1rem", color: "#0f172a" }}>
            Operational Engine Status
          </h2>
          <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(280px, 1fr))", gap: "1.25rem" }}>
            <div style={{ padding: "1.25rem", borderRadius: "0.85rem", background: "#f8fafc", border: "1px solid #e2e8f0" }}>
              <div style={{ display: "flex", alignItems: "center", gap: "0.5rem", color: "#047857", fontWeight: 700, fontSize: "0.85rem", marginBottom: "0.35rem" }}>
                <span className="status-dot" /> Machine Learning Inference
              </div>
              <p style={{ fontSize: "0.8rem", color: "#64748b", margin: 0 }}>
                FastAPI random forest regressor loaded. Dynamically serving continuous waiting time estimates to queue endpoints.
              </p>
            </div>

            <div style={{ padding: "1.25rem", borderRadius: "0.85rem", background: "#f8fafc", border: "1px solid #e2e8f0" }}>
              <div style={{ display: "flex", alignItems: "center", gap: "0.5rem", color: "#047857", fontWeight: 700, fontSize: "0.85rem", marginBottom: "0.35rem" }}>
                <span className="status-dot" /> High Concurrency Schedule Lock
              </div>
              <p style={{ fontSize: "0.8rem", color: "#64748b", margin: 0 }}>
                Pessimistic row-level locking enabled on doctor appointment slots to prevent double-booking under spike loads.
              </p>
            </div>
          </div>
        </div>
      </div>
    </AppShell>
  );
}
