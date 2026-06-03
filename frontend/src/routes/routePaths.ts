type QueryParamValue = string | null | undefined;

export function getSafeRedirectPath(redirect: QueryParamValue): string | undefined {
  if (!redirect) {
    return undefined;
  }

  const normalizedRedirect = redirect.trim();

  if (!normalizedRedirect.startsWith("/") || normalizedRedirect.startsWith("//")) {
    return undefined;
  }

  return normalizedRedirect;
}

export function buildPathWithQuery(
  path: string,
  params: Record<string, QueryParamValue>
): string {
  const searchParams = new URLSearchParams();

  Object.entries(params).forEach(([key, value]) => {
    if (value) {
      searchParams.set(key, value);
    }
  });

  const query = searchParams.toString();
  return query ? `${path}?${query}` : path;
}

export const routePaths = {
  home: "/",
  marketplace: "/marketplace",
  marketplaceCategories: "/marketplace/categories",
  login: "/login",
  forgotPassword: "/forgot-password",
  resetPassword: "/reset-password",
  register: "/register",
  verifyEmail: "/verify-email",
  dashboard: "/dashboard",
  betaFeedback: "/beta-feedback",
  marketplaceItem: (uuid: string) => `/marketplace/items/${uuid}`,
  favorites: "/favorites",
  savedSearches: "/saved-searches",
  myItems: "/my-items",
  myItemsNew: "/my-items/new",
  myItemDetail: (uuid: string) => `/my-items/${uuid}`,
  myItemsEdit: (uuid: string) => `/my-items/${uuid}/edit`,
  offers: "/offers",
  offersIncoming: "/offers/incoming",
  offersSent: "/offers/sent",
  offerDetail: (uuid: string) => `/offers/${uuid}`,
  reviews: "/reviews",
  notifications: "/notifications",
  profile: "/profile",
  publicProfile: (uuid: string) => `/users/${uuid}`,
  admin: {
    dashboard: "/admin",
    users: "/admin/users",
    userDetail: (uuid: string) => `/admin/users/${uuid}`,
    roles: "/admin/roles",
    permissions: "/admin/permissions",
    system: "/admin/system",
    operations: "/admin/operations",
    categories: "/admin/categories",
    betaFeedback: "/admin/feedback/beta",
    tags: "/admin/tags",
    listings: "/admin/listings",
    listingDetail: (uuid: string) => `/admin/listings/${uuid}`,
    reports: "/admin/reports",
    reviews: "/admin/reviews",
  },
} as const;
