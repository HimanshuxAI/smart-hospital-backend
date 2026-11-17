import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";

import { useApi } from "../../api/api-context";
import type { EmergencyTier, QueueEntry } from "../../api/types";
import { WorkspacePage } from "../../components/layout/WorkspacePage";
import { Badge } from "../../components/ui/badge";
import { Button } from "../../components/ui/button";
import { Dialog } from "../../components/ui/dialog";
import { EmptyState, ErrorState, LoadingState } from "../../components/ui/feedback";
import { SelectField } from "../../components/ui/select";
import { DataTable } from "../../components/ui/table";
import { formatTime } from "../../lib/format";
import { queryKeys } from "../../lib/query-keys";
import { sortQueueEntries } from "../../lib/queue";
import { PriorityBadge } from "../queue/PriorityBadge";

const statusTone: Record<string, "neutral" | "info" | "success" | "warning" | "danger"> = {
  WAITING: "warning",
  CALLED: "info",
  IN_CONSULTATION: "success",
  COMPLETED: "neutral",
  SKIPPED: "danger",
  CANCELLED: "danger",
};

const triageOptions: { value: EmergencyTier; label: string }[] = [
  { value: "NORMAL", label: "Normal" },
  { value: "MEDIUM", label: "Medium" },
  { value: "HIGH", label: "High" },
  { value: "CRITICAL", label: "Critical" },
];

export function DoctorQueuePage() {
  const api = useApi();
  const queryClient = useQueryClient();
  const [skipTarget, setSkipTarget] = useState<QueueEntry | null>(null);
  const [skipReason, setSkipReason] = useState("");

  const profileQuery = useQuery({
    queryKey: queryKeys.ownDoctorProfile,
    queryFn: () => api.getOwnDoctorProfile(),
  });

  const doctorId = profileQuery.data?.id;

  const queueQuery = useQuery({
    queryKey: queryKeys.queue(`doctor-${doctorId ?? "own"}`),
    queryFn: () => api.getQueue({ doctorId }),
    enabled: doctorId !== undefined,
  });

  const invalidate = async () => {
    await queryClient.invalidateQueries({
      queryKey: queryKeys.queue(`doctor-${doctorId ?? "own"}`),
    });
    await queryClient.invalidateQueries({ queryKey: queryKeys.doctorDashboard });
  };

  const transition = useMutation({
    mutationFn: (input: {
      action: "call" | "serve" | "complete";
      entryId: number;
    }) => {
      if (input.action === "call") {
        return api.callQueueEntry(input.entryId);
      }
      if (input.action === "serve") {
        return api.serveQueueEntry(input.entryId);
      }
      return api.completeQueueEntry(input.entryId);
    },
    onSuccess: invalidate,
  });

  const classify = useMutation({
    mutationFn: (input: { entryId: number; triageLevel: EmergencyTier }) =>
      api.classifyQueueEntry(input.entryId, input.triageLevel),
    onSuccess: invalidate,
  });

  const skip = useMutation({
    mutationFn: (input: { entryId: number; reason: string }) =>
      api.skipQueueEntry(input.entryId, input.reason),
    onSuccess: async () => {
      setSkipTarget(null);
      setSkipReason("");
      await invalidate();
    },
  });

  const entries = sortQueueEntries(queueQuery.data ?? []);

  const actionsFor = (entry: QueueEntry) => {
    if (entry.queueStatus === "WAITING") {
      return (
        <Button
          size="sm"
          loading={transition.isPending}
          onClick={() => transition.mutate({ action: "call", entryId: entry.queueEntryId })}
        >
          Call
        </Button>
      );
    }
    if (entry.queueStatus === "CALLED") {
      return (
        <div className="row-actions">
          <Button
            size="sm"
            loading={transition.isPending}
            onClick={() => transition.mutate({ action: "serve", entryId: entry.queueEntryId })}
          >
            Start consultation
          </Button>
          <Button variant="secondary" size="sm" onClick={() => setSkipTarget(entry)}>
            Skip
          </Button>
        </div>
      );
    }
    if (entry.queueStatus === "IN_CONSULTATION") {
      return (
        <Button
          size="sm"
          loading={transition.isPending}
          onClick={() => transition.mutate({ action: "complete", entryId: entry.queueEntryId })}
        >
          Complete
        </Button>
      );
    }
    return <span className="muted-text">No actions available</span>;
  };

  return (
    <WorkspacePage
      role="DOCTOR"
      eyebrow="Doctor workspace"
      title="Live queue"
      description="Patients ordered by triage priority, then by arrival time."
    >
      {profileQuery.isLoading || queueQuery.isLoading ? (
        <LoadingState label="Loading queue..." />
      ) : null}

      {profileQuery.isError || queueQuery.isError ? (
        <ErrorState
          title="Could not load the queue"
          message="The live queue could not be reached. Try again in a moment."
          onRetry={() => void queueQuery.refetch()}
        />
      ) : null}

      {!queueQuery.isLoading && !queueQuery.isError && entries.length === 0 ? (
        <EmptyState
          title="The queue is clear"
          description="No patients are waiting right now. New check-ins appear here automatically."
        />
      ) : null}

      {entries.length > 0 ? (
        <DataTable
          caption="Live patient queue"
          rows={entries}
          rowKey={(entry) => String(entry.queueEntryId)}
          columns={[
            {
              key: "token",
              header: "Token",
              render: (entry) => (
                <span className="table-primary">#{entry.tokenNumber}</span>
              ),
            },
            {
              key: "priority",
              header: "Priority",
              render: (entry) => <PriorityBadge tier={entry.triageLevel} />,
            },
            {
              key: "arrival",
              header: "Arrived",
              render: (entry) => formatTime(entry.arrivedAt),
            },
            {
              key: "position",
              header: "Position",
              render: (entry) =>
                entry.position > 0 ? `${entry.position} in line` : "With doctor",
            },
            {
              key: "status",
              header: "Status",
              render: (entry) => (
                <Badge tone={statusTone[entry.queueStatus] ?? "neutral"}>
                  {entry.queueStatus.replace("_", " ")}
                </Badge>
              ),
            },
            {
              key: "triage",
              header: "Triage",
              render: (entry) => (
                <SelectField
                  label={`Triage for token ${entry.tokenNumber}`}
                  className="inline-field"
                  value={entry.triageLevel}
                  onChange={(event) =>
                    classify.mutate({
                      entryId: entry.queueEntryId,
                      triageLevel: event.target.value as EmergencyTier,
                    })
                  }
                  options={triageOptions}
                />
              ),
            },
            {
              key: "actions",
              header: "Actions",
              render: (entry) => actionsFor(entry),
            },
          ]}
        />
      ) : null}

      <Dialog
        open={skipTarget !== null}
        title="Skip patient"
        description="Skipped patients keep their token and can be returned to the queue."
        onClose={() => setSkipTarget(null)}
      >
        <form
          className="form-grid"
          onSubmit={(event) => {
            event.preventDefault();
            if (!skipTarget || skipReason.trim().length === 0) {
              return;
            }
            skip.mutate({ entryId: skipTarget.queueEntryId, reason: skipReason.trim() });
          }}
        >
          <label className="field">
            <span className="field-label">Reason</span>
            <textarea
              className="field-control"
              rows={3}
              value={skipReason}
              onChange={(event) => setSkipReason(event.target.value)}
              required
            />
          </label>
          <div className="dialog-actions">
            <Button variant="secondary" onClick={() => setSkipTarget(null)}>
              Keep in queue
            </Button>
            <Button type="submit" variant="danger" loading={skip.isPending} loadingLabel="Skipping...">
              Skip patient
            </Button>
          </div>
        </form>
      </Dialog>
    </WorkspacePage>
  );
}
