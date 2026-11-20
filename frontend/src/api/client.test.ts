import { describe, expect, it, vi } from "vitest";

import { ApiRequestError, createApiClient } from "./client";
import { createTokenStore } from "./token-store";
import type { TokenBundleResponse } from "./types";

const bundle: TokenBundleResponse = {
  accessToken: "new-access",
  refreshToken: "new-refresh",
  tokenType: "Bearer",
  expiresIn: 900,
  user: { id: 1, email: "patient@example.com", role: "PATIENT" },
};

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/problem+json" },
  });
}

describe("createApiClient", () => {
  it("refreshes once and retries a 401 request with the new bearer token", async () => {
    const tokens = createTokenStore(sessionStorage);
    tokens.setAccessToken("old-access");
    tokens.setRefreshToken("old-refresh");
    const fetchMock = vi
      .fn<typeof fetch>()
      .mockResolvedValueOnce(jsonResponse({ code: "UNAUTHORIZED" }, 401))
      .mockResolvedValueOnce(jsonResponse({ ok: true }));
    const refresh = vi.fn().mockResolvedValue(bundle);
    const client = createApiClient({
      baseUrl: "http://api.test",
      fetch: fetchMock,
      tokens,
      refresh,
    });

    const result = await client.request<{ ok: boolean }>("/api/v1/protected");

    expect(result).toEqual({ ok: true });
    expect(refresh).toHaveBeenCalledTimes(1);
    expect(fetchMock).toHaveBeenCalledTimes(2);
    expect((fetchMock.mock.calls[1]?.[0] as Request).headers.get("Authorization"))
      .toBe("Bearer new-access");
  });

  it("shares one refresh across concurrent 401 responses", async () => {
    const tokens = createTokenStore(sessionStorage);
    tokens.setAccessToken("old-access");
    tokens.setRefreshToken("old-refresh");
    const fetchMock = vi
      .fn<typeof fetch>()
      .mockResolvedValueOnce(jsonResponse({}, 401))
      .mockResolvedValueOnce(jsonResponse({}, 401))
      .mockResolvedValueOnce(jsonResponse({ ok: 1 }))
      .mockResolvedValueOnce(jsonResponse({ ok: 2 }));
    let releaseRefresh: (() => void) | undefined;
    const refresh = vi.fn().mockImplementation(
      () =>
        new Promise<TokenBundleResponse>((resolve) => {
          releaseRefresh = () => resolve(bundle);
        }),
    );
    const client = createApiClient({
      baseUrl: "http://api.test",
      fetch: fetchMock,
      tokens,
      refresh,
    });

    const first = client.request<{ ok: number }>("/api/v1/first");
    const second = client.request<{ ok: number }>("/api/v1/second");
    await Promise.resolve();
    releaseRefresh?.();

    await expect(first).resolves.toEqual({ ok: 1 });
    await expect(second).resolves.toEqual({ ok: 2 });
    expect(refresh).toHaveBeenCalledTimes(1);
  });

  it("does not refresh a second time when the retried request is unauthorized", async () => {
    const tokens = createTokenStore(sessionStorage);
    tokens.setAccessToken("old-access");
    const fetchMock = vi
      .fn<typeof fetch>()
      .mockResolvedValueOnce(jsonResponse({}, 401))
      .mockResolvedValueOnce(jsonResponse({}, 401));
    const refresh = vi.fn().mockResolvedValue(bundle);
    const client = createApiClient({
      baseUrl: "http://api.test",
      fetch: fetchMock,
      tokens,
      refresh,
    });

    await expect(client.request("/api/v1/protected")).rejects.toBeInstanceOf(
      ApiRequestError,
    );
    expect(refresh).toHaveBeenCalledTimes(1);
    expect(fetchMock).toHaveBeenCalledTimes(2);
  });

  it("clears tokens and reports expiration once when refresh fails", async () => {
    const tokens = createTokenStore(sessionStorage);
    tokens.setAccessToken("old-access");
    tokens.setRefreshToken("old-refresh");
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse({}, 401));
    const refresh = vi.fn().mockRejectedValue(new Error("refresh failed"));
    const onSessionExpired = vi.fn();
    const client = createApiClient({
      baseUrl: "http://api.test",
      fetch: fetchMock,
      tokens,
      refresh,
      onSessionExpired,
    });

    await expect(client.request("/api/v1/protected")).rejects.toBeInstanceOf(
      ApiRequestError,
    );

    expect(refresh).toHaveBeenCalledTimes(1);
    expect(tokens.getAccessToken()).toBeNull();
    expect(tokens.getRefreshToken()).toBeNull();
    expect(onSessionExpired).toHaveBeenCalledTimes(1);
  });
});
