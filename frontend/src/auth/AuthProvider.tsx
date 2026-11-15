import {
  useCallback,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from "react";

import type { AuthAdapter } from "../api/adapter";
import { createTokenStore, type TokenStore } from "../api/token-store";
import type {
  AuthUser,
  LoginInput,
  RegisterInput,
  TokenBundleResponse,
} from "../api/types";
import { AuthContext, type AuthContextValue, type AuthStatus } from "./auth-context";

type AuthProviderProps = {
  adapter: AuthAdapter;
  tokenStore?: TokenStore;
  children: ReactNode;
};

export function AuthProvider({
  adapter,
  tokenStore = createTokenStore(),
  children,
}: AuthProviderProps) {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [status, setStatus] = useState<AuthStatus>("restoring");

  const applyBundle = useCallback(
    (bundle: TokenBundleResponse) => {
      tokenStore.setAccessToken(bundle.accessToken);
      tokenStore.setRefreshToken(bundle.refreshToken);
      setUser(bundle.user);
      setStatus("authenticated");
      return bundle.user;
    },
    [tokenStore],
  );

  useEffect(() => {
    let active = true;
    const refreshToken = tokenStore.getRefreshToken();

    if (!refreshToken) {
      setStatus("anonymous");
      return () => {
        active = false;
      };
    }

    void adapter
      .refresh(refreshToken)
      .then((bundle) => {
        if (active) {
          applyBundle(bundle);
        }
      })
      .catch(() => {
        tokenStore.clear();
        if (active) {
          setUser(null);
          setStatus("anonymous");
        }
      });

    return () => {
      active = false;
    };
  }, [adapter, applyBundle, tokenStore]);

  const signIn = useCallback(
    async (credentials: LoginInput) => applyBundle(await adapter.login(credentials)),
    [adapter, applyBundle],
  );

  const register = useCallback(
    async (input: RegisterInput) => applyBundle(await adapter.register(input)),
    [adapter, applyBundle],
  );

  const signOut = useCallback(async () => {
    const refreshToken = tokenStore.getRefreshToken();
    try {
      if (refreshToken) {
        await adapter.logout(refreshToken);
      }
    } finally {
      tokenStore.clear();
      setUser(null);
      setStatus("anonymous");
    }
  }, [adapter, tokenStore]);

  const value = useMemo<AuthContextValue>(
    () => ({ user, status, signIn, register, signOut }),
    [register, signIn, signOut, status, user],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
