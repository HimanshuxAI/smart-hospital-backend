import { useAuth } from "../../auth/auth-context";
import { AppShell } from "../../components/layout/AppShell";
import { Ticket, Clock, CheckCircle2, AlertCircle } from "lucide-react";

export function PatientQueuePage() {
  const { user } = useAuth();

  if (!user) return null;

  return (
    <AppShell role="PATIENT" user={user}>
      <div style={{ display: "flex", flexDirection: "column", gap: "1.75rem" }}>
        
        <div>
          <h1 style={{ fontSize: "1.75rem", fontWeight: 700, color: "var(--text-main)", letterSpacing: "-0.02em" }}>
            Live Queue Token
          </h1>
          <p style={{ color: "var(--text-muted)", fontSize: "0.92rem", marginTop: "0.2rem" }}>
            Your real-time ticket for today's walk-in and consultation triage.
          </p>
        </div>

        {/* Big Ticket Banner */}
        <div className="card-saas" style={{ maxWidth: "560px", margin: "0 auto", width: "100%", textAlign: "center", padding: "2.5rem 2rem" }}>
          <span style={{ fontSize: "0.8rem", fontWeight: 600, color: "var(--text-muted)", textTransform: "uppercase", letterSpacing: "0.05em" }}>
            Admission Token
          </span>

          <div style={{ fontSize: "4.5rem", fontWeight: 800, color: "var(--text-main)", lineHeight: 1.1, margin: "0.75rem 0" }}>
            #T-104
          </div>

          <span style={{
            display: "inline-block",
            padding: "0.25rem 0.85rem",
            borderRadius: "9999px",
            background: "var(--badge-green-bg)",
            color: "var(--badge-green-text)",
            border: "1px solid var(--badge-green-border)",
            fontSize: "0.8rem",
            fontWeight: 600,
            marginBottom: "2rem"
          }}>
            Status: Priority 3 (Normal Triage)
          </span>

          <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "1rem", textAlign: "left" }}>
            <div style={{ padding: "1rem", background: "var(--bg-subtle)", borderRadius: "8px", border: "1px solid var(--border-color)" }}>
              <div style={{ fontSize: "0.75rem", color: "var(--text-muted)" }}>Ahead of you</div>
              <div style={{ fontSize: "1.5rem", fontWeight: 700, color: "var(--text-main)", marginTop: "0.2rem" }}>2 Patients</div>
            </div>

            <div style={{ padding: "1rem", background: "var(--primary-subtle)", borderRadius: "8px", border: "1px solid #bae6fd" }}>
              <div style={{ fontSize: "0.75rem", color: "var(--primary)", fontWeight: 600 }}>Estimated Wait</div>
              <div style={{ fontSize: "1.5rem", fontWeight: 700, color: "var(--primary)", marginTop: "0.2rem" }}>~12 mins</div>
            </div>
          </div>
        </div>
      </div>
    </AppShell>
  );
}
