import axios, { AxiosError } from "axios";
import { z } from "zod";

/**
 * Versioned API client. All server communication goes through here so
 * request-id propagation and the standard error envelope stay consistent.
 * Base URL resolution: NEXT_PUBLIC_API_BASE_URL (canonical per .env.example)
 * falls back to legacy NEXT_PUBLIC_API_URL, then localhost default.
 * The canonical value is `http://localhost:8080/api` — `/v1` is appended
 * when missing so both `.../api` and `.../api/v1` env values work.
 */

function resolveBaseUrl(): string {
  const raw =
    process.env.NEXT_PUBLIC_API_BASE_URL ??
    process.env.NEXT_PUBLIC_API_URL ??
    "http://localhost:8080/api/v1";
  const trimmed = raw.replace(/\/+$/, "");
  return trimmed.endsWith("/v1") ? trimmed : `${trimmed}/v1`;
}

const API_BASE_URL = resolveBaseUrl();

export const ApiErrorSchema = z.object({
  timestamp: z.string(),
  status: z.number(),
  code: z.string(),
  message: z.string(),
  path: z.string().nullable().optional(),
  requestId: z.string().nullable().optional(),
});

export type ApiError = z.infer<typeof ApiErrorSchema>;

export function toApiError(error: unknown): ApiError {
  const axiosError = error as AxiosError<unknown>;
  const parsed = ApiErrorSchema.safeParse(axiosError.response?.data);
  if (parsed.success) return parsed.data;
  if (axiosError.response) {
    return {
      timestamp: new Date().toISOString(),
      status: axiosError.response.status,
      code: "REQUEST_FAILED",
      message: axiosError.message,
    };
  }
  return {
    timestamp: new Date().toISOString(),
    status: 0,
    code: "NETWORK_ERROR",
    message: "Unable to reach the Fixna API",
  };
}

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  withCredentials: false,
  timeout: 15000,
  headers: { "Content-Type": "application/json" },
});

let accessToken: string | null = null;

export function setAccessToken(token: string | null): void {
  accessToken = token;
}

export function getAccessToken(): string | null {
  return accessToken;
}

apiClient.interceptors.request.use((config) => {
  if (accessToken) config.headers.set("Authorization", `Bearer ${accessToken}`);
  if (typeof crypto !== "undefined" && "randomUUID" in crypto) {
    config.headers.set("X-Request-Id", crypto.randomUUID());
  }
  return config;
});

