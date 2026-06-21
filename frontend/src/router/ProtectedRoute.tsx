import { Navigate, Outlet } from "react-router-dom";
import { useAuthStore } from "@/features/auth/authStore";
import type { UserRole } from "@/types";

interface ProtectedRouteProps {
  /** If provided, only users with this role may access the route */
  requiredRole?: UserRole;
  /**
   * When true, users who must change their password are allowed through.
   * Used exclusively for the /change-password route itself.
   */
  allowMustChangePassword?: boolean;
}

/**
 * Redirects unauthenticated users to /login.
 * Redirects users with a pending mandatory password change to /change-password,
 * unless allowMustChangePassword is explicitly set to true.
 * Redirects authenticated users that lack the required role to /forbidden.
 */
export function ProtectedRoute({ requiredRole, allowMustChangePassword = false }: ProtectedRouteProps) {
  const { accessToken, role, mustChangePassword } = useAuthStore();

  if (!accessToken) {
    return <Navigate to="/login" replace />;
  }

  if (mustChangePassword && !allowMustChangePassword) {
    return <Navigate to="/change-password" replace />;
  }

  if (requiredRole && role !== requiredRole) {
    return <Navigate to="/forbidden" replace />;
  }

  return <Outlet />;
}
