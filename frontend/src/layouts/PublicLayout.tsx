import { Outlet, useLocation } from "react-router-dom";

export function PublicLayout() {
  const location = useLocation();
  const isMarketplaceRoute =
    location.pathname === "/marketplace" ||
    location.pathname.startsWith("/marketplace/");

  if (isMarketplaceRoute) {
    return <Outlet />;
  }

  return (
    <div className="min-h-screen bg-slate-50 dark:bg-slate-900">
      <Outlet />
    </div>
  );
}
