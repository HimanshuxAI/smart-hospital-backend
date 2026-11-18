import { useQuery } from "@tanstack/react-query";
import { useState } from "react";

import { useApi } from "../../api/api-context";
import { WorkspacePage } from "../../components/layout/WorkspacePage";
import { Badge } from "../../components/ui/badge";
import { EmptyState, ErrorState, LoadingState } from "../../components/ui/feedback";
import { SelectField } from "../../components/ui/select";
import { DataTable } from "../../components/ui/table";
import { addDaysIsoDate, formatTime, todayIsoDate } from "../../lib/format";
import { queryKeys } from "../../lib/query-keys";

const dateOptions = Array.from({ length: 7 }, (_, index) => {
  const iso = addDaysIsoDate(index);
  return { value: iso, label: index === 0 ? `Today (${iso})` : iso };
});

export function DoctorSchedulePage() {
  const api = useApi();
  const [date, setDate] = useState(todayIsoDate());

  const profileQuery = useQuery({
    queryKey: queryKeys.ownDoctorProfile,
    queryFn: () => api.getOwnDoctorProfile(),
  });

  const doctorId = profileQuery.data?.id;

  const slotsQuery = useQuery({
    queryKey: queryKeys.slots(doctorId ?? 0, date),
    queryFn: () => api.getSlots(doctorId ?? 0, date),
    enabled: doctorId !== undefined,
  });

  const appointmentsQuery = useQuery({
    queryKey: queryKeys.appointments(`doctor-${doctorId ?? "own"}-${date}`),
    queryFn: () => api.getAppointments({ doctorId, date }),
    enabled: doctorId !== undefined,
  });

  const slots = slotsQuery.data ?? [];
  const appointments = appointmentsQuery.data ?? [];

  return (
    <WorkspacePage
      role="DOCTOR"
      title="My schedule"
      description="Consultation slots and booked appointments for the selected day."
    >
      <div className="filter-bar">
        <SelectField
          label="Date"
          value={date}
          onChange={(event) => setDate(event.target.value)}
          options={dateOptions}
        />
      </div>

      {profileQuery.isLoading || slotsQuery.isLoading ? (
        <LoadingState label="Loading schedule..." />
      ) : null}

      {profileQuery.isError || slotsQuery.isError ? (
        <ErrorState
          title="Could not load the schedule"
          message="The schedule could not be reached. Try again in a moment."
          onRetry={() => void slotsQuery.refetch()}
        />
      ) : null}

      {!slotsQuery.isLoading && !slotsQuery.isError && slots.length === 0 ? (
        <EmptyState
          title="No slots published"
          description="There are no consultation slots for this date yet."
        />
      ) : null}

      {slots.length > 0 ? (
        <DataTable
          caption="Consultation slots"
          rows={slots}
          rowKey={(slot) => String(slot.id)}
          columns={[
            {
              key: "start",
              header: "Start",
              render: (slot) => <span className="table-primary">{formatTime(slot.startsAt)}</span>,
            },
            { key: "end", header: "End", render: (slot) => formatTime(slot.endsAt) },
            {
              key: "reason",
              header: "Reason",
              render: (slot) => slot.reasonLabel ?? "Consultation",
            },
            {
              key: "status",
              header: "Status",
              render: (slot) => (
                <Badge
                  tone={
                    slot.status === "AVAILABLE"
                      ? "success"
                      : slot.status === "BOOKED"
                        ? "info"
                        : "neutral"
                  }
                >
                  {slot.status}
                </Badge>
              ),
            },
          ]}
        />
      ) : null}

      <h2 className="section-title">Booked appointments</h2>

      {appointmentsQuery.isLoading ? <LoadingState label="Loading appointments..." /> : null}

      {appointmentsQuery.isError ? (
        <ErrorState
          title="Could not load appointments"
          message="The appointment list could not be reached. Try again in a moment."
          onRetry={() => void appointmentsQuery.refetch()}
        />
      ) : null}

      {!appointmentsQuery.isLoading && !appointmentsQuery.isError && appointments.length === 0 ? (
        <EmptyState
          title="Nothing booked for this date"
          description="Appointments booked for this day will appear here."
        />
      ) : null}

      {appointments.length > 0 ? (
        <DataTable
          caption="Booked appointments"
          rows={appointments}
          rowKey={(appointment) => String(appointment.id)}
          columns={[
            {
              key: "time",
              header: "Time",
              render: (appointment) => (
                <span className="table-primary">{formatTime(appointment.startsAt)}</span>
              ),
            },
            {
              key: "reason",
              header: "Visit reason",
              render: (appointment) => appointment.visitReason ?? "—",
            },
            {
              key: "status",
              header: "Status",
              render: (appointment) => (
                <Badge tone={appointment.status === "COMPLETED" ? "success" : "info"}>
                  {appointment.status.replace("_", " ")}
                </Badge>
              ),
            },
          ]}
        />
      ) : null}
    </WorkspacePage>
  );
}
