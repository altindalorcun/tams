import type { UserRole } from "./auth";

/** GET /api/v1/admin/users response item */
export interface UserResponse {
  id: string;
  username: string;
  email: string;
  role: UserRole;
  studentNumber: string | null;
  isActive: boolean;
  mustChangePassword: boolean;
  createdAt: string;
}

/** POST /api/v1/admin/users request */
export interface CreateUserRequest {
  username: string;
  email: string;
  role: UserRole;
  studentNumber?: string;
}

/** PUT /api/v1/admin/users/{id} request */
export interface UpdateUserRequest {
  username: string;
  email: string;
  isActive: boolean;
}
