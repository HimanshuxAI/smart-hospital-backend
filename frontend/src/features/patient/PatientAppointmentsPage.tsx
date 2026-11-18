import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";

import { useApi } from "../../api/api-context";
import type { Appointment } from "../../api/types";
import { WorkspacePage } from "../../components/layout/WorkspacePage";
import { Badge } from "../../components/ui/badge";
import { Button } from "../../components/ui/button";
import { Dialog } from "../../components/ui/dialog";
import { EmptyState, ErrorState, LoadingState } from "../../components/ui/feedback";
import { DataTable } from "../../components/ui/table";
import { formatDate, formatDateTime } from "../../lib/format";
import { queryKeys } from "../../lib/query-keys";

const statusTone: Record<string, "neutral" | "info" | "success" | "warning" | "danger"> = {
  BOOKED: "info",
  CONFIRMED: "info",
  CHECKED_IN: "warning",
  COMPLETED: "success",
  CANCELLED: "danger",
};

export function PatientAppointmentsPage() {
  const api = useApi();
  const queryClient = useQueryClient();
  const [cancelTarget, setCancelTarget] = useState<Appointment | null>(null);
  const [reason, setReason] = useState("");
  const [notice, setNotice] = useState<string | null>(null);

  const appointmentsQuery = useQuery({
    queryKey: queryKeys.appointments("patient"),
    queryFn: () => api.getAppointments(),
  });

  const invalidate = async () => {
    await queryClient.invalidateQueries({ queryKey: queryKeys.appointments("patient") });
    await queryClient.invalidateQueries({ queryKey: queryKeys.patientDashboard });
    await queryClient.invalidateQueries({ queryKey: queryKeys.queue("patient") });
  };

  const cancel = useMutation({
    mutationFn: (input: { appointmentId: number; reason: string }) =>
      api.cancelAppointment(input.appointmentId, input.reason),
    onSuccess: async () => {
      setCancelTarget(null);
      setReason("");
      setNotice("The appointment was cancelled.");
      await invalidate();
    },
  });

  const checkIn = useMutation({
    mutationFn: (appointmentId: number) => api.checkInAppointment(appointmentId),
    onSuccess: async (result) => {
      setNotice(`You checked in. Your queue token is #${result.tokenNumber}.`);
      await invalidate();
    },
  });

  const appointments = appointmentsQuery.data ?? [];
  const now = Date.now();
  const upcoming = appointments.filter(
    (appointment) =>
      appointment.status !== "CANCELLED" &&
      appointment.status !== "COMPLETED" &&
      new Date(appointment.startsAt).getTime() >= now,
  );
  const history = appointments.filter((appointment) => !upcoming.includes(appointment));

  const columns = (allowActions: boolean) => [
    {
      key: "date",
      header: "Date",
      render: (appointment: Appointment) => (
        <span className="table-primary">{formatDate(appointment.appointmentDate)}</span>
      ),
    },
    {
      key: "time",
      header: "Time",
      render: (appointment: Appointment) => formatDateTime(appointment.startsAt),
    },
    {
      key: "reason",
      header: "Visit reason",
      render: (appointment: Appointment) => appointment.visitReason ?? "—",
    },
    {
      key: "status",
      header: "Status",
      render: (appointment: Appointment) => (
        <Badge tone={statusTone[appointment.status] ?? "neutral"}>
          {appointment.status.replace("_", " ")}
        </Badge>
      ),
    },
    ...(allowActions
      ? [
          {
            key: "actions",
            header: "Actions",
            render: (appointment: Appointment) => (
              <div className="row-actions">
                {appointment.status === "BOOKED" || appointment.status === "CONFIRMED" ? (
                  <>
                    <Button
                      size="sm"
                      loading={checkIn.isPending}
                      onClick={() => checkIn.mutate(appointment.id)}
                    >
                      Check in
                    </Button>
                    <Button
                      variant="secondary"
                      size="sm"
                      onClick={() => setCancelTarget(appointment)}
                    >
                      Cancel
                    </Button>
                  </>
                ) : (
                  <span className="muted-text">No actions available</span>
                )}
              </div>
            ),
          },
        ]
      : []),
  ];

  return (
    <WorkspacePage
      role="PATIENT"
      title="My appointments"
      description="Upcoming visits, check-in, and your appointment history."
    >
      {notice ? (
        <p className="inline-notice" role="status">
          {notice}
        </p>
      ) : null}

      {appointmentsQuery.isLoading ? <LoadingState label="Loading appointments..." /> : null}

      {appointmentsQuery.isError ? (
        <ErrorState
          title="Could not load appointments"
          message="Your appointments could not be reached. Try again in a moment."
          onRetry={() => void appointmentsQuery.refetch()}
        />
      ) : null}

      {!appointmentsQuery.isLoading && !appointmentsQuery.isError && appointments.length === 0 ? (
        <EmptyState
          title="No appointments yet"
          description="Find a doctor and book your first visit."
        />
      ) : null}

      {upcoming.length > 0 ? (
        <>
          <h2 className="section-title">Upcoming</h2>
          <DataTable
            caption="Upcoming appointments"
            rows={upcoming}
            rowKey={(appointment) => String(appointment.id)}
            columns={columns(true)}
          />
        </>
      ) : null}

      {history.length > 0 ? (
        <>
          <h2 className="section-title">Past and cancelled</h2>
          <DataTable
            caption="Past appointments"
            rows={history}
            rowKey={(appointment) => String(appointment.id)}
            columns={columns(false)}
          />
        </>
      ) : null}

      <Dialog
        open={cancelTarget !== null}
        title="Cancel appointment"
        description="Cancelling releases the slot for another patient."
        onClose={() => setCancelTarget(null)}
      >
        <form
          className="form-grid"
          onSubmit={(event) => {
            event.preventDefault();
            if (!cancelTarget || reason.trim().length === 0) {
              return;
            }
            cancel.mutate({ appointmentId: cancelTarget.id, reason: reason.trim() });
          }}
        >
          <label className="field">
            <span className="field-label">Reason</span>
            <textarea
              className="field-control"
              rows={3}
              maxLength={500}
              value={reason}
              onChange={(event) => setReason(event.target.value)}
              required
            />
          </label>
          <div className="dialog-actions">
            <Button variant="secondary" onClick={() => setCancelTarget(null)}>
              Keep appointment
            </Button>
            <Button type="submit" variant="danger" loading={cancel.isPending} loadingLabel="Cancelling...">
              Cancel appointment
            </Button>
          </div>
        </form>
      </Dialog>
    </WorkspacePage>
  );
}
