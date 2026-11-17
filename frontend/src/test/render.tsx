import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, type RenderOptions, type RenderResult } from "@testing-library/react";
import { useEffect, type ReactElement, type ReactNode } from "react";
import { MemoryRouter } from "react-router-dom";

import { ApiProvider } from "../api/api-context";
import type { AppAdapter } from "../api/adapter";
import { AuthProvider } from "../auth/AuthProvider";
import { useAuth } from "../auth/auth-context";

export type RenderAppOptions = {
  adapter: AppAdapter;
  route?: string;
  email?: string;
};

function SignedIn({ email, children }: { email: string; children: ReactNode }) {
  const { signIn, status } = useAuth();

  useEffect(() => {
    void signIn({ email, password: "TestPassword123!" });
  }, [email, signIn]);

  if (status !== "authenticated") {
    return <div role="status">Preparing session</div>;
  }

  return children;
}

export function renderApp(
  ui: ReactElement,
  options: RenderAppOptions,
  renderOptions?: RenderOptions,
): RenderResult {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false, gcTime: 0 } },
  });

  return render(
    <QueryClientProvider client={queryClient}>
      <ApiProvider adapter={options.adapter}>
        <AuthProvider adapter={options.adapter}>
          <MemoryRouter initialEntries={[options.route ?? "/"]}>
            <SignedIn email={options.email ?? "admin@hospital.local"}>{ui}</SignedIn>
          </MemoryRouter>
        </AuthProvider>
      </ApiProvider>
    </QueryClientProvider>,
    renderOptions,
  );
}
