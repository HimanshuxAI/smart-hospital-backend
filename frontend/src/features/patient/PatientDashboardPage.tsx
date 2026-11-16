import { useAuth } from "../../auth/auth-context";
import { AppShell } from "../../components/layout/AppShell";
import { Card } from "../../components/ui/card";
import { PriorityBadge } from "../queue/PriorityBadge";
import { mockPatientDashboard } from "../../mocks/data";

export function PatientDashboardPage() {
  const { user } = useAuth();
  const data = mockPatientDashboard;
  const isDemo = import.meta.env.VITE_API_MODE === "demo";

  if (!user) return null;

  return (
    <AppShell role="PATIENT" user={user} demoMode={isDemo}>
      <div style={{ display: "flex", flexDirection: "column", gap: "1.5rem" }}>
        <h1 style={{ fontSize: "1.5rem", fontWeight: "bold" }}>Welcome, {data.patientName}</h1>
        <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(300px, 1fr))", gap: "1.5rem" }}>
          <Card title="Upcoming Appointment">
            {data.upcomingAppointment ? (
              <div style={{ display: "flex", flexDirection: "column", gap: "0.5rem" }}>
                <div><strong>Doctor:</strong> {data.upcomingAppointment.doctorName}</div>
                <div><strong>Department:</strong> {data.upcomingAppointment.departmentName}</div>
                <div><strong>Time:</strong> {new Date(data.upcomingAppointment.startsAt).toLocaleString()}</div>
                <div><strong>Status:</strong> <span style={{ color: "#0f766e", fontWeight: 600 }}>{data.upcomingAppointment.status}</span></div>
              </div>
            ) : (
              <p style={{ color: "#64748b" }}>No upcoming appointments scheduled.</p>
            )}
          </Card>

          <Card title="Active Queue Status">
            {data.activeQueueEntry ? (
              <div style={{ display: "flex", flexDirection: "column", gap: "0.75rem" }}>
                <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                  <span><strong>Token:</strong> #{data.activeQueueEntry.tokenNumber}</span>
                  <PriorityBadge tier={data.activeQueueEntry.triageLevel} />
                </div>
                <div><strong>Queue Status:</strong> {data.activeQueueEntry.queueStatus}</div>
                <div><strong>Position in Line:</strong> {data.activeQueueEntry.position} ahead</div>
                <div><strong>Estimated Wait:</strong> ~{data.activeQueueEntry.estimatedWaitMinutes} minutes</div>
              </div>
            ) : (
              <p style={{ color: "#64748b" }}>You are not currently in an active queue.</p>
            )}
          </Card>
        </div>
      </div>
    </AppShell>
  );
}
