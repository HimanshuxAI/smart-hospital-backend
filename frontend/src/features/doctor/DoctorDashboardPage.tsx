import { useAuth } from "../../auth/auth-context";
import { AppShell } from "../../components/layout/AppShell";
import { mockDoctorDashboard } from "../../mocks/data";
import { 
  Users, 
  Clock, 
  Activity, 
  CalendarCheck, 
  CheckCircle, 
  ChevronRight, 
  Stethoscope,
  Sparkles,
  ArrowUpRight
} from "lucide-react";

export function DoctorDashboardPage() {
  const { user } = useAuth();
  const data = mockDoctorDashboard;
  const isDemo = import.meta.env.VITE_API_MODE === "demo";

  if (!user) return null;

  return (
    <AppShell role="DOCTOR" user={user} demoMode={isDemo}>
      <div style={{ display: "flex", flexDirection: "column", gap: "2rem" }}>
        
        {/* Physician Header */}
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-end", flexWrap: "wrap", gap: "1rem" }}>
          <div>
            <div style={{
              display: "inline-flex",
              alignItems: "center",
              gap: "0.45rem",
              padding: "0.25rem 0.75rem",
              borderRadius: "9999px",
              background: "#e0f2fe",
              color: "#0369a1",
              fontSize: "0.75rem",
              fontWeight: 700,
              letterSpacing: "0.06em",
              textTransform: "uppercase",
              marginBottom: "0.5rem"
            }}>
              <Stethoscope size={14} /> Physician Clinical Console
            </div>
            <h1 style={{ fontSize: "2rem", fontWeight: 800, letterSpacing: "-0.03em", margin: 0, color: "#0f172a" }}>
              Doctor Station — {data.doctorName}
            </h1>
            <p style={{ margin: "0.35rem 0 0", color: "#64748b", fontSize: "0.95rem" }}>
              Real-time patient flow, triage classification and consultation intake.
            </p>
          </div>

          <div style={{ display: "flex", gap: "0.75rem" }}>
            <button type="button" className="button button-primary">
              <Users size={16} />
              <span>Call Next Patient</span>
            </button>
          </div>
        </div>

        {/* Clinical Overview Stat Cards */}
        <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(240px, 1fr))", gap: "1.25rem" }}>
          
          {/* Today's Appointments */}
          <div style={{
            background: "#ffffff",
            borderRadius: "1.25rem",
            padding: "1.5rem",
            border: "1px solid #e2e8f0",
            boxShadow: "var(--shadow-card)",
            display: "flex",
            flexDirection: "column",
            justifyContent: "space-between"
          }}>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start" }}>
              <span style={{ fontSize: "0.85rem", fontWeight: 700, color: "#64748b" }}>Today's Appointments</span>
              <div style={{ width: "2.25rem", height: "2.25rem", borderRadius: "0.6rem", background: "#f0fdf4", color: "#16a34a", display: "grid", placeItems: "center" }}>
                <CalendarCheck size={18} />
              </div>
            </div>
            <div style={{ marginTop: "1rem" }}>
              <div style={{ fontSize: "2.5rem", fontWeight: 800, color: "#0f172a", letterSpacing: "-0.04em", lineHeight: 1 }}>
                {data.todayAppointmentCount}
              </div>
              <div style={{ fontSize: "0.78rem", color: "#16a34a", fontWeight: 600, marginTop: "0.5rem", display: "flex", alignItems: "center", gap: "0.2rem" }}>
                <ArrowUpRight size={14} /> Full day scheduled
              </div>
            </div>
          </div>

          {/* Waiting in Queue */}
          <div style={{
            background: "#ffffff",
            borderRadius: "1.25rem",
            padding: "1.5rem",
            border: "1px solid #fed7aa",
            boxShadow: "var(--shadow-card)",
            display: "flex",
            flexDirection: "column",
            justifyContent: "space-between"
          }}>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start" }}>
              <span style={{ fontSize: "0.85rem", fontWeight: 700, color: "#9a3412" }}>Waiting in Queue</span>
              <div style={{ width: "2.25rem", height: "2.25rem", borderRadius: "0.6rem", background: "#fff7ed", color: "#ea580c", display: "grid", placeItems: "center" }}>
                <Clock size={18} />
              </div>
            </div>
            <div style={{ marginTop: "1rem" }}>
              <div style={{ fontSize: "2.5rem", fontWeight: 800, color: "#c2410c", letterSpacing: "-0.04em", lineHeight: 1 }}>
                {data.waitingCount}
              </div>
              <div style={{ fontSize: "0.78rem", color: "#9a3412", fontWeight: 600, marginTop: "0.5rem", display: "flex", alignItems: "center", gap: "0.3rem" }}>
                <Sparkles size={13} /> Prioritized by ML triage
              </div>
            </div>
          </div>

          {/* In Active Consultation */}
          <div style={{
            background: "#ffffff",
            borderRadius: "1.25rem",
            padding: "1.5rem",
            border: "1px solid #bae6fd",
            boxShadow: "var(--shadow-card)",
            display: "flex",
            flexDirection: "column",
            justifyContent: "space-between"
          }}>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start" }}>
              <span style={{ fontSize: "0.85rem", fontWeight: 700, color: "#0369a1" }}>In Consultation</span>
              <div style={{ width: "2.25rem", height: "2.25rem", borderRadius: "0.6rem", background: "#f0f9ff", color: "#0284c7", display: "grid", placeItems: "center" }}>
                <Activity size={18} />
              </div>
            </div>
            <div style={{ marginTop: "1rem" }}>
              <div style={{ fontSize: "2.5rem", fontWeight: 800, color: "#0284c7", letterSpacing: "-0.04em", lineHeight: 1 }}>
                {data.inConsultationCount}
              </div>
              <div style={{ fontSize: "0.78rem", color: "#0369a1", fontWeight: 600, marginTop: "0.5rem" }}>
                Active examination in progress
              </div>
            </div>
          </div>
        </div>

        {/* Action Panel for Consultation */}
        <div style={{
          background: "#ffffff",
          borderRadius: "1.25rem",
          padding: "1.75rem",
          border: "1px solid #e2e8f0",
          boxShadow: "var(--shadow-card)"
        }}>
          <h2 style={{ fontSize: "1.1rem", fontWeight: 700, margin: "0 0 1rem", color: "#0f172a" }}>
            Patient Intake Queue Workflow
          </h2>
          <div style={{ display: "flex", gap: "1rem", flexWrap: "wrap" }}>
            <div style={{ flex: 1, minWidth: "260px", padding: "1.25rem", borderRadius: "0.85rem", background: "#f8fafc", border: "1px solid #e2e8f0" }}>
              <div style={{ fontSize: "0.85rem", fontWeight: 700, color: "#1e293b", marginBottom: "0.35rem" }}>1. Check-In & Triage</div>
              <p style={{ fontSize: "0.8rem", color: "#64748b", margin: 0 }}>Patients check in at reception or mobile portal; priority tier classified via ML inference.</p>
            </div>
            <div style={{ flex: 1, minWidth: "260px", padding: "1.25rem", borderRadius: "0.85rem", background: "#f8fafc", border: "1px solid #e2e8f0" }}>
              <div style={{ fontSize: "0.85rem", fontWeight: 700, color: "#1e293b", marginBottom: "0.35rem" }}>2. Call to Station</div>
              <p style={{ fontSize: "0.8rem", color: "#64748b", margin: 0 }}>Summon the highest priority ticket to your consultation room with instant display update.</p>
            </div>
            <div style={{ flex: 1, minWidth: "260px", padding: "1.25rem", borderRadius: "0.85rem", background: "#f8fafc", border: "1px solid #e2e8f0" }}>
              <div style={{ fontSize: "0.85rem", fontWeight: 700, color: "#1e293b", marginBottom: "0.35rem" }}>3. Complete Visit</div>
              <p style={{ fontSize: "0.8rem", color: "#64748b", margin: 0 }}>Finalize notes and release the slot to recalculate real-time hospital wait metrics.</p>
            </div>
          </div>
        </div>
      </div>
    </AppShell>
  );
}
