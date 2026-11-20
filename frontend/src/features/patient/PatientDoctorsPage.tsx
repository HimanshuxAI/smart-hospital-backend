import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useMemo, useState } from "react";

import { useApi } from "../../api/api-context";
import type { DirectoryDoctor } from "../../api/types";
import { WorkspacePage } from "../../components/layout/WorkspacePage";
import { Button } from "../../components/ui/button";
import { Dialog } from "../../components/ui/dialog";
import { EmptyState, ErrorState, LoadingState } from "../../components/ui/feedback";
import { SelectField } from "../../components/ui/select";
import { TextField } from "../../components/ui/field";
import { addDaysIsoDate, formatTime } from "../../lib/format";
import { queryKeys } from "../../lib/query-keys";

const dateOptions = Array.from({ length: 7 }, (_, index) => {
  const iso = addDaysIsoDate(index);
  return { value: iso, label: index === 0 ? `Today (${iso})` : iso };
});

export function PatientDoctorsPage() {
  const api = useApi();
  const queryClient = useQueryClient();
  const [search, setSearch] = useState("");
  const [departmentId, setDepartmentId] = useState("all");
  const [selectedDoctor, setSelectedDoctor] = useState<DirectoryDoctor | null>(null);
  const [bookingDate, setBookingDate] = useState(dateOptions[0]?.value ?? addDaysIsoDate(0));
  const [slotId, setSlotId] = useState("");
  const [visitReason, setVisitReason] = useState("");
  const [formError, setFormError] = useState<string | null>(null);

  const doctorsQuery = useQuery({
    queryKey: queryKeys.doctors,
    queryFn: () => api.getDoctors(),
  });

  const departmentsQuery = useQuery({
    queryKey: queryKeys.departments,
    queryFn: () => api.getDepartments(),
  });

  const slotsQuery = useQuery({
    queryKey: queryKeys.slots(selectedDoctor?.id ?? 0, bookingDate),
    queryFn: () => api.getSlots(selectedDoctor?.id ?? 0, bookingDate),
    enabled: selectedDoctor !== null,
  });

  const book = useMutation({
    mutationFn: () =>
      api.bookAppointment({
        slotId: Number(slotId),
        doctorId: selectedDoctor?.id,
        visitReason: visitReason.trim(),
      }),
    onSuccess: async () => {
      setSelectedDoctor(null);
      setSlotId("");
      setVisitReason("");
      setFormError(null);
      await queryClient.invalidateQueries({ queryKey: queryKeys.appointments("patient") });
      await queryClient.invalidateQueries({
        queryKey: queryKeys.slots(selectedDoctor?.id ?? 0, bookingDate),
      });
    },
    onError: () => {
      setFormError("That slot could not be booked. Pick another slot and try again.");
    },
  });

  const doctors = doctorsQuery.data ?? [];
  const departments = departmentsQuery.data ?? [];

  const filteredDoctors = useMemo(() => {
    const term = search.trim().toLowerCase();
    return doctors
      .filter((doctor) =>
        departmentId === "all" ? true : String(doctor.departmentId) === departmentId,
      )
      .filter((doctor) =>
        term.length === 0
          ? true
          : `${doctor.fullName} ${doctor.specialty ?? ""} ${doctor.departmentName ?? ""}`
              .toLowerCase()
              .includes(term),
      );
  }, [departmentId, doctors, search]);

  const availableSlots = (slotsQuery.data ?? []).filter((slot) => slot.status === "OPEN");

  return (
    <WorkspacePage
      role="PATIENT"
      title="Find a doctor"
      description="Browse clinicians by department or specialty, then book an available slot."
    >
      <div className="filter-bar">
        <TextField
          label="Search"
          type="search"
          placeholder="Name, specialty, or department"
          value={search}
          onChange={(event) => setSearch(event.target.value)}
        />
        <SelectField
          label="Department"
          value={departmentId}
          onChange={(event) => setDepartmentId(event.target.value)}
          options={[
            { value: "all", label: "All departments" },
            ...departments.map((department) => ({
              value: String(department.id),
              label: department.name,
            })),
          ]}
        />
      </div>

      {doctorsQuery.isLoading || departmentsQuery.isLoading ? (
        <LoadingState label="Loading doctors..." />
      ) : null}

      {doctorsQuery.isError || departmentsQuery.isError ? (
        <ErrorState
          title="Could not load the directory"
          message="The doctor directory could not be reached. Try again in a moment."
          onRetry={() => void doctorsQuery.refetch()}
        />
      ) : null}

      {!doctorsQuery.isLoading && !doctorsQuery.isError && filteredDoctors.length === 0 ? (
        <EmptyState
          title="No doctors match your search"
          description="Try a different name, specialty, or department."
        />
      ) : null}

      {filteredDoctors.length > 0 ? (
        <>
          <p className="result-count" role="status">
            {filteredDoctors.length} {filteredDoctors.length === 1 ? "doctor" : "doctors"} found
          </p>
          <div className="card-grid">
            {filteredDoctors.map((doctor) => (
              <article className="card doctor-card" key={doctor.id}>
                <div className="card-content">
                  <h2 className="card-title">{doctor.fullName}</h2>
                  <p className="card-description">{doctor.specialty ?? "General practitioner"}</p>
                  <dl className="detail-list">
                    <div>
                      <dt>Department</dt>
                      <dd>{doctor.departmentName ?? "Unassigned"}</dd>
                    </div>
                    <div>
                      <dt>Consultation</dt>
                      <dd>{doctor.averageConsultationMinutes} minutes</dd>
                    </div>
                  </dl>
                  <Button
                    onClick={() => {
                      setSelectedDoctor(doctor);
                      setSlotId("");
                      setFormError(null);
                    }}
                  >
                    Book appointment
                  </Button>
                </div>
              </article>
            ))}
          </div>
        </>
      ) : null}

      <Dialog
        open={selectedDoctor !== null}
        title={`Book with ${selectedDoctor?.fullName ?? ""}`}
        description="Choose an available slot and tell the doctor why you are coming in."
        onClose={() => setSelectedDoctor(null)}
      >
        <form
          className="form-grid"
          onSubmit={(event) => {
            event.preventDefault();
            if (!slotId || visitReason.trim().length === 0) {
              setFormError("Choose a slot and enter a visit reason.");
              return;
            }
            setFormError(null);
            book.mutate();
          }}
        >
          <SelectField
            label="Date"
            value={bookingDate}
            onChange={(event) => {
              setBookingDate(event.target.value);
              setSlotId("");
            }}
            options={dateOptions}
          />

          {slotsQuery.isLoading ? <LoadingState label="Loading slots..." /> : null}

          {!slotsQuery.isLoading && availableSlots.length === 0 ? (
            <EmptyState
              title="No slots on this date"
              description="Try another day to see more availability."
            />
          ) : null}

          {availableSlots.length > 0 ? (
            <SelectField
              label="Available time"
              value={slotId}
              onChange={(event) => setSlotId(event.target.value)}
              options={[
                { value: "", label: "Select a time" },
                ...availableSlots.map((slot) => ({
                  value: String(slot.id),
                  label: formatTime(slot.startsAt),
                })),
              ]}
            />
          ) : null}

          <label className="field">
            <span className="field-label">Visit reason</span>
            <textarea
              className="field-control"
              rows={3}
              maxLength={500}
              value={visitReason}
              onChange={(event) => setVisitReason(event.target.value)}
              required
            />
          </label>

          {formError ? (
            <p className="form-error" role="alert">
              {formError}
            </p>
          ) : null}

          <div className="dialog-actions">
            <Button variant="secondary" onClick={() => setSelectedDoctor(null)}>
              Cancel
            </Button>
            <Button type="submit" loading={book.isPending} loadingLabel="Booking...">
              Confirm booking
            </Button>
          </div>
        </form>
      </Dialog>
    </WorkspacePage>
  );
}
