export type ApiProblem = {
  status: number;
  code: string;
  title: string;
  detail: string;
  path?: string;
  timestamp?: string;
  fieldErrors: Record<string, string>;
  retryAfterSeconds?: number;
};

const STATUS_TITLES: Record<number, string> = {
  400: "Bad Request",
  401: "Unauthorized",
  403: "Forbidden",
  404: "Not Found",
  409: "Conflict",
  422: "Unprocessable Entity",
  429: "Too Many Requests",
  500: "Internal Server Error",
  502: "Bad Gateway",
  503: "Service Unavailable",
  504: "Gateway Timeout",
};

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

function stringValue(value: unknown): string | undefined {
  return typeof value === "string" && value.trim() ? value : undefined;
}

function fieldErrorsFrom(value: unknown): Record<string, string> {
  if (!isRecord(value)) {
    return {};
  }

  return Object.fromEntries(
    Object.entries(value).filter(
      (entry): entry is [string, string] => typeof entry[1] === "string",
    ),
  );
}

export function parseProblem(
  status: number,
  body: unknown,
  headers?: Headers,
): ApiProblem {
  const source = isRecord(body) ? body : {};
  const title =
    stringValue(source.title) ??
    STATUS_TITLES[status] ??
    "Request failed";
  const detail =
    stringValue(source.detail) ?? stringValue(source.message) ?? title;
  const retryAfter = headers?.get("Retry-After");
  const retryAfterSeconds = retryAfter
    ? Number.parseInt(retryAfter, 10)
    : Number.NaN;

  const problem: ApiProblem = {
    status,
    code: stringValue(source.code) ?? `HTTP_${status}`,
    title,
    detail,
    fieldErrors: {
      ...fieldErrorsFrom(source.fieldErrors),
      ...fieldErrorsFrom(source.errors),
    },
  };

  const path = stringValue(source.path);
  if (path) {
    problem.path = path;
  }

  const timestamp = stringValue(source.timestamp);
  if (timestamp) {
    problem.timestamp = timestamp;
  }

  if (Number.isInteger(retryAfterSeconds) && retryAfterSeconds >= 0) {
    problem.retryAfterSeconds = retryAfterSeconds;
  }

  return problem;
}
