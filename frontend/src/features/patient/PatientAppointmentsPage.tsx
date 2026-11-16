import { useAuth } from "../../auth/auth-context";
import { AppShell } from "../../components/layout/AppShell";
import { Calendar, Clock, MapPin, CheckCircle2 } from "lucide-react";

export function PatientAppointmentsPage() {
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
              [ PATIENT RECORDS // SCHEDULED VISITS ]
            </div>
            <h1 style={{
              fontFamily: "var(--font-serif)",
              fontSize: "clamp(2.5rem, 4vw, 3.5rem)",
              margin: 0,
              color: "var(--color-ink)",
              lineHeight: 1.05
            }}>
              My <em style={{ fontStyle: "italic", color: "var(--accent-vermilion)" }}>Appointments</em>
            </h1>
          </div>
        </div>

        {/* Appointment List */}
        <div style={{ display: "flex", flexDirection: "column", gap: "1.25rem" }}>
          <div style={{
            background: "#ffffff",
            border: "2px solid var(--color-ink)",
            borderRadius: "8px",
            padding: "2rem",
            boxShadow: "var(--shadow-hard-md)",
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
            flexWrap: "wrap",
            gap: "1.5rem"
          }}>
            <div style={{ display: "flex", gap: "1.5rem", alignItems: "center" }}>
              <div style={{
                width: "3.5rem",
                height: "3.5rem",
                border: "1.5px solid var(--color-ink)",
                borderRadius: "6px",
                background: "var(--accent-wasabi)",
                display: "grid",
                placeItems: "center",
                boxShadow: "2px 2px 0px var(--color-ink)"
              }}>
                <Calendar size={24} color="var(--color-ink)" />
              </div>
              <div>
                <span style={{ fontFamily: "var(--font-mono)", fontSize: "0.72rem", color: "var(--accent-vermilion)", fontWeight: 700, textTransform: "uppercase" }}>
                  CARDIOVASCULAR MEDICINE // CONFIRMED
                </span>
                <h2 style={{ fontFamily: "var(--font-display)", fontSize: "1.35rem", fontWeight: 800, margin: "0.2rem 0", color: "var(--color-ink)" }}>
                  Dr. Sarah Jenkins, MD
                </h2>
                <div style={{ display: "flex", gap: "1.25rem", color: "var(--color-muted)", fontSize: "0.85rem" }}>
                  <span style={{ display: "flex", alignItems: "center", gap: "0.3rem" }}>
                    <Clock size={15} /> Tomorrow at 10:30 AM
                  </span>
                  <span style={{ display: "flex", alignItems: "center", gap: "0.3rem" }}>
                    <MapPin size={15} /> Clinic Room 4B
                  </span>
                </div>
              </div>
            </div>

            <div style={{ display: "flex", gap: "0.75rem" }}>
              <button type="button" className="button button-secondary">
                Reschedule
              </button>
              <button type="button" className="button button-primary">
                Check In Now
              </button>
            </div>
          </div>
        </div>
      </div>
    </AppShell>
  );
}
