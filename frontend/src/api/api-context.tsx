import { createContext, useContext, type ReactNode } from "react";

import type { AppAdapter } from "./adapter";

const ApiContext = createContext<AppAdapter | null>(null);

export function ApiProvider({ adapter, children }: { adapter: AppAdapter; children: ReactNode }) {
  return <ApiContext.Provider value={adapter}>{children}</ApiContext.Provider>;
}

export function useApi(): AppAdapter {
  const adapter = useContext(ApiContext);
  if (!adapter) {
    throw new Error("useApi must be used inside ApiProvider");
  }
  return adapter;
}
