import { useQuery } from "@tanstack/react-query";

import { useApi } from "../../api/api-context";
import { WorkspacePage } from "../../components/layout/WorkspacePage";
import { Card, CardContent } from "../../components/ui/card";
import { EmptyState, ErrorState, LoadingState } from "../../components/ui/feedback";
import { formatDateTime } from "../../lib/format";
import { queryKeys } from "../../lib/query-keys";
import { PriorityBadge } from "../queue/PriorityBadge";

export function PatientDashboardPage() {
  const api = useApi();

  const dashboardQuery = useQuery({
    queryKey: queryKeys.patientDashboard,
    queryFn: () => api.getPatientDashboard(),
  });

  const data = dashboardQuery.data;

  return (
    <WorkspacePage
      role="PATIENT"
      eyebrow="Patient portal"
      title={data ? `Welcome, ${data.patientName}` : "Welcome"}
      description="Your next visit and live queue position at a glance."
    >
      {dashboardQuery.isLoading ? <LoadingState label="Loading your dashboard..." /> : null}

      {dashboardQuery.isError ? (
        <ErrorState
          title="Could not load your dashboard"
          message="We could not reach the hospital service. Try again in a moment."
          onRetry={() => void dashboardQuery.refetch()}
        />
      ) : null}

      {data ? (
        <div className="metric-grid metric-grid-two">
          <Card>
            <CardContent>
              <span className="metric-label">Next appointment</span>
              {data.upcomingAppointment ? (
                <>
                  <strong className="metric-value-text">
                    {data.upcomingAppointment.doctorName}
                  </strong>
                  <p className="metric-hint">{data.upcomingAppointment.departmentName}</p>
                  <p className="metric-hint">
                    {formatDateTime(data.upcomingAppointment.startsAt)}
                  </p>
                </>
              ) : (
                <p className="metric-hint">No upcoming appointments scheduled.</p>
              )}
            </CardContent>
          </Card>

          <Card>
            <CardContent>
              <span className="metric-label">Active queue token</span>
              {data.activeQueueEntry ? (
                <>
                  <strong className="metric-value-text">
                    #{data.activeQueueEntry.tokenNumber}
                  </strong>
                  <p className="metric-hint">
                    <PriorityBadge tier={data.activeQueueEntry.triageLevel} />
                  </p>
                  <p className="metric-hint">
                    {data.activeQueueEntry.position} ahead · about{" "}
                    {data.activeQueueEntry.estimatedWaitMinutes} min
                  </p>
                </>
              ) : (
                <p className="metric-hint">You are not currently in an active queue.</p>
              )}
            </CardContent>
          </Card>
        </div>
      ) : null}

      {!dashboardQuery.isLoading && !dashboardQuery.isError && !data ? (
        <EmptyState
          title="No dashboard data"
          description="Your visits and queue token will appear here once services respond."
        />
      ) : null}
    </WorkspacePage>
  );
}
