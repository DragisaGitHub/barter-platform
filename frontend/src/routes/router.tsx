import { createBrowserRouter, Navigate } from "react-router-dom";
import { PublicLayout } from "../layouts/PublicLayout";
import { AppLayout } from "../layouts/AppLayout";
import { ProtectedRoute } from "./ProtectedRoute";
import { AdminRoute } from "./AdminRoute";
import { LandingPage } from "../features/landing/LandingPage";
import { LoginPage } from "../features/auth/LoginPage";
import { RegisterPage } from "../features/auth/RegisterPage";
import { DashboardPage } from "../features/dashboard/DashboardPage";
import { PlaceholderPage } from "../features/placeholder/PlaceholderPage";
import { AdminDashboardPage } from "../features/admin/AdminDashboardPage";
import { UsersListPage } from "../features/admin/UsersListPage";
import { UserDetailPage } from "../features/admin/UserDetailPage";
import { RolesPage } from "../features/admin/RolesPage";
import { PermissionsPage } from "../features/admin/PermissionsPage";
import { SystemPage } from "../features/admin/SystemPage";

export const router = createBrowserRouter([
  {
    element: <PublicLayout />,
    children: [
      {
        path: "/",
        element: <LandingPage />,
      },
      {
        path: "/login",
        element: <LoginPage />,
      },
      {
        path: "/register",
        element: <RegisterPage />,
      },
    ],
  },
  {
    element: (
      <ProtectedRoute>
        <AppLayout />
      </ProtectedRoute>
    ),
    children: [
      {
        path: "/dashboard",
        element: <DashboardPage />,
      },
      {
        path: "/marketplace",
        element: (
          <PlaceholderPage
            title="Marketplace"
            description="Browse available items and services for trade."
          />
        ),
      },
      {
        path: "/offers",
        element: (
          <PlaceholderPage
            title="My Offers"
            description="Manage your active listings and trade offers."
          />
        ),
      },
      {
        path: "/messages",
        element: (
          <PlaceholderPage
            title="Messages"
            description="Chat with other traders about potential exchanges."
          />
        ),
      },
      {
        path: "/profile",
        element: (
          <PlaceholderPage
            title="Profile"
            description="View and edit your profile information."
          />
        ),
      },
    ],
  },
  {
    element: (
      <AdminRoute>
        <AppLayout />
      </AdminRoute>
    ),
    children: [
      {
        path: "/admin",
        element: <AdminDashboardPage />,
      },
      {
        path: "/admin/users",
        element: <UsersListPage />,
      },
      {
        path: "/admin/users/:uuid",
        element: <UserDetailPage />,
      },
      {
        path: "/admin/roles",
        element: <RolesPage />,
      },
      {
        path: "/admin/permissions",
        element: <PermissionsPage />,
      },
      {
        path: "/admin/system",
        element: <SystemPage />,
      },
    ],
  },
  {
    path: "*",
    element: <Navigate to="/" replace />,
  },
]);
