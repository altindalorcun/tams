import { axiosInstance } from "./axiosInstance";
import type { LoginRequest, LoginResponse, RegisterRequest } from "@/types";

/**
 * Authenticate a user and receive JWT tokens.
 */
export async function login(data: LoginRequest): Promise<LoginResponse> {
  const response = await axiosInstance.post<LoginResponse>("/api/v1/auth/login", data);
  return response.data;
}

/**
 * Register a new user account.
 */
export async function register(data: RegisterRequest): Promise<void> {
  await axiosInstance.post("/api/v1/auth/register", data);
}

/**
 * Exchange a refresh token for a new access token.
 */
export async function refreshToken(token: string): Promise<LoginResponse> {
  const response = await axiosInstance.post<LoginResponse>("/api/v1/auth/refresh", {
    refreshToken: token,
  });
  return response.data;
}

/**
 * Invalidate the current refresh token on the server.
 */
export async function logout(token: string): Promise<void> {
  await axiosInstance.post("/api/v1/auth/logout", { refreshToken: token });
}
