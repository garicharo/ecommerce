import { clearSession, loginPath } from "./auth";
import type { ApiError } from "./types";

type UnauthorizedHandler = () => void;

let unauthorizedHandler: UnauthorizedHandler | null = null;

export function setUnauthorizedHandler(handler: UnauthorizedHandler | null): void {
  unauthorizedHandler = handler;
}

export class HttpError extends Error {
  readonly status: number;
  readonly body: ApiError;

  constructor(status: number, body: ApiError) {
    super(body.message || `Request failed (${status})`);
    this.status = status;
    this.body = body;
  }
}

type RequestOptions = {
  method?: string;
  body?: unknown;
  headers?: Record<string, string>;
  idempotencyKey?: string;
  auth?: boolean;
  redirectOn401?: boolean;
};

async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const headers: Record<string, string> = { ...options.headers };
  if (options.idempotencyKey) {
    headers["Idempotency-Key"] = options.idempotencyKey;
  }
  const init: RequestInit = {
    method: options.method ?? "GET",
    credentials: "include",
    headers,
  };
  if (options.body instanceof FormData) {
    init.body = options.body;
  } else if (options.body !== undefined) {
    headers["Content-Type"] = "application/json";
    init.body = JSON.stringify(options.body);
    init.headers = headers;
  }

  const res = await fetch(path, init);
  if (res.status === 401 && options.redirectOn401 !== false) {
    unauthorizedHandler?.();
    clearSession();
    window.location.assign(loginPath());
    throw new HttpError(401, { code: "UNAUTHORIZED", message: "Not logged in", details: [] });
  }
  if (res.status === 204) {
    return undefined as T;
  }
  const json = (await res.json().catch(() => ({}))) as ApiError & T;
  if (!res.ok) {
    throw new HttpError(res.status, {
      code: json.code ?? "ERROR",
      message: json.message ?? `Request failed (${res.status})`,
      details: json.details ?? [],
    });
  }
  return json as T;
}

export const api = {
  get: <T>(path: string, options?: RequestOptions) => request<T>(path, options),
  post: <T>(path: string, body?: unknown, options?: RequestOptions) =>
    request<T>(path, { ...options, method: "POST", body }),
  put: <T>(path: string, body?: unknown, options?: RequestOptions) =>
    request<T>(path, { ...options, method: "PUT", body }),
  del: <T>(path: string, options?: RequestOptions) => request<T>(path, { ...options, method: "DELETE" }),
};
