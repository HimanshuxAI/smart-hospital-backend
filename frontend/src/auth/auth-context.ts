import { createContext, useContext } from "react";

import type { LoginInput, RegisterInput, AuthUser } from "../api/types";

export type AuthStatus = "restoring" | "authenticated" | "anonymous";

export type AuthContextValue = {
  user: AuthUser | null;
  status: AuthStatus;
  signIn(credentials: LoginInput): Promise<AuthUser>;
  register(input: RegisterInput): Promise<AuthUser>;
  signOut(): Promise<void>;
};

export const AuthContext = createContext<AuthContextValue | null>(null);

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used inside AuthProvider");
  }
  return context;
}
