import { useAuth } from "../../auth/auth-context";
import { AppShell } from "../../components/layout/AppShell";
import { mockAdminDashboard } from "../../mocks/data";
import { 
  Users, 
  Stethoscope, 
  Building2, 
  CalendarDays, 
  Activity, 
  Terminal,
  Cpu,
  Database
} from "lucide-react";

export function AdminDashboardPage() {
  const { user } = useAuth();
  const data = mockAdminDashboard;
  const isDemo = import.meta.env.VITE_API_MODE === "demo";

  if (!user) return null;

  return (
    <AppShell role="ADMIN" user={user} demoMode={isDemo}>
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
              [ ROOT SYSTEM TELEMETRY // FACILITY CONTROL ]
            </div>
            <h1 style={{
              fontFamily: "var(--font-serif)",
              fontSize: "clamp(2.5rem, 4vw, 3.5rem)",
              margin: 0,
              color: "var(--color-ink)",
              lineHeight: 1.05
            }}>
              Infrastructure Core
            </h1>
          </div>

          <div style={{ display: "flex", gap: "0.75rem", alignItems: "center" }}>
            <span style={{
              display: "inline-flex",
              alignItems: "center",
              gap: "0.5rem",
              padding: "0.4rem 0.85rem",
              background: "#ffffff",
              border: "1.5px solid var(--color-ink)",
              borderRadius: "4px",
              boxShadow: "var(--shadow-hard-xs)",
              fontFamily: "var(--font-mono)",
              fontSize: "0.75rem",
              fontWeight: 700
            }}>
              <Database size={15} color="var(--accent-vermilion)" />
              MYSQL 8.4 CONNECTED
            </span>
          </div>
        </div>

        {/* Dense Architectural Numbers Grid */}
        <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(210px, 1fr))", gap: "1.25rem" }}>
          
          <div style={{ background: "#ffffff", border: "2px solid var(--color-ink)", borderRadius: "8px", padding: "1.5rem", boxShadow: "var(--shadow-hard-sm)" }}>
            <div style={{ fontFamily: "var(--font-mono)", fontSize: "0.72rem", color: "var(--color-muted)", textTransform: "uppercase" }}>
              // TOTAL IDENTITIES
            </div>
            <div style={{ fontFamily: "var(--font-display)", fontSize: "3rem", fontWeight: 800, color: "var(--color-ink)", margin: "0.25rem 0" }}>
              {data.totalUsers}
            </div>
            <div style={{ fontFamily: "var(--font-mono)", fontSize: "0.72rem", color: "var(--color-muted)" }}>Active profiles</div>
          </div>

          <div style={{ background: "#ffffff", border: "2px solid var(--color-ink)", borderRadius: "8px", padding: "1.5rem", boxShadow: "var(--shadow-hard-sm)" }}>
            <div style={{ fontFamily: "var(--font-mono)", fontSize: "0.72rem", color: "var(--color-muted)", textTransform: "uppercase" }}>
              // ACTIVE DOCTORS
            </div>
            <div style={{ fontFamily: "var(--font-display)", fontSize: "3rem", fontWeight: 800, color: "var(--color-ink)", margin: "0.25rem 0" }}>
              {data.totalDoctors}
            </div>
            <div style={{ fontFamily: "var(--font-mono)", fontSize: "0.72rem", color: "var(--color-muted)" }}>Stations staffed</div>
          </div>

          <div style={{ background: "#ffffff", border: "2px solid var(--color-ink)", borderRadius: "8px", padding: "1.5rem", boxShadow: "var(--shadow-hard-sm)" }}>
            <div style={{ fontFamily: "var(--font-mono)", fontSize: "0.72rem", color: "var(--color-muted)", textTransform: "uppercase" }}>
              // CLINICAL DEPTS
            </div>
            <div style={{ fontFamily: "var(--font-display)", fontSize: "3rem", fontWeight: 800, color: "var(--color-ink)", margin: "0.25rem 0" }}>
              {data.totalDepartments}
            </div>
            <div style={{ fontFamily: "var(--font-mono)", fontSize: "0.72rem", color: "var(--color-muted)" }}>Operational units</div>
          </div>

          <div style={{ background: "#ffffff", border: "2px solid var(--color-ink)", borderRadius: "8px", padding: "1.5rem", boxShadow: "var(--shadow-hard-sm)" }}>
            <div style={{ fontFamily: "var(--font-mono)", fontSize: "0.72rem", color: "var(--color-muted)", textTransform: "uppercase" }}>
              // TODAY'S BOOKINGS
            </div>
            <div style={{ fontFamily: "var(--font-display)", fontSize: "3rem", fontWeight: 800, color: "var(--color-ink)", margin: "0.25rem 0" }}>
              {data.appointmentsToday}
            </div>
            <div style={{ fontFamily: "var(--font-mono)", fontSize: "0.72rem", color: "var(--color-muted)" }}>Concurrency passed</div>
          </div>

          <div style={{ background: "var(--accent-vermilion-soft)", border: "2px solid var(--color-ink)", borderRadius: "8px", padding: "1.5rem", boxShadow: "var(--shadow-hard-sm)" }}>
            <div style={{ fontFamily: "var(--font-mono)", fontSize: "0.72rem", color: "var(--accent-vermilion)", fontWeight: 700, textTransform: "uppercase" }}>
              // QUEUED TICKETS
            </div>
            <div style={{ fontFamily: "var(--font-display)", fontSize: "3rem", fontWeight: 800, color: "var(--accent-vermilion)", margin: "0.25rem 0" }}>
              {data.queueEntriesToday}
            </div>
            <div style={{ fontFamily: "var(--font-mono)", fontSize: "0.72rem", color: "var(--accent-vermilion)" }}>Live admission flow</div>
          </div>
        </div>

        {/* Engine Diagnostics Panel */}
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
              SYSTEM ARCHITECTURE TELEMETRY
            </span>
            <Cpu size={18} />
          </div>

          <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(280px, 1fr))", gap: "1.5rem" }}>
            <div style={{ padding: "1.5rem", border: "1.5px solid var(--color-ink)", borderRadius: "4px", background: "var(--bg-canvas)" }}>
              <div style={{ fontFamily: "var(--font-mono)", fontSize: "0.78rem", fontWeight: 700, marginBottom: "0.4rem", color: "var(--color-ink)" }}>
                [ENGINE] ML FASTAPI INFERENCE SERVER
              </div>
              <p style={{ margin: 0, fontSize: "0.85rem", color: "var(--color-ink-subtle)", lineHeight: 1.6 }}>
                Port 8000 listening. RandomForestRegressor models calibrated on patient volume, emergency severity indices, and departmental backlog.
              </p>
            </div>

            <div style={{ padding: "1.5rem", border: "1.5px solid var(--color-ink)", borderRadius: "4px", background: "var(--bg-canvas)" }}>
              <div style={{ fontFamily: "var(--font-mono)", fontSize: "0.78rem", fontWeight: 700, marginBottom: "0.4rem", color: "var(--color-ink)" }}>
                [ENGINE] SPRING BOOT CONCURRENCY GUARANTEE
              </div>
              <p style={{ margin: 0, fontSize: "0.85rem", color: "var(--color-ink-subtle)", lineHeight: 1.6 }}>
                Port 8080 running. JPA Pessimistic write locks with Flyway versioned schemas ensuring deterministic zero-collision slot bookings.
              </p>
            </div>
          </div>
        </div>
      </div>
    </AppShell>
  );
}
