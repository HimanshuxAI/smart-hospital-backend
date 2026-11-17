import { useId, type SelectHTMLAttributes } from "react";

export type SelectOption = {
  value: string;
  label: string;
};

export type SelectFieldProps = Omit<
  SelectHTMLAttributes<HTMLSelectElement>,
  "id" | "aria-describedby" | "aria-invalid"
> & {
  label: string;
  options: SelectOption[];
  hint?: string | undefined;
  error?: string | undefined;
};

export function SelectField({
  label,
  options,
  hint,
  error,
  className = "",
  ...props
}: SelectFieldProps) {
  const id = useId();
  const hintId = hint ? `${id}-hint` : undefined;
  const errorId = error ? `${id}-error` : undefined;
  const describedBy = [hintId, errorId].filter(Boolean).join(" ") || undefined;

  return (
    <div className={`field ${error ? "field-invalid" : ""} ${className}`.trim()}>
      <label className="field-label" htmlFor={id}>
        {label}
      </label>
      {hint ? (
        <span className="field-hint" id={hintId}>
          {hint}
        </span>
      ) : null}
      <select
        {...props}
        id={id}
        className="field-control"
        aria-invalid={error ? true : undefined}
        aria-describedby={describedBy}
      >
        {options.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </select>
      {error ? (
        <span className="field-error" id={errorId} role="alert">
          {error}
        </span>
      ) : null}
    </div>
  );
}
