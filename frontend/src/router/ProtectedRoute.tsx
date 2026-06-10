import { Navigate, Outlet } from "react-router-dom";
import { useAuthStore } from "@/features/auth/authStore";
import type { UserRole } from "@/types";

interface ProtectedRouteProps {
  /** If provided, only users with this role may access the route */
  requiredRole?: UserRole;
}

/**
 * Redirects unauthenticated users to /login.
 * Redirects authenticated users that lack the required role to /forbidden.
 */
export function ProtectedRoute({ requiredRole }: ProtectedRouteProps) {
  const { accessToken, role } = useAuthStore();

  if (!accessToken) {
    return <Navigate to="/login" replace />;
  }

  if (requiredRole && role !== requiredRole) {
    return <Navigate to="/forbidden" replace />;
  }

  return <Outlet />;
}
