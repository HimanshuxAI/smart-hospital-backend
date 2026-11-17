import { useQuery } from "@tanstack/react-query";

import { useApi } from "../../api/api-context";
import { WorkspacePage } from "../../components/layout/WorkspacePage";
import { Badge } from "../../components/ui/badge";
import { EmptyState, ErrorState, LoadingState } from "../../components/ui/feedback";
import { DataTable } from "../../components/ui/table";
import { formatDate, formatTime } from "../../lib/format";
import { queryKeys } from "../../lib/query-keys";

export function DoctorPatientsPage() {
  const api = useApi();

  const profileQuery = useQuery({
    queryKey: queryKeys.ownDoctorProfile,
    queryFn: () => api.getOwnDoctorProfile(),
  });

  const doctorId = profileQuery.data?.id;

  const appointmentsQuery = useQuery({
    queryKey: queryKeys.appointments(`doctor-${doctorId ?? "own"}-all`),
    queryFn: () => api.getAppointments({ doctorId }),
    enabled: doctorId !== undefined,
  });

  const appointments = appointmentsQuery.data ?? [];

  return (
    <WorkspacePage
      role="DOCTOR"
      eyebrow="Doctor workspace"
      title="Patients"
      description="Appointments assigned to you, with visit reasons and arrival times."
    >
      {profileQuery.isLoading || appointmentsQuery.isLoading ? (
        <LoadingState label="Loading patients..." />
      ) : null}

      {profileQuery.isError || appointmentsQuery.isError ? (
        <ErrorState
          title="Could not load patients"
          message="The patient list could not be reached. Try again in a moment."
          onRetry={() => void appointmentsQuery.refetch()}
        />
      ) : null}

      {!appointmentsQuery.isLoading && !appointmentsQuery.isError && appointments.length === 0 ? (
        <EmptyState
          title="No patients assigned"
          description="Appointments booked with you will show their visit details here."
        />
      ) : null}

      {appointments.length > 0 ? (
        <DataTable
          caption="Patients assigned to this doctor"
          rows={appointments}
          rowKey={(appointment) => String(appointment.id)}
          columns={[
            {
              key: "patient",
              header: "Patient reference",
              render: (appointment) => (
                <span className="table-primary">Patient #{appointment.patientId}</span>
              ),
            },
            {
              key: "reason",
              header: "Visit reason",
              render: (appointment) => appointment.visitReason ?? "—",
            },
            {
              key: "date",
              header: "Date",
              render: (appointment) => formatDate(appointment.appointmentDate),
            },
            {
              key: "time",
              header: "Time",
              render: (appointment) => formatTime(appointment.startsAt),
            },
            {
              key: "status",
              header: "Status",
              render: (appointment) => (
                <Badge
                  tone={
                    appointment.status === "CANCELLED"
                      ? "danger"
                      : appointment.status === "COMPLETED"
                        ? "success"
                        : "info"
                  }
                >
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
