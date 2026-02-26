import { Navigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import type { UserRole } from "../types/api";

export function ProtectedRoute({
  children,
  allowRoles,
}: {
  children: React.ReactNode;
  allowRoles?: UserRole[];
}) {
  const { isAuthenticated, hasAnyRole } = useAuth();
  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }
  if (allowRoles && !hasAnyRole(allowRoles)) {
    return <Navigate to="/forbidden" replace />;
  }
  return <>{children}</>;
}
