import { NavLink } from "react-router-dom";
import { useTranslation } from "react-i18next";
import {
  LayoutDashboard,
  Store,
  Heart,
  Search,
  Inbox,
  Send,
  List,
  Shield,
  User,
  Star,
  X,
  MessageCircle,
} from "lucide-react";
import { usePendingIncomingCount, usePendingSentCount } from "../features/trade/useTradeOffers";
import { useUnreadTradeMessageCount } from "../features/trade/useTradeOfferMessages";
import { routePaths } from "@/routes/routePaths";
import { cn } from "@/utils";
import { useAuth } from "@/auth/AuthContext";

interface SidebarProps {
  isOpen: boolean;
  onClose?: () => void;
}

interface NavItem {
  id: string;
  to: string;
  icon: typeof LayoutDashboard;
  label: string;
  badge?: number;
}

function NavBadge({ count }: { count: number }) {
  if (count <= 0) return null;
  return (
    <span className="ml-auto flex items-center justify-center min-w-5 h-5 px-1.5 rounded-full bg-indigo-500 text-white text-xs font-semibold leading-none">
      {count > 99 ? "99+" : count}
    </span>
  );
}

export function Sidebar({ isOpen, onClose }: SidebarProps) {
  const { t } = useTranslation(["common", "navigation"]);
  const { hasRole } = useAuth();
  const { data: incomingData } = usePendingIncomingCount();
  const { data: sentData } = usePendingSentCount();
  const { data: unreadMessagesData } = useUnreadTradeMessageCount();
  const pendingIncoming = incomingData?.totalElements ?? 0;
  const pendingSent = sentData?.totalElements ?? 0;
  const unreadMessages = unreadMessagesData?.count ?? 0;
  const isStaff = hasRole("ADMIN") || hasRole("MODERATOR");

  const navItems: NavItem[] = [
    { id: "dashboard", to: "/dashboard", icon: LayoutDashboard, label: t("navigation:dashboard") },
    { id: "marketplace", to: "/marketplace", icon: Store, label: t("navigation:marketplaceLabel") },
    { id: "favorites", to: "/favorites", icon: Heart, label: t("navigation:favorites") },
    { id: "saved-searches", to: routePaths.savedSearches, icon: Search, label: t("navigation:savedSearches") },
    { id: "my-items", to: "/my-items", icon: List, label: t("navigation:myItems") },
    { id: "offers-incoming", to: "/offers/incoming", icon: Inbox, label: t("navigation:incomingOffers"), badge: pendingIncoming },
    { id: "offers-sent", to: "/offers/sent", icon: Send, label: t("navigation:sentOffers"), badge: pendingSent },
    { id: "trade-messages", to: "/offers/incoming", icon: MessageCircle, label: t("navigation:tradeMessages"), badge: unreadMessages },
    { id: "reviews", to: routePaths.reviews, icon: Star, label: t("navigation:reviews") },
    ...(isStaff
      ? [{ id: "admin-reports", to: routePaths.admin.reports, icon: Shield, label: t("navigation:moderationQueue") }]
      : []),
    { id: "profile", to: "/profile", icon: User, label: t("navigation:profile") },
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
                key={item.id}
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
