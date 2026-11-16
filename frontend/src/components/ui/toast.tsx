import {
  createContext,
  useCallback,
  useContext,
  useMemo,
  useState,
  type ReactNode,
} from "react";

export type ToastTone = "info" | "success" | "warning" | "danger";
export type ToastMessage = {
  id: string;
  title: string;
  message?: string;
  tone: ToastTone;
};

type ToastContextValue = {
  toasts: ToastMessage[];
  push(toast: Omit<ToastMessage, "id">): void;
};

const ToastContext = createContext<ToastContextValue | null>(null);

export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<ToastMessage[]>([]);
  const push = useCallback((toast: Omit<ToastMessage, "id">) => {
    setToasts((current) => [
      ...current,
      { ...toast, id: crypto.randomUUID() },
    ]);
  }, []);
  const value = useMemo(() => ({ push, toasts }), [push, toasts]);

  return (
    <ToastContext.Provider value={value}>
      {children}
    </ToastContext.Provider>
  );
}

export function ToastViewport({ toasts: providedToasts }: { toasts?: ToastMessage[] }) {
  const context = useContext(ToastContext);
  const toasts = providedToasts ?? context?.toasts ?? [];

  return (
    <div className="toast-viewport" role="status" aria-live="polite" aria-atomic="false">
      {toasts.map((toast) => (
        <div key={toast.id} className={`toast toast-${toast.tone}`}>
          <strong>{toast.title}</strong>
          {toast.message ? <span>{toast.message}</span> : null}
        </div>
      ))}
    </div>
  );
}

export function useToast(): ToastContextValue {
  const context = useContext(ToastContext);
  if (!context) {
    throw new Error("useToast must be used inside ToastProvider");
  }
  return context;
}
