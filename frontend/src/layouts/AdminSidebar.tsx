import { NavLink } from "react-router-dom";
import { useTranslation } from "react-i18next";
import {
  FolderTree,
  KeyRound,
  LayoutDashboard,
  Lock,
  Package,
  Settings,
  Shield,
  MessageSquareWarning,
  Tags,
  Users,
  X,
} from "lucide-react";
import { cn } from "@/utils";
import { routePaths } from "../routes/routePaths";

interface AdminSidebarProps {
  isOpen: boolean;
  onClose?: () => void;
}

interface AdminNavItem {
  to: string;
  label: string;
  icon: typeof LayoutDashboard;
  end?: boolean;
}

export function AdminSidebar({ isOpen, onClose }: AdminSidebarProps) {
  const { t } = useTranslation(["navigation"]);

  const adminNavItems: AdminNavItem[] = [
    { to: routePaths.admin.dashboard, label: t("navigation:adminControlPanel"), icon: LayoutDashboard, end: true },
    { to: routePaths.admin.users, label: t("navigation:users"), icon: Users },
    { to: routePaths.admin.roles, label: t("navigation:roles"), icon: KeyRound },
    { to: routePaths.admin.permissions, label: t("navigation:permissions"), icon: Lock },
    { to: routePaths.admin.system, label: t("navigation:system"), icon: Settings },
    { to: routePaths.admin.categories, label: t("navigation:categories"), icon: FolderTree, end: true },
    { to: routePaths.admin.listings, label: t("navigation:listings"), icon: Package, end: true },
    { to: routePaths.admin.reviews, label: t("navigation:reviews"), icon: MessageSquareWarning, end: true },
    { to: routePaths.admin.tags, label: t("navigation:tags"), icon: Tags, end: true },
  ];

  return (
    <>
      {isOpen && onClose && (
        <button
          type="button"
          className="fixed inset-0 z-40 bg-slate-950/60 backdrop-blur-sm lg:hidden"
          onClick={onClose}
          aria-label={t("navigation:closeAdminNavigation")}
        />
      )}

      <aside
        className={cn(
          "fixed inset-y-0 left-0 z-50 flex h-screen w-72 flex-col border-r border-slate-800 bg-slate-950 text-slate-100 transition-transform duration-200 lg:translate-x-0",
          isOpen ? "translate-x-0" : "-translate-x-full"
        )}
        aria-label={t("navigation:adminControlShort")}
      >
        <div className="flex items-center justify-between border-b border-slate-800 px-5 py-4">
          <div className="min-w-0">
            <div className="flex items-center gap-3">
              <div className="flex size-10 items-center justify-center rounded-xl bg-indigo-500/15 text-indigo-300 ring-1 ring-inset ring-indigo-400/20">
                <Shield className="size-5" />
              </div>
              <div className="min-w-0">
                <p className="truncate text-sm font-semibold text-white">Barter Platform</p>
                <p className="truncate text-xs text-slate-400">{t("navigation:adminControlPanel")}</p>
              </div>
            </div>
          </div>

          {onClose && (
            <button
              type="button"
              onClick={onClose}
              className="rounded-lg p-2 text-slate-400 transition-colors hover:bg-slate-900 hover:text-white lg:hidden"
              aria-label={t("navigation:closeSidebar")}
            >
              <X className="size-5" />
            </button>
          )}
        </div>

        <div className="flex flex-1 flex-col overflow-hidden">
          <div className="border-b border-slate-800/80 px-5 py-4">
            <p className="text-xs font-semibold uppercase tracking-[0.24em] text-slate-500">
              {t("navigation:platformManagement")}
            </p>
          </div>

          <nav className="flex-1 overflow-y-auto px-3 py-4">
            <ul className="space-y-1.5">
              {adminNavItems.map((item) => (
                <li key={item.to}>
                  <NavLink
                    to={item.to}
                    end={item.end}
                    onClick={onClose}
                    className={({ isActive }) =>
                      cn(
                        "group flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium transition-colors focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:ring-offset-2 focus:ring-offset-slate-950",
                        isActive
                          ? "bg-indigo-600 text-white shadow-sm"
                          : "text-slate-300 hover:bg-slate-900 hover:text-white"
                      )
                    }
                  >
                    <item.icon className="size-4 shrink-0" />
                    <span className="truncate">{item.label}</span>
                  </NavLink>
                </li>
              ))}
            </ul>
          </nav>
        </div>

      </aside>
    </>
  );
}


