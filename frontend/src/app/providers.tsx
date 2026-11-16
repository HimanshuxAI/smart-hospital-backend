import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { useState, type ReactNode } from "react";
import { AuthProvider } from "../auth/AuthProvider";
import { createDemoAdapter } from "../mocks/adapter";
import { AppErrorBoundary } from "./error-boundary";

export function AppProviders({ children }: { children: ReactNode }) {
  const [queryClient] = useState(
    () =>
      new QueryClient({
        defaultOptions: {
          queries: {
            staleTime: 30000,
            retry: 1,
            refetchOnWindowFocus: false,
          },
        },
      })
  );

  const [adapter] = useState(() => createDemoAdapter());

  return (
    <AppErrorBoundary>
      <QueryClientProvider client={queryClient}>
        <AuthProvider adapter={adapter}>
          {children}
        </AuthProvider>
      </QueryClientProvider>
    </AppErrorBoundary>
  );
}
