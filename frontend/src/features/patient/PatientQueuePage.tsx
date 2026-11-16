import { useAuth } from "../../auth/auth-context";
import { AppShell } from "../../components/layout/AppShell";
import { Ticket, Clock, CheckCircle2, AlertTriangle, ArrowUpRight } from "lucide-react";

export function PatientQueuePage() {
  const { user } = useAuth();

  if (!user) return null;

  return (
    <AppShell role="PATIENT" user={user}>
      <div style={{ display: "flex", flexDirection: "column", gap: "2.5rem" }}>
        
        {/* Header */}
        <div style={{
          display: "flex",
          justifyContent: "space-between",
          alignItems: "flex-end",
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
              [ REAL-TIME TRIAGE // LIVE PASS ]
            </div>
            <h1 style={{
              fontFamily: "var(--font-serif)",
              fontSize: "clamp(2.5rem, 4vw, 3.5rem)",
              margin: 0,
              color: "var(--color-ink)",
              lineHeight: 1.05
            }}>
              Queue <em style={{ fontStyle: "italic", color: "var(--accent-vermilion)" }}>Token</em>
            </h1>
          </div>
        </div>

        {/* Big Ticket Display */}
        <div style={{
          maxWidth: "600px",
          margin: "0 auto",
          width: "100%",
          background: "#ffffff",
          border: "2.5px solid var(--color-ink)",
          borderRadius: "8px",
          padding: "2.5rem",
          boxShadow: "var(--shadow-hard-lg)",
          textAlign: "center"
        }}>
          <div style={{ fontFamily: "var(--font-mono)", fontSize: "0.78rem", color: "var(--color-muted)", textTransform: "uppercase" }}>
            HOSPITAL ADMISSION TICKET // ACTIVE
          </div>

          <div style={{
            fontFamily: "var(--font-display)",
            fontSize: "5.5rem",
            fontWeight: 800,
            color: "var(--color-ink)",
            letterSpacing: "-0.05em",
            lineHeight: 1,
            margin: "1rem 0"
          }}>
            #T-104
          </div>

          <div style={{
            display: "inline-block",
            padding: "0.25rem 0.85rem",
            border: "1.5px solid var(--color-ink)",
            background: "var(--accent-wasabi)",
            borderRadius: "4px",
            fontFamily: "var(--font-mono)",
            fontSize: "0.8rem",
            fontWeight: 700,
            textTransform: "uppercase",
            marginBottom: "2rem"
          }}>
            TRIAGE PRIORITY: NORMAL (LEVEL 3)
          </div>

          <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "1rem", textAlign: "left" }}>
            <div style={{ padding: "1.25rem", border: "1.5px solid var(--color-ink)", borderRadius: "4px", background: "var(--bg-canvas)" }}>
              <div style={{ fontFamily: "var(--font-mono)", fontSize: "0.7rem", color: "var(--color-muted)" }}>WAITING IN FRONT</div>
              <div style={{ fontFamily: "var(--font-display)", fontSize: "2rem", fontWeight: 800 }}>2 Patients</div>
            </div>

            <div style={{ padding: "1.25rem", border: "1.5px solid var(--color-ink)", borderRadius: "4px", background: "var(--accent-vermilion-soft)" }}>
              <div style={{ fontFamily: "var(--font-mono)", fontSize: "0.7rem", color: "var(--accent-vermilion)", fontWeight: 700 }}>ML ESTIMATED TIME</div>
              <div style={{ fontFamily: "var(--font-display)", fontSize: "2rem", fontWeight: 800, color: "var(--accent-vermilion)" }}>~12 mins</div>
            </div>
          </div>
        </div>
      </div>
    </AppShell>
  );
}
