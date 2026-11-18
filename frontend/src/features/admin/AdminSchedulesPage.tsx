import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";

import { useApi } from "../../api/api-context";
import { WorkspacePage } from "../../components/layout/WorkspacePage";
import { Badge } from "../../components/ui/badge";
import { Button } from "../../components/ui/button";
import { Dialog } from "../../components/ui/dialog";
import { EmptyState, ErrorState, LoadingState } from "../../components/ui/feedback";
import { SelectField } from "../../components/ui/select";
import { DataTable } from "../../components/ui/table";
import { TextField } from "../../components/ui/field";
import { addDaysIsoDate, formatTime, todayIsoDate } from "../../lib/format";
import { queryKeys } from "../../lib/query-keys";

const dateOptions = Array.from({ length: 7 }, (_, index) => {
  const iso = addDaysIsoDate(index);
  return { value: iso, label: index === 0 ? `Today (${iso})` : iso };
});

export function AdminSchedulesPage() {
  const api = useApi();
  const queryClient = useQueryClient();
  const [doctorId, setDoctorId] = useState("");
  const [date, setDate] = useState(todayIsoDate());
  const [dialogOpen, setDialogOpen] = useState(false);
  const [startTime, setStartTime] = useState("09:00");
  const [duration, setDuration] = useState("30");
  const [reasonLabel, setReasonLabel] = useState("General consultation");
  const [formError, setFormError] = useState<string | null>(null);

  const doctorsQuery = useQuery({
    queryKey: queryKeys.doctors,
    queryFn: () => api.getDoctors(),
  });

  const doctors = doctorsQuery.data ?? [];
  const activeDoctorId = doctorId || (doctors[0] ? String(doctors[0].id) : "");

  const slotsQuery = useQuery({
    queryKey: queryKeys.slots(Number(activeDoctorId), date),
    queryFn: () => api.getAdminSlots(Number(activeDoctorId), date),
    enabled: activeDoctorId !== "",
  });

  const createSlot = useMutation({
    mutationFn: () => {
      const startsAt = new Date(`${date}T${startTime}:00`);
      const endsAt = new Date(startsAt.getTime() + Number(duration) * 60000);
      return api.createSlot({
        doctorId: Number(activeDoctorId),
        startsAt: startsAt.toISOString(),
        endsAt: endsAt.toISOString(),
        reasonLabel,
      });
    },
    onSuccess: async () => {
      setDialogOpen(false);
      setFormError(null);
      await queryClient.invalidateQueries({
        queryKey: queryKeys.slots(Number(activeDoctorId), date),
      });
    },
    onError: () => {
      setFormError("The slot could not be created. Check the time and try again.");
    },
  });

  const cancelSlot = useMutation({
    mutationFn: (slotId: number) => api.cancelSlot(slotId),
    onSuccess: async () => {
      await queryClient.invalidateQueries({
        queryKey: queryKeys.slots(Number(activeDoctorId), date),
      });
    },
  });

  const slots = slotsQuery.data ?? [];

  return (
    <WorkspacePage
      role="ADMIN"
      title="Schedules"
      description="Publish and withdraw doctor consultation slots."
      actions={
        <Button
          onClick={() => setDialogOpen(true)}
          disabled={activeDoctorId === ""}
        >
          Add slot
        </Button>
      }
    >
      <div className="filter-bar">
        <SelectField
          label="Doctor"
          value={activeDoctorId}
          onChange={(event) => setDoctorId(event.target.value)}
          options={[
            { value: "", label: "Select a doctor" },
            ...doctors.map((doctor) => ({
              value: String(doctor.id),
              label: doctor.fullName,
            })),
          ]}
        />
        <SelectField
          label="Date"
          value={date}
          onChange={(event) => setDate(event.target.value)}
          options={dateOptions}
        />
      </div>

      {slotsQuery.isLoading ? <LoadingState label="Loading slots..." /> : null}

      {slotsQuery.isError ? (
        <ErrorState
          title="Could not load slots"
          message="The schedule could not be reached. Try again in a moment."
          onRetry={() => void slotsQuery.refetch()}
        />
      ) : null}

      {!slotsQuery.isLoading && !slotsQuery.isError && slots.length === 0 ? (
        <EmptyState
          title="No slots on this date"
          description="Publish a consultation slot so patients can book it."
          action={
            <Button onClick={() => setDialogOpen(true)} disabled={activeDoctorId === ""}>
              Add slot
            </Button>
          }
        />
      ) : null}

      {slots.length > 0 ? (
        <DataTable
          caption="Doctor schedule slots"
          rows={slots}
          rowKey={(slot) => String(slot.id)}
          columns={[
            {
              key: "start",
              header: "Start",
              render: (slot) => <span className="table-primary">{formatTime(slot.startsAt)}</span>,
            },
            {
              key: "end",
              header: "End",
              render: (slot) => formatTime(slot.endsAt),
            },
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
            {
              key: "actions",
              header: "Actions",
              render: (slot) => (
                <Button
                  variant="secondary"
                  size="sm"
                  disabled={slot.status === "CANCELLED" || cancelSlot.isPending}
                  onClick={() => cancelSlot.mutate(slot.id)}
                >
                  {slot.status === "CANCELLED" ? "Cancelled" : "Cancel slot"}
                </Button>
              ),
            },
          ]}
        />
      ) : null}

      <Dialog
        open={dialogOpen}
        title="Add slot"
        description="Publish a consultation slot for the selected doctor."
        onClose={() => setDialogOpen(false)}
      >
        <form
          className="form-grid"
          onSubmit={(event) => {
            event.preventDefault();
            if (!activeDoctorId) {
              setFormError("Select a doctor first.");
              return;
            }
            setFormError(null);
            createSlot.mutate();
          }}
        >
          <TextField
            label="Start time"
            type="time"
            value={startTime}
            onChange={(event) => setStartTime(event.target.value)}
            required
          />
          <TextField
            label="Duration (minutes)"
            type="number"
            min={5}
            max={240}
            value={duration}
            onChange={(event) => setDuration(event.target.value)}
            required
          />
          <TextField
            label="Reason label"
            value={reasonLabel}
            onChange={(event) => setReasonLabel(event.target.value)}
          />

          {formError ? (
            <p className="form-error" role="alert">
              {formError}
            </p>
          ) : null}

          <div className="dialog-actions">
            <Button variant="secondary" onClick={() => setDialogOpen(false)}>
              Cancel
            </Button>
            <Button type="submit" loading={createSlot.isPending} loadingLabel="Publishing...">
              Publish slot
            </Button>
          </div>
        </form>
      </Dialog>
    </WorkspacePage>
  );
}
