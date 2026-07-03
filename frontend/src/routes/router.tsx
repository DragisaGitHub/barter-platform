import { createBrowserRouter, Navigate } from "react-router-dom";
import { useAuth } from "@/auth/AuthContext";
import { LoadingScreen } from "@/components/ui/Spinner";
import { PublicLayout } from "../layouts/PublicLayout";
import { AppLayout } from "../layouts/AppLayout";
import { AdminLayout } from "../layouts/AdminLayout";
import { ProtectedRoute } from "./ProtectedRoute";
import { AdminRoute } from "./AdminRoute";
import { StaffRoute } from "./StaffRoute";
import { LoginPage } from "../features/auth/LoginPage";
import { ForgotPasswordPage } from "../features/auth/ForgotPasswordPage";
import { ResetPasswordPage } from "../features/auth/ResetPasswordPage";
import { RegisterPage } from "../features/auth/RegisterPage";
import { VerifyEmailPage } from "../features/auth/VerifyEmailPage";
import { DashboardPage } from "../features/dashboard/DashboardPage";
import { AdminDashboardPage } from "../features/admin/AdminDashboardPage";
import { AdminCategoriesPage } from "../features/admin/AdminCategoriesPage";
import { AdminCategorySchemasPage } from "../features/admin/AdminCategorySchemasPage";
import { AdminListingsPage } from "../features/admin/AdminListingsPage";
import { AdminListingDetailPage } from "../features/admin/AdminListingDetailPage";
import { AdminReportsPage } from "../features/admin/AdminReportsPage";
import { AdminReviewsPage } from "../features/admin/AdminReviewsPage";
import { AdminTagsPage } from "../features/admin/AdminTagsPage";
import { AdminBetaFeedbackPage } from "../features/admin/AdminBetaFeedbackPage";
import { UsersListPage } from "../features/admin/UsersListPage";
import { UserDetailPage } from "../features/admin/UserDetailPage";
import { RolesPage } from "../features/admin/RolesPage";
import { PermissionsPage } from "../features/admin/PermissionsPage";
import { SystemPage } from "../features/admin/SystemPage";
import { AdminOperationsPage } from "../features/admin/AdminOperationsPage";
import { MarketplacePage } from "../features/catalog/MarketplacePage";
import { MarketplaceCategoriesPage } from "../features/catalog/MarketplaceCategoriesPage";
import { ItemDetailPage } from "../features/catalog/ItemDetailPage";
import { MyItemsPage } from "../features/catalog/MyItemsPage";
import { FavoritesPage } from "../features/catalog/FavoritesPage";
import { SavedSearchesPage } from "../features/catalog/SavedSearchesPage";
import { CreateItemPage } from "../features/catalog/CreateItemPage";
import { EditItemPage } from "../features/catalog/EditItemPage";
import { IncomingOffersPage } from "../features/trade/IncomingOffersPage";
import { SentOffersPage } from "../features/trade/SentOffersPage";
import { TradeOfferDetailPage } from "../features/trade/TradeOfferDetailPage";
import { ReviewsPage } from "../features/reviews/ReviewsPage";
import { NotificationsPage } from "../features/notifications/NotificationsPage";
import { ProfilePage } from "../features/profile/ProfilePage";
import { PublicProfilePage } from "../features/profile/PublicProfilePage";
import { NotFoundPage } from "../features/error/NotFoundPage";
import { RouteErrorPage } from "../features/error/ErrorPage";
import { LandingPage } from "../features/landing/LandingPage";
import { BetaFeedbackPage } from "../features/feedback/BetaFeedbackPage";
import { TermsOfServicePage } from "../features/legal/TermsOfServicePage";
import { PrivacyPolicyPage } from "../features/legal/PrivacyPolicyPage";
import { CommunityGuidelinesPage } from "../features/legal/CommunityGuidelinesPage";
import { ProhibitedItemsPage } from "../features/legal/ProhibitedItemsPage";
import { SafetyTipsPage } from "../features/legal/SafetyTipsPage";
import { routePaths } from "./routePaths";

function AdminAwareHomePage() {
  const { isAuthenticated, isLoading, hasRole } = useAuth();

  if (isLoading) {
    return <LoadingScreen />;
  }

  if (isAuthenticated && hasRole("ADMIN")) {
    return <Navigate to={routePaths.admin.dashboard} replace />;
  }

   if (isAuthenticated && hasRole("MODERATOR")) {
    return <Navigate to={routePaths.admin.reports} replace />;
  }

  if (isAuthenticated) {
    return <Navigate to={routePaths.dashboard} replace />;
  }

  return <LandingPage />;
}

function AdminAwareMarketplacePage() {
  const { isAuthenticated, isLoading, hasRole } = useAuth();

  if (isLoading) {
    return <LoadingScreen />;
  }

  if (isAuthenticated && hasRole("ADMIN")) {
    return <Navigate to={routePaths.admin.dashboard} replace />;
  }

  return <MarketplacePage />;
}

function AdminAwareMarketplaceCategoriesPage() {
  const { isAuthenticated, isLoading, hasRole } = useAuth();

  if (isLoading) {
    return <LoadingScreen />;
  }

  if (isAuthenticated && hasRole("ADMIN")) {
    return <Navigate to={routePaths.admin.dashboard} replace />;
  }

  return <MarketplaceCategoriesPage />;
}

function AdminAwareDashboardPage() {
  const { hasRole } = useAuth();

  if (hasRole("ADMIN")) {
    return <Navigate to={routePaths.admin.dashboard} replace />;
  }

  if (hasRole("MODERATOR")) {
    return <Navigate to={routePaths.admin.reports} replace />;
  }

  return <DashboardPage />;
}

function StaffAwareAdminHome() {
  const { hasRole } = useAuth();

  if (hasRole("ADMIN")) {
    return <AdminDashboardPage />;
  }

  return <Navigate to={routePaths.admin.reports} replace />;
}

export const router = createBrowserRouter([
  {
    element: <PublicLayout />,
    errorElement: <RouteErrorPage />,
    children: [
      {
        path: "/",
        element: <AdminAwareHomePage />,
      },
      {
        path: "/marketplace",
        element: <AdminAwareMarketplacePage />,
      },
      {
        path: routePaths.marketplaceCategories,
        element: <AdminAwareMarketplaceCategoriesPage />,
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
      {
        path: routePaths.legal.terms,
        element: <TermsOfServicePage />,
      },
      {
        path: routePaths.legal.privacy,
        element: <PrivacyPolicyPage />,
      },
      {
        path: routePaths.legal.communityGuidelines,
        element: <CommunityGuidelinesPage />,
      },
      {
        path: routePaths.legal.prohibitedItems,
        element: <ProhibitedItemsPage />,
      },
      {
        path: routePaths.legal.safetyTips,
        element: <SafetyTipsPage />,
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
        element: <AdminAwareDashboardPage />,
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
        path: routePaths.savedSearches,
        element: <SavedSearchesPage />,
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
        path: routePaths.reviews,
        element: <ReviewsPage />,
      },
      {
        path: "/notifications",
        element: <NotificationsPage />,
      },
      {
        path: "/profile",
        element: <ProfilePage />,
      },
      {
        path: routePaths.betaFeedback,
        element: <BetaFeedbackPage />,
      },
    ],
  },
  {
    element: (
      <StaffRoute>
        <AdminLayout />
      </StaffRoute>
    ),
    errorElement: <RouteErrorPage />,
    children: [
      {
        path: "/admin",
        element: <StaffAwareAdminHome />,
      },
      {
        path: "/admin/listings",
        element: <AdminListingsPage />,
      },
      {
        path: "/admin/listings/:uuid",
        element: <AdminListingDetailPage />,
      },
      {
        path: routePaths.admin.reports,
        element: <AdminReportsPage />,
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
        path: routePaths.admin.operations,
        element: <AdminOperationsPage />,
      },
      {
        path: "/admin/categories",
        element: <AdminCategoriesPage />,
      },
      {
        path: routePaths.admin.categorySchemas,
        element: <AdminCategorySchemasPage />,
      },
      {
        path: routePaths.admin.betaFeedback,
        element: <AdminBetaFeedbackPage />,
      },
      {
        path: "/admin/reviews",
        element: <AdminReviewsPage />,
      },
      {
        path: "/admin/tags",
        element: <AdminTagsPage />,
      },
    ],
  },
  {
    path: "*",
    element: <NotFoundPage />,
  },
]);
