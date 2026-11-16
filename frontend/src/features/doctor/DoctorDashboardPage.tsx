import { useAuth } from "../../auth/auth-context";
import { AppShell } from "../../components/layout/AppShell";
import { mockDoctorDashboard } from "../../mocks/data";
import { 
  Users, 
  Clock, 
  Activity, 
  CalendarCheck, 
  ChevronRight, 
  Stethoscope,
  ArrowRight
} from "lucide-react";

export function DoctorDashboardPage() {
  const { user } = useAuth();
  const data = mockDoctorDashboard;
  const isDemo = import.meta.env.VITE_API_MODE === "demo";

  if (!user) return null;

  return (
    <AppShell role="DOCTOR" user={user} demoMode={isDemo}>
      <div style={{ display: "flex", flexDirection: "column", gap: "1.75rem" }}>
        
        {/* Header */}
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-end", flexWrap: "wrap", gap: "1rem" }}>
          <div>
            <h1 style={{ fontSize: "1.75rem", fontWeight: 700, color: "var(--text-main)", letterSpacing: "-0.02em" }}>
              Doctor Station — {data.doctorName}
            </h1>
            <p style={{ color: "var(--text-muted)", fontSize: "0.92rem", marginTop: "0.2rem" }}>
              Manage today's clinical consultation queue and patient flow.
            </p>
          </div>

          <button type="button" className="button button-primary">
            <Users size={16} />
            <span>Call Next Patient</span>
            <ArrowRight size={16} />
          </button>
        </div>

        {/* 3 Metric Cards */}
        <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(240px, 1fr))", gap: "1.25rem" }}>
          
          <div className="card-saas">
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
              <span style={{ fontSize: "0.85rem", fontWeight: 500, color: "var(--text-muted)" }}>Appointments Today</span>
              <div style={{ width: "2rem", height: "2rem", borderRadius: "6px", background: "var(--bg-subtle)", color: "var(--text-main)", display: "flex", alignItems: "center", justifyContent: "center" }}>
                <CalendarCheck size={16} />
              </div>
            </div>
            <div style={{ fontSize: "2.25rem", fontWeight: 700, color: "var(--text-main)", marginTop: "0.75rem" }}>
              {data.todayAppointmentCount}
            </div>
            <span style={{ fontSize: "0.75rem", color: "var(--text-muted)" }}>Scheduled on calendar</span>
          </div>

          <div className="card-saas">
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
              <span style={{ fontSize: "0.85rem", fontWeight: 500, color: "var(--text-muted)" }}>Patients Waiting</span>
              <div style={{ width: "2rem", height: "2rem", borderRadius: "6px", background: "var(--badge-orange-bg)", color: "var(--badge-orange-text)", display: "flex", alignItems: "center", justifyContent: "center" }}>
                <Clock size={16} />
              </div>
            </div>
            <div style={{ fontSize: "2.25rem", fontWeight: 700, color: "var(--badge-orange-text)", marginTop: "0.75rem" }}>
              {data.waitingCount}
            </div>
            <span style={{ fontSize: "0.75rem", color: "var(--badge-orange-text)", fontWeight: 500 }}>In waiting area</span>
          </div>

          <div className="card-saas">
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
              <span style={{ fontSize: "0.85rem", fontWeight: 500, color: "var(--text-muted)" }}>In Consultation</span>
              <div style={{ width: "2rem", height: "2rem", borderRadius: "6px", background: "var(--primary-subtle)", color: "var(--primary)", display: "flex", alignItems: "center", justifyContent: "center" }}>
                <Activity size={16} />
              </div>
            </div>
            <div style={{ fontSize: "2.25rem", fontWeight: 700, color: "var(--primary)", marginTop: "0.75rem" }}>
              {data.inConsultationCount}
            </div>
            <span style={{ fontSize: "0.75rem", color: "var(--primary)", fontWeight: 500 }}>Active examination</span>
          </div>
        </div>

        {/* Workflow card */}
        <div className="card-saas">
          <h2 style={{ fontSize: "1rem", fontWeight: 600, color: "var(--text-main)", marginBottom: "1rem" }}>
            Patient Flow Guidelines
          </h2>
          <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(260px, 1fr))", gap: "1rem" }}>
            <div style={{ padding: "1rem", background: "var(--bg-subtle)", borderRadius: "8px", border: "1px solid var(--border-color)" }}>
              <div style={{ fontSize: "0.85rem", fontWeight: 600, color: "var(--text-main)", marginBottom: "0.25rem" }}>1. Triage & Sorting</div>
              <p style={{ fontSize: "0.8rem", color: "var(--text-muted)", margin: 0 }}>Patients are ordered automatically using machine learning triage priority weights.</p>
            </div>
            <div style={{ padding: "1rem", background: "var(--bg-subtle)", borderRadius: "8px", border: "1px solid var(--border-color)" }}>
              <div style={{ fontSize: "0.85rem", fontWeight: 600, color: "var(--text-main)", marginBottom: "0.25rem" }}>2. Call to Station</div>
              <p style={{ fontSize: "0.8rem", color: "var(--text-muted)", margin: 0 }}>Clicking "Call Next Patient" broadcasts the notification to public monitors.</p>
            </div>
            <div style={{ padding: "1rem", background: "var(--bg-subtle)", borderRadius: "8px", border: "1px solid var(--border-color)" }}>
              <div style={{ fontSize: "0.85rem", fontWeight: 600, color: "var(--text-main)", marginBottom: "0.25rem" }}>3. Finish Visit</div>
              <p style={{ fontSize: "0.8rem", color: "var(--text-muted)", margin: 0 }}>Complete the consultation note to clear the queue slot and free capacity.</p>
            </div>
          </div>
        </div>
      </div>
    </AppShell>
  );
}
