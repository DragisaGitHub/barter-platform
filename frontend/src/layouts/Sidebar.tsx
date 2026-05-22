import { NavLink } from "react-router-dom";
import { useTranslation } from "react-i18next";
import {
  LayoutDashboard,
  Store,
  Heart,
  Inbox,
  Send,
  List,
  Shield,
  User,
  Star,
  X,
} from "lucide-react";
import { usePendingIncomingCount, usePendingSentCount } from "../features/trade/useTradeOffers";
import { routePaths } from "@/routes/routePaths";
import { cn } from "@/utils";
import { useAuth } from "@/auth/AuthContext";

interface SidebarProps {
  isOpen: boolean;
  onClose?: () => void;
}

interface NavItem {
  to: string;
  icon: typeof LayoutDashboard;
  label: string;
  badge?: number;
}

function NavBadge({ count }: { count: number }) {
  if (count <= 0) return null;
  return (
    <span className="ml-auto flex items-center justify-center min-w-[20px] h-5 px-1.5 rounded-full bg-indigo-500 text-white text-xs font-semibold leading-none">
      {count > 99 ? "99+" : count}
    </span>
  );
}

export function Sidebar({ isOpen, onClose }: SidebarProps) {
  const { t } = useTranslation(["common", "navigation"]);
  const { hasRole } = useAuth();
  const { data: incomingData } = usePendingIncomingCount();
  const { data: sentData } = usePendingSentCount();
  const pendingIncoming = incomingData?.totalElements ?? 0;
  const pendingSent = sentData?.totalElements ?? 0;
  const isStaff = hasRole("ADMIN") || hasRole("MODERATOR");

  const navItems: NavItem[] = [
    { to: "/dashboard", icon: LayoutDashboard, label: t("navigation:dashboard") },
    { to: "/marketplace", icon: Store, label: t("navigation:marketplaceLabel") },
    { to: "/favorites", icon: Heart, label: t("navigation:favorites") },
    { to: "/my-items", icon: List, label: t("navigation:myItems") },
    { to: "/offers/incoming", icon: Inbox, label: t("navigation:incomingOffers"), badge: pendingIncoming },
    { to: "/offers/sent", icon: Send, label: t("navigation:sentOffers"), badge: pendingSent },
    { to: routePaths.reviews, icon: Star, label: t("navigation:reviews") },
    ...(isStaff
      ? [{ to: routePaths.admin.reports, icon: Shield, label: t("navigation:moderationQueue") }]
      : []),
    { to: "/profile", icon: User, label: t("navigation:profile") },
  ];


  return (
    <>
      {isOpen && onClose && (
        <div
          className="fixed inset-0 bg-black/50 backdrop-blur-sm z-40 lg:hidden"
          onClick={onClose}
          aria-hidden="true"
        />
      )}

      <aside
        className={cn(
          "fixed top-0 left-0 z-50 h-screen w-64 bg-slate-900 border-r border-slate-800 transition-transform duration-200 lg:translate-x-0",
          isOpen ? "translate-x-0" : "-translate-x-full"
        )}
      >
        <div className="flex flex-col h-full">
          <div className="flex items-center justify-between border-b border-slate-800 p-5">
            <h1 className="text-lg font-bold text-white">Barter Platform</h1>
            {onClose && (
              <button
                onClick={onClose}
                className="lg:hidden text-slate-400 hover:text-white transition-colors"
                aria-label={t("navigation:closeSidebar")}
              >
                <X className="size-5" />
              </button>
            )}
          </div>

          <nav className="flex-1 space-y-1 overflow-y-auto p-3">
            {navItems.map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                onClick={onClose}
                className={({ isActive }) =>
                  cn(
                    "flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition-colors",
                    isActive
                      ? "bg-indigo-600 text-white"
                      : "text-slate-300 hover:bg-slate-800 hover:text-white"
                  )
                }
              >
                <item.icon className="size-5" />
                {item.label}
                {item.badge != null && <NavBadge count={item.badge} />}
              </NavLink>
            ))}
          </nav>
        </div>
      </aside>
    </>
  );
}
