import { useQuery } from "@tanstack/react-query";

import { useApi } from "../../api/api-context";
import { WorkspacePage } from "../../components/layout/WorkspacePage";
import { Badge } from "../../components/ui/badge";
import { EmptyState, ErrorState, LoadingState } from "../../components/ui/feedback";
import { formatDateTime, formatWait } from "../../lib/format";
import { queryKeys } from "../../lib/query-keys";
import { PriorityBadge } from "../queue/PriorityBadge";

const activeStatuses = new Set(["WAITING", "CALLED", "IN_CONSULTATION"]);

export function PatientQueuePage() {
  const api = useApi();

  const queueQuery = useQuery({
    queryKey: queryKeys.queue("patient"),
    queryFn: () => api.getQueue(),
  });

  const dashboardQuery = useQuery({
    queryKey: queryKeys.patientDashboard,
    queryFn: () => api.getPatientDashboard(),
  });

  const entries = queueQuery.data ?? [];
  const activeEntry = entries.find((entry) => activeStatuses.has(entry.queueStatus));
  const dashboardEntry = dashboardQuery.data?.activeQueueEntry ?? null;
  const waitMinutes = dashboardEntry?.estimatedWaitMinutes ?? null;

  return (
    <WorkspacePage
      role="PATIENT"
      eyebrow="Patient portal"
      title="Queue token"
      description="Your live position, triage level, and estimated waiting time."
    >
      {queueQuery.isLoading ? <LoadingState label="Loading your token..." /> : null}

      {queueQuery.isError ? (
        <ErrorState
          title="Could not load your token"
          message="Your queue status could not be reached. Try again in a moment."
          onRetry={() => void queueQuery.refetch()}
        />
      ) : null}

      {!queueQuery.isLoading && !queueQuery.isError && !activeEntry ? (
        <EmptyState
          title="You are not in the queue"
          description="Check in at the hospital reception and your token will appear here."
        />
      ) : null}

      {activeEntry ? (
        <article className="card token-card">
          <div className="card-content token-card-content">
            <span className="token-label">Your queue token</span>
            <strong className="token-number">#{activeEntry.tokenNumber}</strong>

            <dl className="detail-list">
              <div>
                <dt>Status</dt>
                <dd>
                  <Badge tone={activeEntry.queueStatus === "WAITING" ? "warning" : "info"}>
                    {activeEntry.queueStatus.replace("_", " ")}
                  </Badge>
                </dd>
              </div>
              <div>
                <dt>Triage</dt>
                <dd>
                  <PriorityBadge tier={activeEntry.triageLevel} />
                </dd>
              </div>
              <div>
                <dt>Position</dt>
                <dd>
                  {activeEntry.position > 0
                    ? `${activeEntry.position} ${activeEntry.position === 1 ? "person" : "people"} ahead of you`
                    : "You are with the doctor"}
                </dd>
              </div>
              <div>
                <dt>Estimated wait</dt>
                <dd>{formatWait(waitMinutes)}</dd>
              </div>
              <div>
                <dt>Arrived</dt>
                <dd>{formatDateTime(activeEntry.arrivedAt)}</dd>
              </div>
            </dl>

            <p className="muted-text">
              Keep this page open and the token updates as the queue progresses.
            </p>
          </div>
        </article>
      ) : null}
    </WorkspacePage>
  );
}
