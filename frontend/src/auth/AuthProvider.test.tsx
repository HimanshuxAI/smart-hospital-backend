import { act, renderHook, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { createTokenStore } from "../api/token-store";
import type { TokenBundleResponse } from "../api/types";
import { AuthProvider } from "./AuthProvider";
import { useAuth } from "./auth-context";

const bundle: TokenBundleResponse = {
  accessToken: "access-token",
  refreshToken: "refresh-token",
  tokenType: "Bearer",
  expiresIn: 900,
  user: { id: 1, email: "patient@example.com", role: "PATIENT" },
};

describe("AuthProvider", () => {
  beforeEach(() => {
    sessionStorage.clear();
    localStorage.clear();
  });

  it("signs in and stores the session without persisting the access token", async () => {
    const adapter = {
      login: vi.fn().mockResolvedValue(bundle),
      register: vi.fn(),
      refresh: vi.fn(),
      logout: vi.fn(),
    };
    const tokens = createTokenStore(sessionStorage);
    const { result } = renderHook(() => useAuth(), {
      wrapper: ({ children }) => (
        <AuthProvider adapter={adapter} tokenStore={tokens}>
          {children}
        </AuthProvider>
      ),
    });

    await waitFor(() => expect(result.current.status).toBe("anonymous"));
    await act(async () => {
      await result.current.signIn({
        email: "patient@example.com",
        password: "Patient@123",
      });
    });

    expect(adapter.login).toHaveBeenCalledOnce();
    expect(result.current.user).toEqual(bundle.user);
    expect(result.current.status).toBe("authenticated");
    expect(tokens.getAccessToken()).toBe("access-token");
    expect(sessionStorage.getItem("smarthospital.refresh-token")).toBe(
      "refresh-token",
    );
    expect(localStorage.getItem("smarthospital.refresh-token")).toBeNull();
  });

  it("restores an existing session from the refresh token", async () => {
    const adapter = {
      login: vi.fn(),
      register: vi.fn(),
      refresh: vi.fn().mockResolvedValue(bundle),
      logout: vi.fn(),
    };
    const tokens = createTokenStore(sessionStorage);
    tokens.setRefreshToken("stored-refresh");
    const { result } = renderHook(() => useAuth(), {
      wrapper: ({ children }) => (
        <AuthProvider adapter={adapter} tokenStore={tokens}>
          {children}
        </AuthProvider>
      ),
    });

    await waitFor(() => expect(result.current.status).toBe("authenticated"));
    expect(adapter.refresh).toHaveBeenCalledWith("stored-refresh");
    expect(result.current.user).toEqual(bundle.user);
  });

  it("clears an invalid refresh token when restoration fails", async () => {
    const adapter = {
      login: vi.fn(),
      register: vi.fn(),
      refresh: vi.fn().mockRejectedValue(new Error("invalid refresh token")),
      logout: vi.fn(),
    };
    const tokens = createTokenStore(sessionStorage);
    tokens.setRefreshToken("invalid-refresh");
    const { result } = renderHook(() => useAuth(), {
      wrapper: ({ children }) => (
        <AuthProvider adapter={adapter} tokenStore={tokens}>
          {children}
        </AuthProvider>
      ),
    });

    await waitFor(() => expect(result.current.status).toBe("anonymous"));
    expect(tokens.getRefreshToken()).toBeNull();
    expect(result.current.user).toBeNull();
  });

  it("clears in-memory state on sign out", async () => {
    const adapter = {
      login: vi.fn().mockResolvedValue(bundle),
      register: vi.fn(),
      refresh: vi.fn(),
      logout: vi.fn().mockResolvedValue(undefined),
    };
    const tokens = createTokenStore(sessionStorage);
    const { result } = renderHook(() => useAuth(), {
      wrapper: ({ children }) => (
        <AuthProvider adapter={adapter} tokenStore={tokens}>
          {children}
        </AuthProvider>
      ),
    });

    await waitFor(() => expect(result.current.status).toBe("anonymous"));
    await act(async () => {
      await result.current.signIn({
        email: "patient@example.com",
        password: "Patient@123",
      });
    });
    await act(async () => {
      await result.current.signOut();
    });

    expect(adapter.logout).toHaveBeenCalledWith("refresh-token");
    expect(result.current.user).toBeNull();
    expect(result.current.status).toBe("anonymous");
    expect(tokens.getAccessToken()).toBeNull();
    expect(tokens.getRefreshToken()).toBeNull();
  });
});
