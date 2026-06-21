/** User roles matching backend Role enum */
export type UserRole = "ADMIN" | "TEACHER" | "STUDENT";

/** JWT token payload fields we care about */
export interface TokenPayload {
  sub: string;
  role: UserRole;
  exp: number;
}

/** Auth state stored in the Zustand store */
export interface AuthState {
  accessToken: string | null;
  role: UserRole | null;
  userId: string | null;
  mustChangePassword: boolean;
}

/** POST /api/v1/auth/login request */
export interface LoginRequest {
  email: string;
  password: string;
}

/** POST /api/v1/auth/login response */
export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  mustChangePassword: boolean;
}

/** POST /api/v1/auth/change-password request */
export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}
