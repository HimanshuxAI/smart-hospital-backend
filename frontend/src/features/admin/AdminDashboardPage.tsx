import { useAuth } from "../../auth/auth-context";
import { AppShell } from "../../components/layout/AppShell";
import { mockAdminDashboard } from "../../mocks/data";
import { 
  Users, 
  Stethoscope, 
  Building2, 
  CalendarDays, 
  Activity, 
  Database,
  CheckCircle2
} from "lucide-react";

export function AdminDashboardPage() {
  const { user } = useAuth();
  const data = mockAdminDashboard;
  const isDemo = import.meta.env.VITE_API_MODE === "demo";

  if (!user) return null;

  return (
    <AppShell role="ADMIN" user={user} demoMode={isDemo}>
      <div style={{ display: "flex", flexDirection: "column", gap: "1.75rem" }}>
        
        {/* Header */}
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-end", flexWrap: "wrap", gap: "1rem" }}>
          <div>
            <h1 style={{ fontSize: "1.75rem", fontWeight: 700, color: "var(--text-main)", letterSpacing: "-0.02em" }}>
              Hospital Operations
            </h1>
            <p style={{ color: "var(--text-muted)", fontSize: "0.92rem", marginTop: "0.2rem" }}>
              Facility performance metrics, active staffing, and clinical capacity.
            </p>
          </div>

          <div style={{
            display: "inline-flex",
            alignItems: "center",
            gap: "0.5rem",
            padding: "0.4rem 0.85rem",
            background: "var(--bg-surface)",
            border: "1px solid var(--border-color)",
            borderRadius: "6px",
            fontSize: "0.8rem",
            fontWeight: 500,
            color: "var(--text-main)"
          }}>
            <Database size={15} color="var(--primary)" />
            <span>Database: MySQL 8.4 Online</span>
          </div>
        </div>

        {/* 5 Stats Cards */}
        <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(200px, 1fr))", gap: "1rem" }}>
          
          <div className="card-saas">
            <span style={{ fontSize: "0.8rem", color: "var(--text-muted)", fontWeight: 500 }}>Total Users</span>
            <div style={{ fontSize: "2rem", fontWeight: 700, color: "var(--text-main)", margin: "0.35rem 0" }}>
              {data.totalUsers}
            </div>
            <span style={{ fontSize: "0.75rem", color: "var(--text-muted)" }}>Registered accounts</span>
          </div>

          <div className="card-saas">
            <span style={{ fontSize: "0.8rem", color: "var(--text-muted)", fontWeight: 500 }}>Active Doctors</span>
            <div style={{ fontSize: "2rem", fontWeight: 700, color: "var(--text-main)", margin: "0.35rem 0" }}>
              {data.totalDoctors}
            </div>
            <span style={{ fontSize: "0.75rem", color: "#16a34a", fontWeight: 500 }}>On duty today</span>
          </div>

          <div className="card-saas">
            <span style={{ fontSize: "0.8rem", color: "var(--text-muted)", fontWeight: 500 }}>Departments</span>
            <div style={{ fontSize: "2rem", fontWeight: 700, color: "var(--text-main)", margin: "0.35rem 0" }}>
              {data.totalDepartments}
            </div>
            <span style={{ fontSize: "0.75rem", color: "var(--text-muted)" }}>Clinical units</span>
          </div>

          <div className="card-saas">
            <span style={{ fontSize: "0.8rem", color: "var(--text-muted)", fontWeight: 500 }}>Appointments</span>
            <div style={{ fontSize: "2rem", fontWeight: 700, color: "var(--text-main)", margin: "0.35rem 0" }}>
              {data.appointmentsToday}
            </div>
            <span style={{ fontSize: "0.75rem", color: "var(--primary)", fontWeight: 500 }}>Booked today</span>
          </div>

          <div className="card-saas">
            <span style={{ fontSize: "0.8rem", color: "var(--text-muted)", fontWeight: 500 }}>Queue Today</span>
            <div style={{ fontSize: "2rem", fontWeight: 700, color: "var(--badge-orange-text)", margin: "0.35rem 0" }}>
              {data.queueEntriesToday}
            </div>
            <span style={{ fontSize: "0.75rem", color: "var(--badge-orange-text)", fontWeight: 500 }}>Total patients processed</span>
          </div>
        </div>

        {/* System Health */}
        <div className="card-saas">
          <h2 style={{ fontSize: "1rem", fontWeight: 600, color: "var(--text-main)", marginBottom: "1rem" }}>
            Connected Services
          </h2>
          <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(280px, 1fr))", gap: "1rem" }}>
            <div style={{ padding: "1rem", background: "var(--bg-subtle)", borderRadius: "8px", border: "1px solid var(--border-color)" }}>
              <div style={{ display: "flex", alignItems: "center", gap: "0.4rem", color: "#16a34a", fontWeight: 600, fontSize: "0.85rem", marginBottom: "0.25rem" }}>
                <span className="status-dot" /> Spring Boot Core API (8080)
              </div>
              <p style={{ fontSize: "0.8rem", color: "var(--text-muted)", margin: 0 }}>
                JWT authentication, appointment scheduling, and pessimistic row locking operational.
              </p>
            </div>

            <div style={{ padding: "1rem", background: "var(--bg-subtle)", borderRadius: "8px", border: "1px solid var(--border-color)" }}>
              <div style={{ display: "flex", alignItems: "center", gap: "0.4rem", color: "#16a34a", fontWeight: 600, fontSize: "0.85rem", marginBottom: "0.25rem" }}>
                <span className="status-dot" /> ML Waiting Time Predictor (8000)
              </div>
              <p style={{ fontSize: "0.8rem", color: "var(--text-muted)", margin: 0 }}>
                FastAPI microservice loaded with random forest regression models for dynamic queue estimation.
              </p>
            </div>
          </div>
        </div>
      </div>
    </AppShell>
  );
}
