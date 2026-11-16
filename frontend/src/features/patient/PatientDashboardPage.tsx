import { useAuth } from "../../auth/auth-context";
import { AppShell } from "../../components/layout/AppShell";
import { Card } from "../../components/ui/card";
import { PriorityBadge } from "../queue/PriorityBadge";
import { mockPatientDashboard } from "../../mocks/data";
import { 
  Calendar, 
  Clock, 
  MapPin, 
  Sparkles, 
  Ticket, 
  AlertCircle, 
  CheckCircle2, 
  ChevronRight,
  ShieldCheck,
  Stethoscope
} from "lucide-react";

export function PatientDashboardPage() {
  const { user } = useAuth();
  const data = mockPatientDashboard;
  const isDemo = import.meta.env.VITE_API_MODE === "demo";

  if (!user) return null;

  return (
    <AppShell role="PATIENT" user={user} demoMode={isDemo}>
      <div style={{ display: "flex", flexDirection: "column", gap: "2rem" }}>
        
        {/* Hero Welcome Banner */}
        <div style={{
          position: "relative",
          borderRadius: "1.5rem",
          padding: "2.5rem",
          background: "linear-gradient(135deg, #0b1329 0%, #0f233f 60%, #075985 100%)",
          color: "#ffffff",
          overflow: "hidden",
          boxShadow: "0 10px 30px -5px rgba(11, 19, 41, 0.3)"
        }}>
          {/* Subtle Accent Glow */}
          <div style={{
            position: "absolute",
            top: "-30%",
            right: "-10%",
            width: "300px",
            height: "300px",
            borderRadius: "50%",
            background: "radial-gradient(circle, rgba(56, 189, 248, 0.25) 0%, transparent 70%)",
            filter: "blur(40px)"
          }} />

          <div style={{ position: "relative", zIndex: 1, maxWidth: "42rem" }}>
            <div style={{
              display: "inline-flex",
              alignItems: "center",
              gap: "0.5rem",
              padding: "0.3rem 0.75rem",
              borderRadius: "9999px",
              background: "rgba(255, 255, 255, 0.1)",
              backdropFilter: "blur(8px)",
              fontSize: "0.75rem",
              fontWeight: 700,
              letterSpacing: "0.06em",
              textTransform: "uppercase",
              color: "#38bdf8",
              marginBottom: "1rem"
            }}>
              <ShieldCheck size={14} /> Verified Patient Portal
            </div>

            <h1 style={{ fontSize: "2.25rem", fontWeight: 800, letterSpacing: "-0.03em", margin: "0 0 0.5rem" }}>
              Welcome back, {data.patientName}
            </h1>
            <p style={{ margin: 0, fontSize: "1rem", color: "#94a3b8", lineHeight: 1.5 }}>
              Track your upcoming clinical appointments, active queue position, and ML predicted wait times live.
            </p>
          </div>
        </div>

        {/* Dynamic Grid: Upcoming Appointment & Live Queue Ticket */}
        <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(360px, 1fr))", gap: "1.75rem" }}>
          
          {/* Upcoming Appointment Card */}
          <div style={{
            background: "#ffffff",
            borderRadius: "1.25rem",
            border: "1px solid #e2e8f0",
            padding: "1.75rem",
            boxShadow: "var(--shadow-card)",
            display: "flex",
            flexDirection: "column",
            justifyContent: "space-between"
          }}>
            <div>
              <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "1.25rem" }}>
                <div style={{ display: "flex", alignItems: "center", gap: "0.6rem" }}>
                  <div style={{
                    width: "2.25rem",
                    height: "2.25rem",
                    borderRadius: "0.65rem",
                    background: "#e0f2fe",
                    color: "#0284c7",
                    display: "grid",
                    placeItems: "center"
                  }}>
                    <Calendar size={18} />
                  </div>
                  <div>
                    <h2 style={{ fontSize: "1.1rem", fontWeight: 700, margin: 0, color: "#0f172a" }}>
                      Upcoming Consultation
                    </h2>
                    <span style={{ fontSize: "0.75rem", color: "#64748b" }}>Scheduled visit</span>
                  </div>
                </div>

                {data.upcomingAppointment && (
                  <span style={{
                    padding: "0.25rem 0.75rem",
                    borderRadius: "9999px",
                    fontSize: "0.75rem",
                    fontWeight: 700,
                    textTransform: "uppercase",
                    letterSpacing: "0.04em",
                    background: "#ecfdf5",
                    color: "#059669",
                    border: "1px solid #a7f3d0"
                  }}>
                    {data.upcomingAppointment.status}
                  </span>
                )}
              </div>

              {data.upcomingAppointment ? (
                <div style={{ display: "flex", flexDirection: "column", gap: "1rem", marginTop: "1rem" }}>
                  <div style={{ display: "flex", alignItems: "center", gap: "0.75rem", padding: "0.75rem", background: "#f8fafc", borderRadius: "0.75rem" }}>
                    <Stethoscope size={20} color="#0d9488" />
                    <div>
                      <div style={{ fontSize: "0.75rem", color: "#64748b" }}>Attending Physician</div>
                      <div style={{ fontSize: "0.95rem", fontWeight: 700, color: "#1e293b" }}>{data.upcomingAppointment.doctorName}</div>
                    </div>
                  </div>

                  <div style={{ display: "flex", alignItems: "center", gap: "0.75rem", padding: "0.75rem", background: "#f8fafc", borderRadius: "0.75rem" }}>
                    <MapPin size={20} color="#6366f1" />
                    <div>
                      <div style={{ fontSize: "0.75rem", color: "#64748b" }}>Clinical Department</div>
                      <div style={{ fontSize: "0.95rem", fontWeight: 700, color: "#1e293b" }}>{data.upcomingAppointment.departmentName}</div>
                    </div>
                  </div>

                  <div style={{ display: "flex", alignItems: "center", gap: "0.75rem", padding: "0.75rem", background: "#f8fafc", borderRadius: "0.75rem" }}>
                    <Clock size={20} color="#0284c7" />
                    <div>
                      <div style={{ fontSize: "0.75rem", color: "#64748b" }}>Date & Time</div>
                      <div style={{ fontSize: "0.95rem", fontWeight: 700, color: "#1e293b" }}>
                        {new Date(data.upcomingAppointment.startsAt).toLocaleString(undefined, {
                          dateStyle: "medium",
                          timeStyle: "short"
                        })}
                      </div>
                    </div>
                  </div>
                </div>
              ) : (
                <div style={{ padding: "2.5rem 1rem", textAlign: "center", color: "#94a3b8" }}>
                  <Calendar size={36} style={{ margin: "0 auto 0.75rem", opacity: 0.5 }} />
                  <p style={{ margin: 0, fontWeight: 500 }}>No upcoming appointments scheduled.</p>
                </div>
              )}
            </div>

            <div style={{ marginTop: "1.5rem", paddingTop: "1rem", borderTop: "1px solid #f1f5f9" }}>
              <button
                type="button"
                className="button button-secondary"
                style={{ width: "100%", justifyContent: "space-between" }}
              >
                <span>Book New Appointment</span>
                <ChevronRight size={16} />
              </button>
            </div>
          </div>

          {/* Active Live Queue Ticket Card */}
          <div style={{
            background: "#ffffff",
            borderRadius: "1.25rem",
            border: "1px solid #e2e8f0",
            padding: "1.75rem",
            boxShadow: "var(--shadow-card)",
            display: "flex",
            flexDirection: "column",
            justifyContent: "space-between"
          }}>
            <div>
              <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "1.25rem" }}>
                <div style={{ display: "flex", alignItems: "center", gap: "0.6rem" }}>
                  <div style={{
                    width: "2.25rem",
                    height: "2.25rem",
                    borderRadius: "0.65rem",
                    background: "#ccfbf1",
                    color: "#0d9488",
                    display: "grid",
                    placeItems: "center"
                  }}>
                    <Ticket size={18} />
                  </div>
                  <div>
                    <h2 style={{ fontSize: "1.1rem", fontWeight: 700, margin: 0, color: "#0f172a" }}>
                      Active Live Ticket
                    </h2>
                    <span style={{ fontSize: "0.75rem", color: "#64748b" }}>Real-time triage queue</span>
                  </div>
                </div>

                {data.activeQueueEntry && (
                  <PriorityBadge tier={data.activeQueueEntry.triageLevel} />
                )}
              </div>

              {data.activeQueueEntry ? (
                <div>
                  {/* Token Big Display Banner */}
                  <div style={{
                    borderRadius: "1rem",
                    background: "linear-gradient(135deg, #f0fdf4 0%, #e0f2fe 100%)",
                    border: "1px solid #bae6fd",
                    padding: "1.5rem",
                    textAlign: "center",
                    marginBottom: "1.25rem"
                  }}>
                    <div style={{ fontSize: "0.75rem", fontWeight: 700, textTransform: "uppercase", letterSpacing: "0.08em", color: "#0369a1" }}>
                      Your Token Number
                    </div>
                    <div style={{ fontSize: "3rem", fontWeight: 800, color: "#0f172a", letterSpacing: "-0.04em", lineHeight: 1.1 }}>
                      #{data.activeQueueEntry.tokenNumber}
                    </div>
                    <div style={{ fontSize: "0.85rem", color: "#047857", fontWeight: 600, marginTop: "0.25rem" }}>
                      ● {data.activeQueueEntry.queueStatus}
                    </div>
                  </div>

                  {/* Estimated Wait & Position metrics */}
                  <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "1rem" }}>
                    <div style={{ padding: "1rem", background: "#f8fafc", borderRadius: "0.75rem", border: "1px solid #f1f5f9" }}>
                      <div style={{ fontSize: "0.75rem", color: "#64748b", fontWeight: 600 }}>Position in Line</div>
                      <div style={{ fontSize: "1.5rem", fontWeight: 800, color: "#0f172a" }}>
                        {data.activeQueueEntry.position} <span style={{ fontSize: "0.85rem", fontWeight: 500, color: "#64748b" }}>ahead</span>
                      </div>
                    </div>

                    <div style={{ padding: "1rem", background: "#f8fafc", borderRadius: "0.75rem", border: "1px solid #f1f5f9" }}>
                      <div style={{ display: "flex", alignItems: "center", gap: "0.3rem", fontSize: "0.75rem", color: "#0284c7", fontWeight: 700 }}>
                        <Sparkles size={13} /> ML Estimated
                      </div>
                      <div style={{ fontSize: "1.5rem", fontWeight: 800, color: "#0284c7" }}>
                        ~{data.activeQueueEntry.estimatedWaitMinutes} <span style={{ fontSize: "0.85rem", fontWeight: 500, color: "#64748b" }}>mins</span>
                      </div>
                    </div>
                  </div>
                </div>
              ) : (
                <div style={{ padding: "2.5rem 1rem", textAlign: "center", color: "#94a3b8" }}>
                  <CheckCircle2 size={36} color="#059669" style={{ margin: "0 auto 0.75rem", opacity: 0.7 }} />
                  <p style={{ margin: 0, fontWeight: 500 }}>You are not currently in an active queue.</p>
                </div>
              )}
            </div>

            <div style={{ marginTop: "1.5rem", paddingTop: "1rem", borderTop: "1px solid #f1f5f9" }}>
              <div style={{ display: "flex", alignItems: "center", gap: "0.5rem", fontSize: "0.78rem", color: "#64748b" }}>
                <span className="status-dot" />
                Live socket connected • updates automatically
              </div>
            </div>
          </div>
        </div>
      </div>
    </AppShell>
  );
}
