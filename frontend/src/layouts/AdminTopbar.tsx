import { useLocation, useNavigate } from "react-router-dom";
import { LogOut, Menu } from "lucide-react";
import { useTranslation } from "react-i18next";
import { useAuth } from "../auth/AuthContext";
import { Button } from "../components/ui/Button";
import { routePaths } from "../routes/routePaths";
import { LanguageSwitcher } from "@/features/preferences/LanguageSwitcher";
import {TFunction} from "i18next";

interface AdminTopbarProps {
  onMenuClick: () => void;
}

function getAdminSectionMeta(pathname: string, t: TFunction<["navigation"]>) {
  if (pathname.startsWith(routePaths.admin.users)) {
    return pathname === routePaths.admin.users
      ? {
          title: t("navigation:users"),
          description: t("navigation:adminUsersDescription"),
        }
      : {
          title: t("navigation:userDetails"),
          description: t("navigation:userDetailsDescription"),
        };
  }

  if (pathname.startsWith(routePaths.admin.roles)) {
    return {
      title: t("navigation:roles"),
      description: t("navigation:rolesDescription"),
    };
  }

  if (pathname.startsWith(routePaths.admin.permissions)) {
    return {
      title: t("navigation:permissions"),
      description: t("navigation:permissionsDescription"),
    };
  }

  if (pathname.startsWith(routePaths.admin.system)) {
    return {
      title: t("navigation:system"),
      description: t("navigation:systemDescription"),
    };
  }

  if (pathname.startsWith(routePaths.admin.categories)) {
    return {
      title: t("navigation:categories"),
      description: t("navigation:categoriesDescription"),
    };
  }

  if (pathname.startsWith(routePaths.admin.betaFeedback)) {
    return {
      title: t("navigation:betaFeedback"),
      description: t("navigation:betaFeedbackDescription"),
    };
  }

  if (pathname.startsWith(routePaths.admin.tags)) {
    return {
      title: t("navigation:tags"),
      description: t("navigation:tagsDescription"),
    };
  }

  if (pathname.startsWith(routePaths.admin.listings)) {
    return {
      title: t("navigation:listings"),
      description: t("navigation:listingsDescription"),
    };
  }

  if (pathname.startsWith(routePaths.admin.reports)) {
    return {
      title: t("navigation:reports"),
      description: t("navigation:reportsDescription"),
    };
  }

  if (pathname.startsWith(routePaths.admin.reviews)) {
    return {
      title: t("navigation:reviews"),
      description: t("navigation:reviewsDescription"),
    };
  }

  return {
    title: t("navigation:adminControlPanel"),
    description: t("navigation:adminControlPanelDescription"),
  };
}

export function AdminTopbar({ onMenuClick }: AdminTopbarProps) {
  const { user, logout, hasRole } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const { t } = useTranslation(["common", "navigation"]);
  const section = getAdminSectionMeta(location.pathname, t);
  const identityLabel = user?.username ?? user?.email ?? "Admin";
  const fallbackRoleLabel = hasRole("ADMIN") ? t("navigation:administrator") : t("navigation:moderator");
  const identitySubtitle = user?.email && user.email !== identityLabel ? user.email : fallbackRoleLabel;

  const handleLogout = async () => {
    await logout();
    navigate(routePaths.login);
  };

  return (
    <header className="sticky top-0 z-30 border-b border-slate-200 bg-white/95 backdrop-blur dark:border-slate-800 dark:bg-slate-950/90">
      <div className="flex items-center justify-between gap-4 px-4 py-3 lg:px-6">
        <div className="flex min-w-0 items-center gap-3">
          <button
            type="button"
            onClick={onMenuClick}
            className="rounded-lg p-2 text-slate-600 transition-colors hover:bg-slate-100 hover:text-slate-900 dark:text-slate-300 dark:hover:bg-slate-800 dark:hover:text-white lg:hidden"
            aria-label={t("navigation:toggleAdminMenu")}
          >
            <Menu className="size-6" />
          </button>

          <div className="min-w-0">
            <div className="flex flex-wrap items-center gap-2">
              <h1 className="truncate text-lg font-semibold text-slate-950 dark:text-white">
                {section.title}
              </h1>
              <span className="inline-flex items-center rounded-full border border-slate-200 bg-slate-100 px-2 py-0.5 text-[11px] font-semibold uppercase tracking-[0.18em] text-slate-600 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-300">
                {t("common:adminBadge")}
              </span>
            </div>
            <p className="hidden text-sm text-slate-600 dark:text-slate-400 sm:block">
              {section.description}
            </p>
          </div>
        </div>

        <div className="flex shrink-0 items-center gap-2 sm:gap-3">
          <LanguageSwitcher />

          <div className="hidden rounded-2xl border border-slate-200 bg-slate-50 px-3 py-2 text-right dark:border-slate-800 dark:bg-slate-900 md:block">
            <p className="text-sm font-medium text-slate-900 dark:text-slate-100">{identityLabel}</p>
            <p className="text-xs text-slate-600 dark:text-slate-400">{identitySubtitle}</p>
          </div>


          <div className="rounded-xl border border-slate-200 bg-white p-2 dark:border-slate-800 dark:bg-slate-900 md:hidden">
            <p className="max-w-32 truncate text-sm font-medium text-slate-900 dark:text-slate-100">
              {identityLabel}
            </p>
          </div>

          <Button variant="ghost" size="sm" onClick={handleLogout} aria-label={t("common:logout")}>
            <LogOut className="size-4" />
            <span className="hidden sm:inline">{t("common:logout")}</span>
          </Button>
        </div>
      </div>
    </header>
  );
}


