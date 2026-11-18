import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";

import { useApi } from "../../api/api-context";
import { WorkspacePage } from "../../components/layout/WorkspacePage";
import { Button } from "../../components/ui/button";
import { Dialog } from "../../components/ui/dialog";
import { EmptyState, ErrorState, LoadingState } from "../../components/ui/feedback";
import { SelectField } from "../../components/ui/select";
import { DataTable } from "../../components/ui/table";
import { TextField } from "../../components/ui/field";
import { queryKeys } from "../../lib/query-keys";

type CreateDoctorForm = {
  fullName: string;
  email: string;
  password: string;
  phone: string;
  departmentId: string;
  specialty: string;
  licenseNumber: string;
  averageConsultationMinutes: string;
};

const emptyForm: CreateDoctorForm = {
  fullName: "",
  email: "",
  password: "",
  phone: "",
  departmentId: "",
  specialty: "",
  licenseNumber: "",
  averageConsultationMinutes: "30",
};

export function AdminDoctorsPage() {
  const api = useApi();
  const queryClient = useQueryClient();
  const [dialogOpen, setDialogOpen] = useState(false);
  const [form, setForm] = useState<CreateDoctorForm>(emptyForm);
  const [formError, setFormError] = useState<string | null>(null);

  const doctorsQuery = useQuery({
    queryKey: queryKeys.doctors,
    queryFn: () => api.getDoctors(),
  });

  const departmentsQuery = useQuery({
    queryKey: queryKeys.departments,
    queryFn: () => api.getDepartments(),
  });

  const createDoctor = useMutation({
    mutationFn: () =>
      api.createDoctor({
        fullName: form.fullName,
        email: form.email,
        password: form.password,
        phone: form.phone,
        departmentId: Number(form.departmentId),
        specialty: form.specialty,
        licenseNumber: form.licenseNumber,
        averageConsultationMinutes: Number(form.averageConsultationMinutes) || 30,
      }),
    onSuccess: async () => {
      setDialogOpen(false);
      setForm(emptyForm);
      setFormError(null);
      await queryClient.invalidateQueries({ queryKey: queryKeys.doctors });
    },
    onError: () => {
      setFormError("The doctor could not be created. Check the details and try again.");
    },
  });

  const doctors = doctorsQuery.data ?? [];
  const departments = departmentsQuery.data ?? [];

  const update = <Key extends keyof CreateDoctorForm>(
    key: Key,
    value: CreateDoctorForm[Key],
  ) => setForm((current) => ({ ...current, [key]: value }));

  return (
    <WorkspacePage
      role="ADMIN"
      title="Doctors"
      description="Maintain the doctor directory that patients can browse and book."
      actions={
        <Button onClick={() => setDialogOpen(true)}>Add doctor</Button>
      }
    >
      {doctorsQuery.isLoading ? <LoadingState label="Loading doctors..." /> : null}

      {doctorsQuery.isError ? (
        <ErrorState
          title="Could not load doctors"
          message="The doctor directory could not be reached. Try again in a moment."
          onRetry={() => void doctorsQuery.refetch()}
        />
      ) : null}

      {!doctorsQuery.isLoading && !doctorsQuery.isError && doctors.length === 0 ? (
        <EmptyState
          title="No doctors registered"
          description="Add the first doctor so patients can start booking appointments."
          action={<Button onClick={() => setDialogOpen(true)}>Add doctor</Button>}
        />
      ) : null}

      {doctors.length > 0 ? (
        <DataTable
          caption="Doctor directory"
          rows={doctors}
          rowKey={(doctor) => String(doctor.id)}
          columns={[
            {
              key: "name",
              header: "Name",
              render: (doctor) => <span className="table-primary">{doctor.fullName}</span>,
            },
            {
              key: "department",
              header: "Department",
              render: (doctor) => doctor.departmentName ?? "Unassigned",
            },
            {
              key: "specialty",
              header: "Specialty",
              render: (doctor) => doctor.specialty ?? "—",
            },
            {
              key: "duration",
              header: "Consultation",
              render: (doctor) => `${doctor.averageConsultationMinutes} min`,
            },
          ]}
        />
      ) : null}

      <Dialog
        open={dialogOpen}
        title="Add doctor"
        description="Create a doctor account and attach it to a department."
        onClose={() => setDialogOpen(false)}
      >
        <form
          className="form-grid"
          onSubmit={(event) => {
            event.preventDefault();
            if (!form.fullName || !form.email || !form.departmentId) {
              setFormError("Name, email, and department are required.");
              return;
            }
            setFormError(null);
            createDoctor.mutate();
          }}
        >
          <TextField
            label="Full name"
            value={form.fullName}
            onChange={(event) => update("fullName", event.target.value)}
            required
          />
          <TextField
            label="Email"
            type="email"
            value={form.email}
            onChange={(event) => update("email", event.target.value)}
            required
          />
          <TextField
            label="Temporary password"
            type="password"
            hint="At least 8 characters."
            value={form.password}
            onChange={(event) => update("password", event.target.value)}
            required
          />
          <TextField
            label="Phone"
            value={form.phone}
            onChange={(event) => update("phone", event.target.value)}
          />
          <SelectField
            label="Department"
            value={form.departmentId}
            onChange={(event) => update("departmentId", event.target.value)}
            options={[
              { value: "", label: "Select a department" },
              ...departments.map((department) => ({
                value: String(department.id),
                label: department.name,
              })),
            ]}
          />
          <TextField
            label="Specialty"
            value={form.specialty}
            onChange={(event) => update("specialty", event.target.value)}
          />
          <TextField
            label="License number"
            value={form.licenseNumber}
            onChange={(event) => update("licenseNumber", event.target.value)}
          />
          <TextField
            label="Average consultation minutes"
            type="number"
            min={1}
            max={240}
            value={form.averageConsultationMinutes}
            onChange={(event) => update("averageConsultationMinutes", event.target.value)}
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
            <Button type="submit" loading={createDoctor.isPending} loadingLabel="Creating...">
              Create doctor
            </Button>
          </div>
        </form>
      </Dialog>
    </WorkspacePage>
  );
}
