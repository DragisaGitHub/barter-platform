import { Navigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import { LoadingScreen } from "../components/ui/Spinner";

interface AdminRouteProps {
  children: React.ReactNode;
}

export function AdminRoute({ children }: AdminRouteProps) {
  const { isAuthenticated, isLoading, hasRole } = useAuth();

  if (isLoading) {
    return <LoadingScreen />;
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  if (!hasRole("ADMIN")) {
    return <Navigate to="/dashboard" replace />;
  }

  return <>{children}</>;
}
