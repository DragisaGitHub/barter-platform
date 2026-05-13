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
  login: "/login",
  forgotPassword: "/forgot-password",
  resetPassword: "/reset-password",
  register: "/register",
  verifyEmail: "/verify-email",
  dashboard: "/dashboard",
  marketplaceItem: (uuid: string) => `/marketplace/items/${uuid}`,
  favorites: "/favorites",
  myItems: "/my-items",
  myItemsNew: "/my-items/new",
  myItemsEdit: (uuid: string) => `/my-items/${uuid}/edit`,
  offers: "/offers",
  notifications: "/notifications",
  messages: "/messages",
  profile: "/profile",
  publicProfile: (uuid: string) => `/users/${uuid}`,
  admin: {
    dashboard: "/admin",
    users: "/admin/users",
    userDetail: (uuid: string) => `/admin/users/${uuid}`,
    roles: "/admin/roles",
    permissions: "/admin/permissions",
    system: "/admin/system",
  },
} as const;
