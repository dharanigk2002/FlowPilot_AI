import { apiRequest, clearCsrfToken, refreshCsrfToken } from "./client";
import type { AuthResponse, CurrentUser } from "./types";

export interface LoginInput {
  email: string;
  password: string;
}

export interface RegisterInput extends LoginInput {
  displayName: string;
}

export async function login(input: LoginInput) {
  const response = await apiRequest<AuthResponse>("/api/auth/login", {
    method: "POST",
    body: JSON.stringify(input),
  });
  await refreshCsrfToken();
  return response;
}

export async function register(input: RegisterInput) {
  const response = await apiRequest<AuthResponse>("/api/auth/register", {
    method: "POST",
    body: JSON.stringify(input),
  });
  await refreshCsrfToken();
  return response;
}

export function getCurrentUser() {
  return apiRequest<CurrentUser>("/api/auth/me");
}

export async function logout() {
  await apiRequest<void>("/api/auth/logout", { method: "POST" });
  clearCsrfToken();
}
