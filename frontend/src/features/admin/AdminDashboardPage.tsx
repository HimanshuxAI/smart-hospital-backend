import { useAuth } from "../../auth/auth-context";
import { AppShell } from "../../components/layout/AppShell";
import { Card } from "../../components/ui/card";
import { mockAdminDashboard } from "../../mocks/data";

export function AdminDashboardPage() {
  const { user } = useAuth();
  const data = mockAdminDashboard;
  const isDemo = import.meta.env.VITE_API_MODE === "demo";

  if (!user) return null;

  return (
    <AppShell role="ADMIN" user={user} demoMode={isDemo}>
      <div style={{ display: "flex", flexDirection: "column", gap: "1.5rem" }}>
        <h1 style={{ fontSize: "1.5rem", fontWeight: "bold" }}>System Administration</h1>
        <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(200px, 1fr))", gap: "1rem" }}>
          <Card title="Total Users">
            <p style={{ fontSize: "1.75rem", fontWeight: "bold" }}>{data.totalUsers}</p>
          </Card>
          <Card title="Active Doctors">
            <p style={{ fontSize: "1.75rem", fontWeight: "bold" }}>{data.totalDoctors}</p>
          </Card>
          <Card title="Departments">
            <p style={{ fontSize: "1.75rem", fontWeight: "bold" }}>{data.totalDepartments}</p>
          </Card>
          <Card title="Appointments Today">
            <p style={{ fontSize: "1.75rem", fontWeight: "bold" }}>{data.appointmentsToday}</p>
          </Card>
          <Card title="Queued Today">
            <p style={{ fontSize: "1.75rem", fontWeight: "bold" }}>{data.queueEntriesToday}</p>
          </Card>
        </div>
      </div>
    </AppShell>
  );
}
