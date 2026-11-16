import { parseProblem, type ApiProblem } from "./problem";
import type { TokenStore } from "./token-store";
import type { TokenBundleResponse } from "./types";

export interface ApiClient {
  request<T>(path: string, init?: RequestInit): Promise<T>;
}

export class ApiRequestError extends Error {
  readonly problem: ApiProblem;

  constructor(problem: ApiProblem) {
    super(problem.detail);
    this.name = "ApiRequestError";
    this.problem = problem;
  }
}

export type ApiClientOptions = {
  baseUrl: string;
  fetch?: typeof fetch;
  tokens: TokenStore;
  refresh: () => Promise<TokenBundleResponse>;
  onSessionExpired?: () => void;
};

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

async function responseBody(response: Response): Promise<unknown> {
  const contentType = response.headers.get("Content-Type") ?? "";
  if (!contentType.includes("json")) {
    return undefined;
  }

  try {
    return await response.json();
  } catch {
    return undefined;
  }
}

function withBearerToken(request: Request, token: string | null): Request {
  if (!token) {
    return request;
  }

  const headers = new Headers(request.headers);
  headers.set("Authorization", `Bearer ${token}`);
  return new Request(request, { headers });
}

export function createApiClient(options: ApiClientOptions): ApiClient {
  const requestFetch = options.fetch ?? fetch;
  const baseUrl = options.baseUrl.replace(/\/$/, "");
  let refreshPromise: Promise<TokenBundleResponse> | null = null;
  let sessionExpiredNotified = false;

  const refreshOnce = (): Promise<TokenBundleResponse> => {
    if (!refreshPromise) {
      refreshPromise = options.refresh().catch((error: unknown) => {
        refreshPromise = null;
        options.tokens.clear();
        if (!sessionExpiredNotified) {
          sessionExpiredNotified = true;
          options.onSessionExpired?.();
        }
        throw error;
      });
    }
    return refreshPromise;
  };

  const execute = async <T>(
    path: string,
    init: RequestInit | undefined,
    alreadyRetried: boolean,
  ): Promise<T> => {
    const url = `${baseUrl}${path.startsWith("/") ? path : `/${path}`}`;
    const original = new Request(url, init);
    const authenticated = withBearerToken(
      original,
      options.tokens.getAccessToken(),
    );

    let response = await requestFetch(authenticated.clone());

    if (response.status === 401 && !alreadyRetried) {
      try {
        const bundle = await refreshOnce();
        options.tokens.setAccessToken(bundle.accessToken);
        options.tokens.setRefreshToken(bundle.refreshToken);
        sessionExpiredNotified = false;

        const retry = withBearerToken(
          original,
          options.tokens.getAccessToken(),
        );
        response = await requestFetch(retry.clone());
      } catch {
        const problem = parseProblem(401, {
          code: "UNAUTHORIZED",
          title: "Unauthorized",
          detail: "Your session has expired",
        });
        throw new ApiRequestError(problem);
      }
    }

    if (!response.ok) {
      const body = await responseBody(response);
      throw new ApiRequestError(
        parseProblem(response.status, body, response.headers),
      );
    }

    if (response.status === 204) {
      return undefined as T;
    }

    const body = await responseBody(response);
    return body as T;
  };

  return {
    request: <T>(path: string, init?: RequestInit) =>
      execute<T>(path, init, false),
  };
}

export function isProblemPayload(value: unknown): value is Record<string, unknown> {
  return isRecord(value);
}
