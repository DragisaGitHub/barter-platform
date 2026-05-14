import { createBrowserRouter, Navigate } from "react-router-dom";
import { PublicLayout } from "../layouts/PublicLayout";
import { AppLayout } from "../layouts/AppLayout";
import { AdminLayout } from "../layouts/AdminLayout";
import { ProtectedRoute } from "./ProtectedRoute";
import { AdminRoute } from "./AdminRoute";
import { LoginPage } from "../features/auth/LoginPage";
import { ForgotPasswordPage } from "../features/auth/ForgotPasswordPage";
import { ResetPasswordPage } from "../features/auth/ResetPasswordPage";
import { RegisterPage } from "../features/auth/RegisterPage";
import { VerifyEmailPage } from "../features/auth/VerifyEmailPage";
import { DashboardPage } from "../features/dashboard/DashboardPage";
import { AdminDashboardPage } from "../features/admin/AdminDashboardPage";
import { AdminCategoriesPage } from "../features/admin/AdminCategoriesPage";
import { UsersListPage } from "../features/admin/UsersListPage";
import { UserDetailPage } from "../features/admin/UserDetailPage";
import { RolesPage } from "../features/admin/RolesPage";
import { PermissionsPage } from "../features/admin/PermissionsPage";
import { SystemPage } from "../features/admin/SystemPage";
import { MarketplacePage } from "../features/catalog/MarketplacePage";
import { ItemDetailPage } from "../features/catalog/ItemDetailPage";
import { MyItemsPage } from "../features/catalog/MyItemsPage";
import { FavoritesPage } from "../features/catalog/FavoritesPage";
import { CreateItemPage } from "../features/catalog/CreateItemPage";
import { EditItemPage } from "../features/catalog/EditItemPage";
import { IncomingOffersPage } from "../features/trade/IncomingOffersPage";
import { SentOffersPage } from "../features/trade/SentOffersPage";
import { TradeOfferDetailPage } from "../features/trade/TradeOfferDetailPage";
import { NotificationsPage } from "../features/notifications/NotificationsPage";
import { ProfilePage } from "../features/profile/ProfilePage";
import { PublicProfilePage } from "../features/profile/PublicProfilePage";
import { NotFoundPage } from "../features/error/NotFoundPage";
import { RouteErrorPage } from "../features/error/ErrorPage";

export const router = createBrowserRouter([
  {
    element: <PublicLayout />,
    errorElement: <RouteErrorPage />,
    children: [
      {
        path: "/",
        element: <Navigate to="/marketplace" replace />,
      },
      {
        path: "/marketplace",
        element: <MarketplacePage />,
      },
      {
        path: "/marketplace/items/:uuid",
        element: <ItemDetailPage />,
      },
      {
        path: "/login",
        element: <LoginPage />,
      },
      {
        path: "/forgot-password",
        element: <ForgotPasswordPage />,
      },
      {
        path: "/reset-password",
        element: <ResetPasswordPage />,
      },
      {
        path: "/register",
        element: <RegisterPage />,
      },
      {
        path: "/verify-email",
        element: <VerifyEmailPage />,
      },
      {
        path: "/users/:uuid",
        element: <PublicProfilePage />,
      },
    ],
  },
  {
    element: (
      <ProtectedRoute>
        <AppLayout />
      </ProtectedRoute>
    ),
    errorElement: <RouteErrorPage />,
    children: [
      {
        path: "/dashboard",
        element: <DashboardPage />,
      },
      {
        path: "/my-items",
        element: <MyItemsPage />,
      },
      {
        path: "/favorites",
        element: <FavoritesPage />,
      },
      {
        path: "/my-items/new",
        element: <CreateItemPage />,
      },
      {
        path: "/my-items/:uuid",
        element: <ItemDetailPage />,
      },
      {
        path: "/my-items/:uuid/edit",
        element: <EditItemPage />,
      },
      {
        path: "/offers/incoming",
        element: <IncomingOffersPage />,
      },
      {
        path: "/offers/sent",
        element: <SentOffersPage />,
      },
      {
        path: "/offers/:uuid",
        element: <TradeOfferDetailPage />,
      },
      {
        path: "/notifications",
        element: <NotificationsPage />,
      },
      {
        path: "/profile",
        element: <ProfilePage />,
      },
    ],
  },
  {
    element: (
      <AdminRoute>
        <AdminLayout />
      </AdminRoute>
    ),
    errorElement: <RouteErrorPage />,
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
      {
        path: "/admin/categories",
        element: <AdminCategoriesPage />,
      },
    ],
  },
  {
    path: "*",
    element: <NotFoundPage />,
  },
]);
