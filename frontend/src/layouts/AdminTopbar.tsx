import { useLocation, useNavigate } from "react-router-dom";
import { LogOut, Menu, Store } from "lucide-react";
import { useAuth } from "../auth/AuthContext";
import { Button } from "../components/ui/Button";
import { routePaths } from "../routes/routePaths";

interface AdminTopbarProps {
  onMenuClick: () => void;
}

function getAdminSectionMeta(pathname: string) {
  if (pathname.startsWith(routePaths.admin.users)) {
    return pathname === routePaths.admin.users
      ? {
          title: "Users",
          description: "Review access, user status, and account details.",
        }
      : {
          title: "User Details",
          description: "Inspect account context and administrative access details.",
        };
  }

  if (pathname.startsWith(routePaths.admin.roles)) {
    return {
      title: "Roles",
      description: "Audit and review role definitions used across the platform.",
    };
  }

  if (pathname.startsWith(routePaths.admin.permissions)) {
    return {
      title: "Permissions",
      description: "Inspect granted capabilities and authorization coverage.",
    };
  }

  if (pathname.startsWith(routePaths.admin.system)) {
    return {
      title: "System",
      description: "Check operational health and platform-level configuration.",
    };
  }

  if (pathname.startsWith(routePaths.admin.categories)) {
    return {
      title: "Categories",
      description: "Prepare category management without changing current contracts yet.",
    };
  }

  return {
    title: "Admin Control Panel",
    description: "Operate platform access, configuration, and core administration from one focused workspace.",
  };
}

export function AdminTopbar({ onMenuClick }: AdminTopbarProps) {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const section = getAdminSectionMeta(location.pathname);
  const identityLabel = user?.username ?? user?.email ?? "Admin";
  const identitySubtitle = user?.email && user.email !== identityLabel ? user.email : "Administrator";

  const handleLogout = async () => {
    await logout();
    navigate(routePaths.login);
  };

  const handleMarketplaceClick = () => {
    navigate(routePaths.marketplace);
  };

  return (
    <header className="sticky top-0 z-30 border-b border-slate-200 bg-white/95 backdrop-blur dark:border-slate-800 dark:bg-slate-950/90">
      <div className="flex items-center justify-between gap-4 px-4 py-3 lg:px-6">
        <div className="flex min-w-0 items-center gap-3">
          <button
            type="button"
            onClick={onMenuClick}
            className="rounded-lg p-2 text-slate-600 transition-colors hover:bg-slate-100 hover:text-slate-900 dark:text-slate-300 dark:hover:bg-slate-800 dark:hover:text-white lg:hidden"
            aria-label="Toggle admin menu"
          >
            <Menu className="size-6" />
          </button>

          <div className="min-w-0">
            <div className="flex flex-wrap items-center gap-2">
              <h1 className="truncate text-lg font-semibold text-slate-950 dark:text-white">
                {section.title}
              </h1>
              <span className="inline-flex items-center rounded-full border border-slate-200 bg-slate-100 px-2 py-0.5 text-[11px] font-semibold uppercase tracking-[0.18em] text-slate-600 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-300">
                Admin
              </span>
            </div>
            <p className="hidden text-sm text-slate-600 dark:text-slate-400 sm:block">
              {section.description}
            </p>
          </div>
        </div>

        <div className="flex shrink-0 items-center gap-2 sm:gap-3">
          <div className="hidden rounded-2xl border border-slate-200 bg-slate-50 px-3 py-2 text-right dark:border-slate-800 dark:bg-slate-900 md:block">
            <p className="text-sm font-medium text-slate-900 dark:text-slate-100">{identityLabel}</p>
            <p className="text-xs text-slate-600 dark:text-slate-400">{identitySubtitle}</p>
          </div>

          <Button
            variant="outline"
            size="sm"
            type="button"
            onClick={handleMarketplaceClick}
            className="hidden md:inline-flex"
          >
            <Store className="size-4" />
            Open marketplace
          </Button>

          <div className="rounded-xl border border-slate-200 bg-white p-2 dark:border-slate-800 dark:bg-slate-900 md:hidden">
            <p className="max-w-[8rem] truncate text-sm font-medium text-slate-900 dark:text-slate-100">
              {identityLabel}
            </p>
          </div>

          <Button variant="ghost" size="sm" onClick={handleLogout} aria-label="Logout">
            <LogOut className="size-4" />
            <span className="hidden sm:inline">Logout</span>
          </Button>
        </div>
      </div>
    </header>
  );
}


