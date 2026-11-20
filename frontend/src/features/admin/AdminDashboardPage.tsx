import { useQuery } from "@tanstack/react-query";

import { useApi } from "../../api/api-context";
import { WorkspacePage } from "../../components/layout/WorkspacePage";
import { Card, CardContent } from "../../components/ui/card";
import { EmptyState, ErrorState, LoadingState } from "../../components/ui/feedback";
import { queryKeys } from "../../lib/query-keys";

export function AdminDashboardPage() {
  const api = useApi();

  const dashboardQuery = useQuery({
    queryKey: queryKeys.adminDashboard,
    queryFn: () => api.getAdminDashboard(),
  });

  const data = dashboardQuery.data;

  return (
    <WorkspacePage
      role="ADMIN"
      title="Operations"
      description="A live snapshot of users, clinicians, and today's hospital activity."
    >
      {dashboardQuery.isLoading ? <LoadingState label="Loading operations..." /> : null}

      {dashboardQuery.isError ? (
        <ErrorState
          title="Could not load operations"
          message="The operations overview could not be reached. Try again in a moment."
          onRetry={() => void dashboardQuery.refetch()}
        />
      ) : null}

      {data ? (
        <div className="metric-grid">
          <Card>
            <CardContent>
              <span className="metric-label">Total users</span>
              <strong className="metric-value">{data.totalUsers}</strong>
            </CardContent>
          </Card>
          <Card>
            <CardContent>
              <span className="metric-label">Active doctors</span>
              <strong className="metric-value">{data.totalDoctors}</strong>
            </CardContent>
          </Card>
          <Card>
            <CardContent>
              <span className="metric-label">Departments</span>
              <strong className="metric-value">{data.totalDepartments}</strong>
            </CardContent>
          </Card>
          <Card>
            <CardContent>
              <span className="metric-label">Appointments today</span>
              <strong className="metric-value">{data.appointmentsToday}</strong>
            </CardContent>
          </Card>
          <Card>
            <CardContent>
              <span className="metric-label">Queued today</span>
              <strong className="metric-value">{data.queueEntriesToday}</strong>
            </CardContent>
          </Card>
        </div>
      ) : null}

      {!dashboardQuery.isLoading && !dashboardQuery.isError && !data ? (
        <EmptyState
          title="No operational data"
          description="The operations overview will populate once the hospital services report in."
        />
      ) : null}
    </WorkspacePage>
  );
}
