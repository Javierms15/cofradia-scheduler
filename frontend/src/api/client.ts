import type { ProblemDetail } from "../types";

const BASE_URL = "http://localhost:8080/api";

export class ApiError extends Error {
  status: number;
  errores?: Record<string, string>;

  constructor(status: number, message: string, errores?: Record<string, string>) {
    super(message);
    this.status = status;
    this.errores = errores;
  }
}

let currentToken: string | null = null;

export function setAuthToken(token: string | null) {
  currentToken = token;
}

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const headers = new Headers(options.headers);
  headers.set("Content-Type", "application/json");
  if (currentToken) {
    headers.set("Authorization", `Bearer ${currentToken}`);
  }

  const response = await fetch(`${BASE_URL}${path}`, { ...options, headers });

  if (response.status === 204) {
    return undefined as T;
  }

  const isJson = response.headers.get("content-type")?.includes("application/json");
  const body = isJson ? await response.json() : undefined;

  if (!response.ok) {
    const problem = body as ProblemDetail | undefined;
    throw new ApiError(
      response.status,
      problem?.detail ?? `Error ${response.status}`,
      problem?.errores
    );
  }

  return body as T;
}

export const api = {
  get: <T>(path: string) => request<T>(path, { method: "GET" }),
  post: <T>(path: string, data?: unknown) =>
    request<T>(path, { method: "POST", body: data !== undefined ? JSON.stringify(data) : undefined }),
};
