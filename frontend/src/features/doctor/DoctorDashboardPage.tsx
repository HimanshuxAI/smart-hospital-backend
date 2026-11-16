import { useAuth } from "../../auth/auth-context";
import { AppShell } from "../../components/layout/AppShell";
import { mockDoctorDashboard } from "../../mocks/data";
import { 
  Users, 
  Clock, 
  Activity, 
  ArrowUpRight, 
  Stethoscope,
  Terminal,
  ChevronRight
} from "lucide-react";

export function DoctorDashboardPage() {
  const { user } = useAuth();
  const data = mockDoctorDashboard;
  const isDemo = import.meta.env.VITE_API_MODE === "demo";

  if (!user) return null;

  return (
    <AppShell role="DOCTOR" user={user} demoMode={isDemo}>
      <div style={{ display: "flex", flexDirection: "column", gap: "2.5rem" }}>
        
        {/* Header */}
        <div style={{
          display: "flex",
          justifyContent: "space-between",
          alignItems: "flex-end",
          flexWrap: "wrap",
          gap: "1.5rem",
          paddingBottom: "1.5rem",
          borderBottom: "1.5px solid var(--color-ink)"
        }}>
          <div>
            <div style={{
              display: "inline-block",
              fontFamily: "var(--font-mono)",
              fontSize: "0.75rem",
              color: "var(--accent-vermilion)",
              fontWeight: 700,
              textTransform: "uppercase",
              letterSpacing: "0.06em",
              marginBottom: "0.5rem"
            }}>
              [ CLINICAL STATION // DR. CONSOLE ]
            </div>
            <h1 style={{
              fontFamily: "var(--font-serif)",
              fontSize: "clamp(2.5rem, 4vw, 3.5rem)",
              margin: 0,
              color: "var(--color-ink)",
              lineHeight: 1.05
            }}>
              Station — <em style={{ fontStyle: "italic", color: "var(--accent-vermilion)" }}>{data.doctorName}</em>
            </h1>
          </div>

          <button type="button" className="button button-primary" style={{ padding: "0.75rem 1.75rem" }}>
            <Users size={18} />
            <span>CALL NEXT TICKET</span>
            <ArrowUpRight size={18} />
          </button>
        </div>

        {/* 3 Physical KPI Blocks */}
        <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(260px, 1fr))", gap: "1.5rem" }}>
          
          <div style={{
            background: "#ffffff",
            border: "2px solid var(--color-ink)",
            borderRadius: "8px",
            padding: "1.75rem",
            boxShadow: "var(--shadow-hard-md)"
          }}>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
              <span style={{ fontFamily: "var(--font-mono)", fontSize: "0.75rem", fontWeight: 700, textTransform: "uppercase" }}>
                // TODAY'S APPOINTMENTS
              </span>
              <span style={{ fontFamily: "var(--font-mono)", fontSize: "0.72rem", color: "var(--color-muted)" }}>[01]</span>
            </div>
            <div style={{ fontFamily: "var(--font-display)", fontSize: "3.5rem", fontWeight: 800, color: "var(--color-ink)", margin: "0.5rem 0 0.25rem", letterSpacing: "-0.04em" }}>
              {data.todayAppointmentCount}
            </div>
            <div style={{ fontFamily: "var(--font-mono)", fontSize: "0.75rem", color: "var(--color-muted)" }}>
              CONFIRMED CALENDAR SLOTS
            </div>
          </div>

          <div style={{
            background: "var(--accent-vermilion-soft)",
            border: "2px solid var(--color-ink)",
            borderRadius: "8px",
            padding: "1.75rem",
            boxShadow: "var(--shadow-hard-md)"
          }}>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
              <span style={{ fontFamily: "var(--font-mono)", fontSize: "0.75rem", fontWeight: 700, color: "var(--accent-vermilion)", textTransform: "uppercase" }}>
                // PATIENTS WAITING
              </span>
              <span style={{ fontFamily: "var(--font-mono)", fontSize: "0.72rem", color: "var(--accent-vermilion)" }}>[02]</span>
            </div>
            <div style={{ fontFamily: "var(--font-display)", fontSize: "3.5rem", fontWeight: 800, color: "var(--accent-vermilion)", margin: "0.5rem 0 0.25rem", letterSpacing: "-0.04em" }}>
              {data.waitingCount}
            </div>
            <div style={{ fontFamily: "var(--font-mono)", fontSize: "0.75rem", color: "var(--accent-vermilion)", fontWeight: 700 }}>
              PRIORITIZED VIA ML ENGINE
            </div>
          </div>

          <div style={{
            background: "#ffffff",
            border: "2px solid var(--color-ink)",
            borderRadius: "8px",
            padding: "1.75rem",
            boxShadow: "var(--shadow-hard-md)"
          }}>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
              <span style={{ fontFamily: "var(--font-mono)", fontSize: "0.75rem", fontWeight: 700, textTransform: "uppercase" }}>
                // IN CONSULTATION
              </span>
              <span style={{ fontFamily: "var(--font-mono)", fontSize: "0.72rem", color: "var(--color-muted)" }}>[03]</span>
            </div>
            <div style={{ fontFamily: "var(--font-display)", fontSize: "3.5rem", fontWeight: 800, color: "var(--color-ink)", margin: "0.5rem 0 0.25rem", letterSpacing: "-0.04em" }}>
              {data.inConsultationCount}
            </div>
            <div style={{ fontFamily: "var(--font-mono)", fontSize: "0.75rem", color: "var(--color-muted)" }}>
              ACTIVE CLINICAL EXAM
            </div>
          </div>
        </div>

        {/* Workflow Card */}
        <div style={{
          background: "#ffffff",
          border: "2px solid var(--color-ink)",
          borderRadius: "8px",
          padding: "2rem",
          boxShadow: "var(--shadow-hard-md)"
        }}>
          <div style={{
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
            paddingBottom: "1rem",
            marginBottom: "1.5rem",
            borderBottom: "1.5px dashed var(--color-border)"
          }}>
            <span style={{ fontFamily: "var(--font-mono)", fontSize: "0.75rem", fontWeight: 700, textTransform: "uppercase" }}>
              OPERATING PROTOCOL // INTAKE PIPELINE
            </span>
            <Terminal size={18} />
          </div>

          <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(260px, 1fr))", gap: "1.25rem" }}>
            <div style={{ padding: "1.25rem", border: "1.5px solid var(--color-ink)", borderRadius: "4px", background: "var(--bg-canvas)" }}>
              <div style={{ fontFamily: "var(--font-mono)", fontSize: "0.75rem", fontWeight: 700, marginBottom: "0.4rem" }}>
                [STEP 01] TRIAGE ARRIVAL
              </div>
              <p style={{ margin: 0, fontSize: "0.85rem", color: "var(--color-ink-subtle)" }}>
                Patient check-in feeds directly into Python scikit-learn models to calculate real-time queue sorting.
              </p>
            </div>

            <div style={{ padding: "1.25rem", border: "1.5px solid var(--color-ink)", borderRadius: "4px", background: "var(--bg-canvas)" }}>
              <div style={{ fontFamily: "var(--font-mono)", fontSize: "0.75rem", fontWeight: 700, marginBottom: "0.4rem" }}>
                [STEP 02] PESSIMISTIC LOCKING
              </div>
              <p style={{ margin: 0, fontSize: "0.85rem", color: "var(--color-ink-subtle)" }}>
                Consultation room summons utilize MySQL database-level locks, guaranteeing zero double-calls.
              </p>
            </div>

            <div style={{ padding: "1.25rem", border: "1.5px solid var(--color-ink)", borderRadius: "4px", background: "var(--bg-canvas)" }}>
              <div style={{ fontFamily: "var(--font-mono)", fontSize: "0.75rem", fontWeight: 700, marginBottom: "0.4rem" }}>
                [STEP 03] DISPATCH COMPLETION
              </div>
              <p style={{ margin: 0, fontSize: "0.85rem", color: "var(--color-ink-subtle)" }}>
                Release station updates the hospital display board and triggers waiting time re-estimations.
              </p>
            </div>
          </div>
        </div>
      </div>
    </AppShell>
  );
}
