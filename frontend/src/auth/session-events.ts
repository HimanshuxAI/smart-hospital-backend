export type SessionEvents = {
  subscribe(listener: () => void): () => void;
  notifyExpired(): void;
};

export function createSessionEvents(): SessionEvents {
  const listeners = new Set<() => void>();

  return {
    subscribe(listener: () => void) {
      listeners.add(listener);
      return () => {
        listeners.delete(listener);
      };
    },
    notifyExpired() {
      for (const listener of [...listeners]) {
        listener();
      }
    },
  };
}
