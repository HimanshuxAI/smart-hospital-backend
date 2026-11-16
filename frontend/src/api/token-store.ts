export const REFRESH_TOKEN_KEY = "smarthospital.refresh-token";

export interface TokenStore {
  getAccessToken(): string | null;
  setAccessToken(token: string): void;
  getRefreshToken(): string | null;
  setRefreshToken(token: string): void;
  clear(): void;
}

export function createTokenStore(storage: Storage = sessionStorage): TokenStore {
  let accessToken: string | null = null;

  return {
    getAccessToken: () => accessToken,
    setAccessToken: (token: string) => {
      accessToken = token;
    },
    getRefreshToken: () => storage.getItem(REFRESH_TOKEN_KEY),
    setRefreshToken: (token: string) => {
      storage.setItem(REFRESH_TOKEN_KEY, token);
    },
    clear: () => {
      accessToken = null;
      storage.removeItem(REFRESH_TOKEN_KEY);
    },
  };
}
