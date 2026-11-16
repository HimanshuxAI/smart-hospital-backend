import { useAuth } from "../../auth/auth-context";
import { AppShell } from "../../components/layout/AppShell";
import { mockPatientDashboard } from "../../mocks/data";
import { Link } from "react-router-dom";
import { 
  Calendar, 
  Clock, 
  MapPin, 
  Ticket, 
  User, 
  ArrowRight,
  Activity,
  CheckCircle2,
  CalendarCheck
} from "lucide-react";

export function PatientDashboardPage() {
  const { user } = useAuth();
  const data = mockPatientDashboard;
  const isDemo = import.meta.env.VITE_API_MODE === "demo";

  if (!user) return null;

  return (
    <AppShell role="PATIENT" user={user} demoMode={isDemo}>
      <div style={{ display: "flex", flexDirection: "column", gap: "1.75rem" }}>
        
        {/* Welcome Section */}
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-end", flexWrap: "wrap", gap: "1rem" }}>
          <div>
            <h1 style={{ fontSize: "1.75rem", fontWeight: 700, color: "var(--text-main)", letterSpacing: "-0.02em" }}>
              Welcome, {data.patientName}
            </h1>
            <p style={{ color: "var(--text-muted)", fontSize: "0.92rem", marginTop: "0.2rem" }}>
              Here is your healthcare schedule and current queue status.
            </p>
          </div>

          <Link to="/patient/doctors" className="button button-primary">
            <span>Book Appointment</span>
            <ArrowRight size={16} />
          </Link>
        </div>

        {/* 2 Main Cards: Upcoming Appointment & Live Queue */}
        <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(340px, 1fr))", gap: "1.5rem" }}>
          
          {/* Active Queue Ticket Card */}
          <div className="card-saas" style={{ display: "flex", flexDirection: "column", justifyContent: "space-between" }}>
            <div>
              <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "1.25rem" }}>
                <div style={{ display: "flex", alignItems: "center", gap: "0.6rem" }}>
                  <div style={{ width: "2rem", height: "2rem", borderRadius: "6px", background: "var(--primary-subtle)", color: "var(--primary)", display: "flex", alignItems: "center", justifyContent: "center" }}>
                    <Ticket size={17} />
                  </div>
                  <h2 style={{ fontSize: "1.05rem", fontWeight: 600, color: "var(--text-main)" }}>Live Queue Ticket</h2>
                </div>
                
                {data.activeQueueEntry && (
                  <span style={{
                    padding: "0.2rem 0.65rem",
                    borderRadius: "9999px",
                    fontSize: "0.75rem",
                    fontWeight: 600,
                    background: "var(--badge-orange-bg)",
                    color: "var(--badge-orange-text)",
                    border: "1px solid var(--badge-orange-border)"
                  }}>
                    {data.activeQueueEntry.triageLevel} Priority
                  </span>
                )}
              </div>

              {data.activeQueueEntry ? (
                <div>
                  <div style={{
                    background: "var(--bg-subtle)",
                    borderRadius: "10px",
                    padding: "1.5rem",
                    textAlign: "center",
                    border: "1px solid var(--border-color)",
                    marginBottom: "1.25rem"
                  }}>
                    <span style={{ fontSize: "0.75rem", fontWeight: 600, color: "var(--text-muted)", textTransform: "uppercase", letterSpacing: "0.04em" }}>
                      Queue Token
                    </span>
                    <div style={{ fontSize: "3rem", fontWeight: 800, color: "var(--text-main)", lineHeight: 1.1, margin: "0.25rem 0" }}>
                      #{data.activeQueueEntry.tokenNumber}
                    </div>
                    <span style={{ fontSize: "0.8rem", color: "#16a34a", fontWeight: 600 }}>
                      ● {data.activeQueueEntry.queueStatus}
                    </span>
                  </div>

                  <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "0.75rem" }}>
                    <div style={{ padding: "0.85rem", background: "var(--bg-subtle)", borderRadius: "8px", border: "1px solid var(--border-color)" }}>
                      <div style={{ fontSize: "0.75rem", color: "var(--text-muted)", fontWeight: 500 }}>Ahead of you</div>
                      <div style={{ fontSize: "1.25rem", fontWeight: 700, color: "var(--text-main)", marginTop: "0.15rem" }}>
                        {data.activeQueueEntry.position} patients
                      </div>
                    </div>

                    <div style={{ padding: "0.85rem", background: "var(--primary-subtle)", borderRadius: "8px", border: "1px solid #bae6fd" }}>
                      <div style={{ fontSize: "0.75rem", color: "var(--primary)", fontWeight: 600 }}>Est. Wait Time</div>
                      <div style={{ fontSize: "1.25rem", fontWeight: 700, color: "var(--primary)", marginTop: "0.15rem" }}>
                        ~{data.activeQueueEntry.estimatedWaitMinutes} mins
                      </div>
                    </div>
                  </div>
                </div>
              ) : (
                <div style={{ padding: "3rem 1rem", textAlign: "center", color: "var(--text-muted)" }}>
                  <CheckCircle2 size={36} color="#10b981" style={{ margin: "0 auto 0.75rem", opacity: 0.8 }} />
                  <p style={{ fontSize: "0.9rem" }}>No active queue token. Check in to join.</p>
                </div>
              )}
            </div>

            <div style={{ marginTop: "1.5rem", paddingTop: "1rem", borderTop: "1px solid var(--border-color)", display: "flex", justifyContent: "space-between", alignItems: "center" }}>
              <span style={{ fontSize: "0.75rem", color: "var(--text-muted)" }}>Auto-refreshes in real-time</span>
              <Link to="/patient/queue" style={{ fontSize: "0.82rem", fontWeight: 600, color: "var(--primary)", textDecoration: "none" }}>
                View Full Pass →
              </Link>
            </div>
          </div>

          {/* Upcoming Appointment Card */}
          <div className="card-saas" style={{ display: "flex", flexDirection: "column", justifyContent: "space-between" }}>
            <div>
              <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "1.25rem" }}>
                <div style={{ display: "flex", alignItems: "center", gap: "0.6rem" }}>
                  <div style={{ width: "2rem", height: "2rem", borderRadius: "6px", background: "#f0fdf4", color: "#16a34a", display: "flex", alignItems: "center", justifyContent: "center" }}>
                    <Calendar size={17} />
                  </div>
                  <h2 style={{ fontSize: "1.05rem", fontWeight: 600, color: "var(--text-main)" }}>Next Appointment</h2>
                </div>

                {data.upcomingAppointment && (
                  <span style={{
                    padding: "0.2rem 0.65rem",
                    borderRadius: "9999px",
                    fontSize: "0.75rem",
                    fontWeight: 600,
                    background: "var(--badge-green-bg)",
                    color: "var(--badge-green-text)",
                    border: "1px solid var(--badge-green-border)"
                  }}>
                    {data.upcomingAppointment.status}
                  </span>
                )}
              </div>

              {data.upcomingAppointment ? (
                <div style={{ display: "flex", flexDirection: "column", gap: "0.85rem" }}>
                  <div style={{ padding: "0.85rem 1rem", background: "var(--bg-subtle)", borderRadius: "8px", border: "1px solid var(--border-color)" }}>
                    <div style={{ fontSize: "0.75rem", color: "var(--text-muted)" }}>Doctor</div>
                    <div style={{ fontSize: "0.95rem", fontWeight: 600, color: "var(--text-main)", marginTop: "0.15rem" }}>
                      {data.upcomingAppointment.doctorName}
                    </div>
                  </div>

                  <div style={{ padding: "0.85rem 1rem", background: "var(--bg-subtle)", borderRadius: "8px", border: "1px solid var(--border-color)" }}>
                    <div style={{ fontSize: "0.75rem", color: "var(--text-muted)" }}>Department</div>
                    <div style={{ fontSize: "0.95rem", fontWeight: 600, color: "var(--text-main)", marginTop: "0.15rem" }}>
                      {data.upcomingAppointment.departmentName}
                    </div>
                  </div>

                  <div style={{ padding: "0.85rem 1rem", background: "var(--bg-subtle)", borderRadius: "8px", border: "1px solid var(--border-color)" }}>
                    <div style={{ fontSize: "0.75rem", color: "var(--text-muted)" }}>Date & Time</div>
                    <div style={{ fontSize: "0.95rem", fontWeight: 600, color: "var(--text-main)", marginTop: "0.15rem" }}>
                      {new Date(data.upcomingAppointment.startsAt).toLocaleString(undefined, {
                        dateStyle: "medium",
                        timeStyle: "short"
                      })}
                    </div>
                  </div>
                </div>
              ) : (
                <div style={{ padding: "3rem 1rem", textAlign: "center", color: "var(--text-muted)" }}>
                  <CalendarCheck size={36} color="var(--text-light)" style={{ margin: "0 auto 0.75rem" }} />
                  <p style={{ fontSize: "0.9rem" }}>No upcoming appointments scheduled.</p>
                </div>
              )}
            </div>

            <div style={{ marginTop: "1.5rem", paddingTop: "1rem", borderTop: "1px solid var(--border-color)", display: "flex", justifyContent: "space-between", alignItems: "center" }}>
              <span style={{ fontSize: "0.75rem", color: "var(--text-muted)" }}>Confirmed visit</span>
              <Link to="/patient/appointments" style={{ fontSize: "0.82rem", fontWeight: 600, color: "var(--primary)", textDecoration: "none" }}>
                All Appointments →
              </Link>
            </div>
          </div>
        </div>
      </div>
    </AppShell>
  );
}
