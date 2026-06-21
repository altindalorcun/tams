import { axiosInstance } from "./axiosInstance";
import type { CreateUserRequest, UpdateUserRequest, UserResponse } from "@/types";

const BASE = "/api/v1/auth/admin/users";

/**
 * Fetch all non-admin platform users.
 */
export async function listUsers(): Promise<UserResponse[]> {
  const response = await axiosInstance.get<UserResponse[]>(BASE);
  return response.data;
}

/**
 * Create a new Teacher or Student account.
 * The account is created with a default password; mustChangePassword is set to true.
 */
export async function createUser(data: CreateUserRequest): Promise<UserResponse> {
  const response = await axiosInstance.post<UserResponse>(BASE, data);
  return response.data;
}

/**
 * Update an existing user's username, email, or active status.
 */
export async function updateUser(id: string, data: UpdateUserRequest): Promise<UserResponse> {
  const response = await axiosInstance.put<UserResponse>(`${BASE}/${id}`, data);
  return response.data;
}

/**
 * Delete a user and invalidate all their active sessions.
 */
export async function deleteUser(id: string): Promise<void> {
  await axiosInstance.delete(`${BASE}/${id}`);
}

/**
 * Reset a user's password to the system default and force a password change on next login.
 */
export async function resetPassword(id: string): Promise<void> {
  await axiosInstance.post(`${BASE}/${id}/reset-password`);
}
