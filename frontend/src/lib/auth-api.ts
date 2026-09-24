import { z } from "zod";
import { apiClient, setAccessToken } from "./api-client";

/**
 * Auth API — mirrors POST /api/v1/auth/* (AuthController).
 * Tokens are stored in-memory via setAccessToken; refresh token is kept
 * alongside for rotation. No tenant id is ever sent — scope comes from JWT.
 */

export const AuthResponseSchema = z.object({
  accessToken: z.string(),
  refreshToken: z.string(),
  tokenType: z.string(),
  expiresInSeconds: z.number(),
  userId: z.string(),
  tenantId: z.string(),
  role: z.string(),
});

export type AuthResponse = z.infer<typeof AuthResponseSchema>;

export const RegisterSchema = z.object({
  email: z.string().email(),
  password: z.string().min(8).max(100),
  firstName: z.string().max(100).optional(),
  lastName: z.string().max(100).optional(),
  tenantName: z.string().min(1).max(200),
});

export type RegisterInput = z.infer<typeof RegisterSchema>;

export const LoginSchema = z.object({
  email: z.string().email(),
  password: z.string().min(1),
});

export type LoginInput = z.infer<typeof LoginSchema>;

let refreshToken: string | null = null;

export function getRefreshToken(): string | null {
  return refreshToken;
}

async function storeAuth(data: unknown): Promise<AuthResponse> {
  const parsed = AuthResponseSchema.parse(data);
  setAccessToken(parsed.accessToken);
  refreshToken = parsed.refreshToken;
  if (typeof window !== "undefined") {
    window.localStorage.setItem("fixna.refreshToken", parsed.refreshToken);
  }
  return parsed;
}

export async function register(input: RegisterInput): Promise<AuthResponse> {
  const { data } = await apiClient.post("/auth/register", input);
  return storeAuth(data);
}

export async function login(input: LoginInput): Promise<AuthResponse> {
  const { data } = await apiClient.post("/auth/login", input);
  return storeAuth(data);
}

export async function refreshSession(): Promise<AuthResponse> {
  const stored =
    refreshToken ??
    (typeof window !== "undefined"
      ? window.localStorage.getItem("fixna.refreshToken")
      : null);
  if (!stored) throw new Error("No refresh token available");
  const { data } = await apiClient.post("/auth/refresh", {
    refreshToken: stored,
  });
  return storeAuth(data);
}

export async function logout(): Promise<void> {
  try {
    await apiClient.post("/auth/logout");
  } finally {
    setAccessToken(null);
    refreshToken = null;
    if (typeof window !== "undefined") {
      window.localStorage.removeItem("fixna.refreshToken");
    }
  }
}
