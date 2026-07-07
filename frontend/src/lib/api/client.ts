import type { ApiErrorResponse } from "./types";

const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

const unsafeMethods = new Set(["POST", "PUT", "PATCH", "DELETE"]);

interface CsrfCredentials {
  token: string;
  headerName: string;
}

let csrfCredentials: CsrfCredentials | null = null;
let csrfRequest: Promise<CsrfCredentials> | null = null;
let unauthorizedHandler: (() => void) | null = null;

export class ApiClientError extends Error {
  constructor(
    message: string,
    public readonly status: number,
    public readonly fieldErrors: ApiErrorResponse["fieldErrors"] = [],
  ) {
    super(message);
    this.name = "ApiClientError";
  }
}

export function clearCsrfToken() {
  csrfCredentials = null;
  csrfRequest = null;
}

export function setUnauthorizedHandler(handler: (() => void) | null) {
  unauthorizedHandler = handler;

  return () => {
    if (unauthorizedHandler === handler) unauthorizedHandler = null;
  };
}

export async function refreshCsrfToken() {
  clearCsrfToken();
  return getCsrfToken();
}

async function getCsrfToken(): Promise<CsrfCredentials> {
  if (csrfCredentials) return csrfCredentials;
  if (csrfRequest) return csrfRequest;

  csrfRequest = fetch(`${API_BASE_URL}/api/auth/csrf`, {
    credentials: "include",
  })
    .then(async (response) => {
      if (!response.ok) throw await toApiError(response);
      return (await response.json()) as CsrfCredentials;
    })
    .then((credentials) => {
      csrfCredentials = credentials;
      return credentials;
    })
    .finally(() => {
      csrfRequest = null;
    });

  return csrfRequest;
}

export async function apiRequest<T>(
  path: string,
  options: RequestInit = {},
): Promise<T> {
  const method = (options.method ?? "GET").toUpperCase();
  const headers = new Headers(options.headers);

  if (unsafeMethods.has(method)) {
    const csrf = await getCsrfToken();
    headers.set(csrf.headerName, csrf.token);
  }

  if (options.body && !(options.body instanceof FormData) && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }

  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    method,
    headers,
    credentials: "include",
  });

  if (response.status === 401) unauthorizedHandler?.();

  if (!response.ok) throw await toApiError(response);
  if (response.status === 204) return undefined as T;
  return (await response.json()) as T;
}

async function toApiError(response: Response): Promise<ApiClientError> {
  try {
    const error = (await response.json()) as ApiErrorResponse;
    return new ApiClientError(
      error.message || `Request failed with status ${response.status}.`,
      response.status,
      error.fieldErrors ?? [],
    );
  } catch {
    return new ApiClientError(
      `Request failed with status ${response.status}.`,
      response.status,
    );
  }
}
