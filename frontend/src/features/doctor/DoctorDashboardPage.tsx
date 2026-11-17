import { useQuery } from "@tanstack/react-query";

import { useApi } from "../../api/api-context";
import { WorkspacePage } from "../../components/layout/WorkspacePage";
import { Card, CardContent } from "../../components/ui/card";
import { EmptyState, ErrorState, LoadingState } from "../../components/ui/feedback";
import { queryKeys } from "../../lib/query-keys";

export function DoctorDashboardPage() {
  const api = useApi();

  const dashboardQuery = useQuery({
    queryKey: queryKeys.doctorDashboard,
    queryFn: () => api.getDoctorDashboard(),
  });

  const data = dashboardQuery.data;

  return (
    <WorkspacePage
      role="DOCTOR"
      eyebrow="Doctor workspace"
      title={data ? `Doctor station - ${data.doctorName}` : "Doctor station"}
      description="Today's appointments and queue pressure at a glance."
    >
      {dashboardQuery.isLoading ? <LoadingState label="Loading your station..." /> : null}

      {dashboardQuery.isError ? (
        <ErrorState
          title="Could not load your station"
          message="We could not reach the hospital service. Try again in a moment."
          onRetry={() => void dashboardQuery.refetch()}
        />
      ) : null}

      {data ? (
        <div className="metric-grid">
          <Card>
            <CardContent>
              <span className="metric-label">Appointments today</span>
              <strong className="metric-value">{data.todayAppointmentCount}</strong>
            </CardContent>
          </Card>
          <Card>
            <CardContent>
              <span className="metric-label">Waiting patients</span>
              <strong className="metric-value metric-value-warning">
                {data.waitingCount}
              </strong>
            </CardContent>
          </Card>
          <Card>
            <CardContent>
              <span className="metric-label">In consultation</span>
              <strong className="metric-value metric-value-info">
                {data.inConsultationCount}
              </strong>
            </CardContent>
          </Card>
        </div>
      ) : null}

      {!dashboardQuery.isLoading && !dashboardQuery.isError && !data ? (
        <EmptyState
          title="No station data"
          description="Your appointments and queue counts will appear here once services respond."
        />
      ) : null}
    </WorkspacePage>
  );
}
