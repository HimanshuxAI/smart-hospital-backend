import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { useMemo, useState, type ReactNode } from "react";

import { ApiProvider } from "../api/api-context";
import type { AppAdapter } from "../api/adapter";
import { createHttpAdapter } from "../api/http-adapter";
import { createTokenStore } from "../api/token-store";
import { AuthProvider } from "../auth/AuthProvider";
import { createSessionEvents, type SessionEvents } from "../auth/session-events";
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
      }),
  );

  const [tokens] = useState(() => createTokenStore());
  const [sessionEvents] = useState<SessionEvents>(() => createSessionEvents());

  const [adapter] = useState<AppAdapter>(() => {
    const isDemo = import.meta.env.VITE_API_MODE === "demo";
    if (isDemo) {
      return createDemoAdapter();
    }

    const baseUrl = import.meta.env.VITE_API_BASE_URL ?? "/api/v1";
    return createHttpAdapter({
      baseUrl,
      tokens,
      onSessionExpired: () => sessionEvents.notifyExpired(),
    });
  });

  const value = useMemo(() => ({ adapter, tokens, sessionEvents }), [adapter, sessionEvents, tokens]);

  return (
    <AppErrorBoundary>
      <QueryClientProvider client={queryClient}>
        <ApiProvider adapter={adapter}>
          <AuthProvider
            adapter={adapter}
            tokenStore={tokens}
            sessionEvents={value.sessionEvents}
          >
            {children}
          </AuthProvider>
        </ApiProvider>
      </QueryClientProvider>
    </AppErrorBoundary>
  );
}
