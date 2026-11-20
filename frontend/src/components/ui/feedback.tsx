import type { ReactNode } from "react";

export function Skeleton({ className = "" }: { className?: string }) {
  return <span className={`skeleton ${className}`.trim()} aria-hidden="true" />;
}

export function LoadingState({ label = "Loading" }: { label?: string }) {
  return (
    <div className="page-state" role="status">
      <span className="loading-pulse" aria-hidden="true" />
      {label}
    </div>
  );
}

export function EmptyState({
  title,
  description,
  action,
}: {
  title: string;
  description: string;
  action?: ReactNode;
}) {
  return (
    <div className="empty-state">
      <span className="empty-state-icon" aria-hidden="true">✓</span>
      <h3>{title}</h3>
      <p>{description}</p>
      {action}
    </div>
  );
}

export function ErrorState({
  title = "Something went wrong",
  message = "We could not load this information. Please try again.",
  onRetry,
}: {
  title?: string;
  message?: string;
  onRetry?: () => void;
}) {
  return (
    <div className="error-state" role="alert">
      <h3>{title}</h3>
      <p>{message}</p>
      {onRetry ? (
        <button className="button button-secondary button-md" type="button" onClick={onRetry}>
          Try again
        </button>
      ) : null}
    </div>
  );
}
