import { useAuth } from "../../auth/auth-context";
import { AppShell } from "../../components/layout/AppShell";
import { Calendar, Clock, MapPin, CheckCircle2 } from "lucide-react";

export function PatientAppointmentsPage() {
  const { user } = useAuth();

  if (!user) return null;

  return (
    <AppShell role="PATIENT" user={user}>
      <div style={{ display: "flex", flexDirection: "column", gap: "1.75rem" }}>
        
        <div>
          <h1 style={{ fontSize: "1.75rem", fontWeight: 700, color: "var(--text-main)", letterSpacing: "-0.02em" }}>
            My Appointments
          </h1>
          <p style={{ color: "var(--text-muted)", fontSize: "0.92rem", marginTop: "0.2rem" }}>
            View and manage your scheduled doctor consultations.
          </p>
        </div>

        {/* Appointment Card */}
        <div className="card-saas" style={{ display: "flex", justifyContent: "space-between", alignItems: "center", flexWrap: "wrap", gap: "1.25rem" }}>
          <div style={{ display: "flex", gap: "1.25rem", alignItems: "center" }}>
            <div style={{ width: "3rem", height: "3rem", borderRadius: "10px", background: "var(--badge-green-bg)", color: "var(--badge-green-text)", display: "flex", alignItems: "center", justifyContent: "center" }}>
              <Calendar size={22} />
            </div>
            <div>
              <div style={{ display: "flex", alignItems: "center", gap: "0.5rem", marginBottom: "0.2rem" }}>
                <span style={{ fontSize: "0.75rem", fontWeight: 600, color: "var(--primary)", textTransform: "uppercase" }}>Cardiology</span>
                <span style={{ fontSize: "0.75rem", padding: "0.15rem 0.5rem", borderRadius: "9999px", background: "var(--badge-green-bg)", color: "var(--badge-green-text)", fontWeight: 600 }}>
                  Confirmed
                </span>
              </div>
              <h2 style={{ fontSize: "1.15rem", fontWeight: 700, color: "var(--text-main)" }}>
                Dr. Sarah Jenkins, MD
              </h2>
              <div style={{ display: "flex", gap: "1.25rem", color: "var(--text-muted)", fontSize: "0.85rem", marginTop: "0.25rem" }}>
                <span style={{ display: "flex", alignItems: "center", gap: "0.3rem" }}>
                  <Clock size={14} /> Tomorrow at 10:30 AM
                </span>
                <span style={{ display: "flex", alignItems: "center", gap: "0.3rem" }}>
                  <MapPin size={14} /> Room 4B
                </span>
              </div>
            </div>
          </div>

          <div style={{ display: "flex", gap: "0.75rem" }}>
            <button type="button" className="button button-secondary">
              Reschedule
            </button>
            <button type="button" className="button button-primary">
              Check-In Now
            </button>
          </div>
        </div>
      </div>
    </AppShell>
  );
}
