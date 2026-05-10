export const routePaths = {
  home: "/",
  login: "/login",
  register: "/register",
  dashboard: "/dashboard",
  marketplace: "/marketplace",
  offers: "/offers",
  messages: "/messages",
  profile: "/profile",
  admin: {
    dashboard: "/admin",
    users: "/admin/users",
    userDetail: (uuid: string) => `/admin/users/${uuid}`,
    roles: "/admin/roles",
    permissions: "/admin/permissions",
    system: "/admin/system",
  },
} as const;
