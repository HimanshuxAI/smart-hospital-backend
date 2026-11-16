import { useAuth } from "../../auth/auth-context";
import { AppShell } from "../../components/layout/AppShell";
import { PriorityBadge } from "../queue/PriorityBadge";
import { mockPatientDashboard } from "../../mocks/data";
import { Link } from "react-router-dom";
import { 
  Calendar, 
  Clock, 
  MapPin, 
  Ticket, 
  ArrowUpRight, 
  User, 
  CheckCircle2, 
  Activity
} from "lucide-react";

export function PatientDashboardPage() {
  const { user } = useAuth();
  const data = mockPatientDashboard;
  const isDemo = import.meta.env.VITE_API_MODE === "demo";

  if (!user) return null;

  return (
    <AppShell role="PATIENT" user={user} demoMode={isDemo}>
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
              [ PATIENT PORTAL // LIVE STATUS ]
            </div>
            <h1 style={{
              fontFamily: "var(--font-serif)",
              fontSize: "clamp(2.5rem, 4vw, 3.5rem)",
              margin: 0,
              color: "var(--color-ink)",
              lineHeight: 1.05
            }}>
              Good day, <em style={{ fontStyle: "italic", color: "var(--accent-vermilion)" }}>{data.patientName}</em>.
            </h1>
          </div>

          <div style={{
            display: "flex",
            alignItems: "center",
            gap: "0.75rem",
            padding: "0.5rem 1rem",
            background: "#ffffff",
            border: "1.5px solid var(--color-ink)",
            borderRadius: "4px",
            boxShadow: "var(--shadow-hard-xs)",
            fontFamily: "var(--font-mono)",
            fontSize: "0.78rem",
            fontWeight: 700
          }}>
            <span className="status-dot" /> ACTIVE DISPATCH ONLINE
          </div>
        </div>

        {/* 2 Physical Asymmetric Cards */}
        <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(360px, 1fr))", gap: "2rem" }}>
          
          {/* Active Live Ticket Board */}
          <div style={{
            background: "#ffffff",
            border: "2px solid var(--color-ink)",
            borderRadius: "8px",
            padding: "2rem",
            boxShadow: "var(--shadow-hard-md)",
            display: "flex",
            flexDirection: "column",
            justifyContent: "space-between"
          }}>
            <div>
              <div style={{
                display: "flex",
                justifyContent: "space-between",
                alignItems: "center",
                paddingBottom: "1rem",
                marginBottom: "1.5rem",
                borderBottom: "1.5px dashed var(--color-border)"
              }}>
                <span style={{ fontFamily: "var(--font-mono)", fontSize: "0.75rem", fontWeight: 700, textTransform: "uppercase" }}>
                  // LIVE QUEUE ADMISSION
                </span>
                {data.activeQueueEntry && <PriorityBadge tier={data.activeQueueEntry.triageLevel} />}
              </div>

              {data.activeQueueEntry ? (
                <div>
                  {/* Huge Raw Ticket Block */}
                  <div style={{
                    background: "var(--bg-canvas)",
                    border: "1.5px solid var(--color-ink)",
                    borderRadius: "4px",
                    padding: "1.75rem",
                    textAlign: "center",
                    marginBottom: "1.5rem"
                  }}>
                    <div style={{ fontFamily: "var(--font-mono)", fontSize: "0.75rem", color: "var(--color-muted)", textTransform: "uppercase" }}>
                      QUEUED TOKEN IDENTIFIER
                    </div>
                    <div style={{
                      fontFamily: "var(--font-display)",
                      fontSize: "3.75rem",
                      fontWeight: 800,
                      color: "var(--color-ink)",
                      lineHeight: 1.1,
                      letterSpacing: "-0.04em",
                      margin: "0.25rem 0"
                    }}>
                      #{data.activeQueueEntry.tokenNumber}
                    </div>
                    <div style={{
                      display: "inline-block",
                      padding: "0.2rem 0.65rem",
                      border: "1px solid var(--color-ink)",
                      background: "var(--accent-wasabi)",
                      fontFamily: "var(--font-mono)",
                      fontSize: "0.72rem",
                      fontWeight: 700,
                      textTransform: "uppercase",
                      marginTop: "0.5rem"
                    }}>
                      STATUS: {data.activeQueueEntry.queueStatus}
                    </div>
                  </div>

                  {/* Dual Metrics */}
                  <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "1rem" }}>
                    <div style={{ padding: "1.25rem", border: "1.5px solid var(--color-ink)", borderRadius: "4px", background: "#ffffff" }}>
                      <div style={{ fontFamily: "var(--font-mono)", fontSize: "0.72rem", color: "var(--color-muted)", textTransform: "uppercase" }}>
                        POSITION IN LINE
                      </div>
                      <div style={{ fontFamily: "var(--font-display)", fontSize: "2rem", fontWeight: 800, color: "var(--color-ink)" }}>
                        {data.activeQueueEntry.position}
                      </div>
                      <div style={{ fontSize: "0.75rem", color: "var(--color-muted)" }}>patients ahead</div>
                    </div>

                    <div style={{ padding: "1.25rem", border: "1.5px solid var(--color-ink)", borderRadius: "4px", background: "var(--accent-vermilion-soft)" }}>
                      <div style={{ fontFamily: "var(--font-mono)", fontSize: "0.72rem", color: "var(--accent-vermilion)", fontWeight: 700, textTransform: "uppercase" }}>
                        ML ESTIMATED WAIT
                      </div>
                      <div style={{ fontFamily: "var(--font-display)", fontSize: "2rem", fontWeight: 800, color: "var(--accent-vermilion)" }}>
                        ~{data.activeQueueEntry.estimatedWaitMinutes}m
                      </div>
                      <div style={{ fontSize: "0.75rem", color: "var(--accent-vermilion)", fontWeight: 600 }}>regressor active</div>
                    </div>
                  </div>
                </div>
              ) : (
                <div style={{ padding: "3rem 1rem", textAlign: "center", color: "var(--color-muted)" }}>
                  <Ticket size={40} style={{ margin: "0 auto 1rem", strokeWidth: 1.5 }} />
                  <p style={{ margin: 0, fontFamily: "var(--font-display)", fontWeight: 700 }}>No active queue token found.</p>
                </div>
              )}
            </div>

            <div style={{
              marginTop: "2rem",
              paddingTop: "1.25rem",
              borderTop: "1.5px dashed var(--color-border)",
              display: "flex",
              justifyContent: "space-between",
              alignItems: "center"
            }}>
              <span style={{ fontFamily: "var(--font-mono)", fontSize: "0.72rem", color: "var(--color-muted)" }}>
                DATA REFRESH: REALTIME WEBSOCKET STREAM
              </span>
              <Link to="/patient/queue" style={{
                fontFamily: "var(--font-mono)",
                fontSize: "0.75rem",
                fontWeight: 700,
                color: "var(--accent-vermilion)",
                textDecoration: "none"
              }}>
                FULL PASS →
              </Link>
            </div>
          </div>

          {/* Upcoming Consultation */}
          <div style={{
            background: "#ffffff",
            border: "2px solid var(--color-ink)",
            borderRadius: "8px",
            padding: "2rem",
            boxShadow: "var(--shadow-hard-md)",
            display: "flex",
            flexDirection: "column",
            justifyContent: "space-between"
          }}>
            <div>
              <div style={{
                display: "flex",
                justifyContent: "space-between",
                alignItems: "center",
                paddingBottom: "1rem",
                marginBottom: "1.5rem",
                borderBottom: "1.5px dashed var(--color-border)"
              }}>
                <span style={{ fontFamily: "var(--font-mono)", fontSize: "0.75rem", fontWeight: 700, textTransform: "uppercase" }}>
                  // CLINICAL SCHEDULE
                </span>
                {data.upcomingAppointment && (
                  <span style={{
                    fontFamily: "var(--font-mono)",
                    fontSize: "0.72rem",
                    padding: "0.15rem 0.6rem",
                    border: "1px solid var(--color-ink)",
                    background: "var(--accent-wasabi)",
                    fontWeight: 700,
                    textTransform: "uppercase"
                  }}>
                    {data.upcomingAppointment.status}
                  </span>
                )}
              </div>

              {data.upcomingAppointment ? (
                <div style={{ display: "flex", flexDirection: "column", gap: "1rem" }}>
                  <div style={{ padding: "1.25rem", border: "1.5px solid var(--color-ink)", borderRadius: "4px" }}>
                    <div style={{ fontFamily: "var(--font-mono)", fontSize: "0.7rem", color: "var(--color-muted)", textTransform: "uppercase" }}>
                      ATTENDING PHYSICIAN
                    </div>
                    <div style={{ fontFamily: "var(--font-display)", fontSize: "1.25rem", fontWeight: 800, color: "var(--color-ink)", marginTop: "0.25rem" }}>
                      {data.upcomingAppointment.doctorName}
                    </div>
                  </div>

                  <div style={{ padding: "1.25rem", border: "1.5px solid var(--color-ink)", borderRadius: "4px" }}>
                    <div style={{ fontFamily: "var(--font-mono)", fontSize: "0.7rem", color: "var(--color-muted)", textTransform: "uppercase" }}>
                      DEPARTMENT
                    </div>
                    <div style={{ fontFamily: "var(--font-display)", fontSize: "1.25rem", fontWeight: 800, color: "var(--color-ink)", marginTop: "0.25rem" }}>
                      {data.upcomingAppointment.departmentName}
                    </div>
                  </div>

                  <div style={{ padding: "1.25rem", border: "1.5px solid var(--color-ink)", borderRadius: "4px", background: "var(--bg-canvas)" }}>
                    <div style={{ fontFamily: "var(--font-mono)", fontSize: "0.7rem", color: "var(--color-muted)", textTransform: "uppercase" }}>
                      SCHEDULED APPOINTMENT TIME
                    </div>
                    <div style={{ fontFamily: "var(--font-mono)", fontSize: "1.1rem", fontWeight: 700, color: "var(--color-ink)", marginTop: "0.25rem" }}>
                      {new Date(data.upcomingAppointment.startsAt).toLocaleString()}
                    </div>
                  </div>
                </div>
              ) : (
                <div style={{ padding: "3rem 1rem", textAlign: "center", color: "var(--color-muted)" }}>
                  <Calendar size={40} style={{ margin: "0 auto 1rem", strokeWidth: 1.5 }} />
                  <p style={{ margin: 0, fontFamily: "var(--font-display)", fontWeight: 700 }}>No bookings on record.</p>
                </div>
              )}
            </div>

            <div style={{ marginTop: "2rem", paddingTop: "1.25rem", borderTop: "1.5px dashed var(--color-border)" }}>
              <Link
                to="/patient/doctors"
                className="button button-primary"
                style={{ width: "100%", justifyContent: "space-between", background: "var(--accent-vermilion)", borderColor: "var(--color-ink)" }}
              >
                <span>BOOK NEW SLOT</span>
                <ArrowUpRight size={18} />
              </Link>
            </div>
          </div>
        </div>
      </div>
    </AppShell>
  );
}
