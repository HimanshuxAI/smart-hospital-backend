import { useAuth } from "../../auth/auth-context";
import { AppShell } from "../../components/layout/AppShell";
import { Card } from "../../components/ui/card";
import { mockDoctorDashboard } from "../../mocks/data";

export function DoctorDashboardPage() {
  const { user } = useAuth();
  const data = mockDoctorDashboard;
  const isDemo = import.meta.env.VITE_API_MODE === "demo";

  if (!user) return null;

  return (
    <AppShell role="DOCTOR" user={user} demoMode={isDemo}>
      <div style={{ display: "flex", flexDirection: "column", gap: "1.5rem" }}>
        <h1 style={{ fontSize: "1.5rem", fontWeight: "bold" }}>Doctor Station - {data.doctorName}</h1>
        <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(220px, 1fr))", gap: "1rem" }}>
          <Card title="Today's Appointments">
            <p style={{ fontSize: "2rem", fontWeight: "bold", color: "#0f766e" }}>{data.todayAppointmentCount}</p>
          </Card>
          <Card title="Waiting Patients">
            <p style={{ fontSize: "2rem", fontWeight: "bold", color: "#b45309" }}>{data.waitingCount}</p>
          </Card>
          <Card title="In Consultation">
            <p style={{ fontSize: "2rem", fontWeight: "bold", color: "#2563eb" }}>{data.inConsultationCount}</p>
          </Card>
        </div>
      </div>
    </AppShell>
  );
}
