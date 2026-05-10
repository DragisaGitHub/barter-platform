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
import { MarketplacePage } from "../features/catalog/MarketplacePage";
import { ItemDetailPage } from "../features/catalog/ItemDetailPage";
import { MyItemsPage } from "../features/catalog/MyItemsPage";
import { CreateItemPage } from "../features/catalog/CreateItemPage";
import { EditItemPage } from "../features/catalog/EditItemPage";
import { IncomingOffersPage } from "../features/trade/IncomingOffersPage";
import { SentOffersPage } from "../features/trade/SentOffersPage";
import { TradeOfferDetailPage } from "../features/trade/TradeOfferDetailPage";

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
        element: <MarketplacePage />,
      },
      {
        path: "/marketplace/items/:uuid",
        element: <ItemDetailPage />,
      },
      {
        path: "/my-items",
        element: <MyItemsPage />,
      },
      {
        path: "/my-items/new",
        element: <CreateItemPage />,
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
