import { NavLink, useNavigate } from "react-router-dom";
import {
  FolderTree,
  KeyRound,
  LayoutDashboard,
  Lock,
  Settings,
  Shield,
  Store,
  Users,
  X,
} from "lucide-react";
import { Button } from "../components/ui/Button";
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

const adminNavItems: AdminNavItem[] = [
  { to: routePaths.admin.dashboard, label: "Admin Dashboard", icon: LayoutDashboard, end: true },
  { to: routePaths.admin.users, label: "Users", icon: Users },
  { to: routePaths.admin.roles, label: "Roles", icon: KeyRound },
  { to: routePaths.admin.permissions, label: "Permissions", icon: Lock },
  { to: routePaths.admin.system, label: "System", icon: Settings },
  { to: routePaths.admin.categories, label: "Categories", icon: FolderTree, end: true },
];

export function AdminSidebar({ isOpen, onClose }: AdminSidebarProps) {
  const navigate = useNavigate();

  const handleMarketplaceClick = () => {
    onClose?.();
    navigate(routePaths.marketplace);
  };

  return (
    <>
      {isOpen && onClose && (
        <button
          type="button"
          className="fixed inset-0 z-40 bg-slate-950/60 backdrop-blur-sm lg:hidden"
          onClick={onClose}
          aria-label="Close admin navigation"
        />
      )}

      <aside
        className={cn(
          "fixed inset-y-0 left-0 z-50 flex h-screen w-72 flex-col border-r border-slate-800 bg-slate-950 text-slate-100 transition-transform duration-200 lg:translate-x-0",
          isOpen ? "translate-x-0" : "-translate-x-full"
        )}
        aria-label="Admin navigation"
      >
        <div className="flex items-center justify-between border-b border-slate-800 px-5 py-4">
          <div className="min-w-0">
            <div className="flex items-center gap-3">
              <div className="flex size-10 items-center justify-center rounded-xl bg-indigo-500/15 text-indigo-300 ring-1 ring-inset ring-indigo-400/20">
                <Shield className="size-5" />
              </div>
              <div className="min-w-0">
                <p className="truncate text-sm font-semibold text-white">Barter Platform</p>
                <p className="truncate text-xs text-slate-400">Admin control panel</p>
              </div>
            </div>
          </div>

          {onClose && (
            <button
              type="button"
              onClick={onClose}
              className="rounded-lg p-2 text-slate-400 transition-colors hover:bg-slate-900 hover:text-white lg:hidden"
              aria-label="Close sidebar"
            >
              <X className="size-5" />
            </button>
          )}
        </div>

        <div className="flex flex-1 flex-col overflow-hidden">
          <div className="border-b border-slate-800/80 px-5 py-4">
            <p className="text-xs font-semibold uppercase tracking-[0.24em] text-slate-500">
              Platform management
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

        <div className="border-t border-slate-800 px-4 py-4">
          <div className="rounded-xl border border-slate-800 bg-slate-900/80 p-3">
            <p className="text-xs font-semibold uppercase tracking-[0.2em] text-slate-500">
              Secondary action
            </p>
            <p className="mt-2 text-sm text-slate-300">
              Open the user-facing marketplace without leaving the admin control panel setup.
            </p>
            <Button
              variant="outline"
              size="sm"
              onClick={handleMarketplaceClick}
              className="mt-3 w-full justify-center border-slate-700 bg-slate-950 text-slate-100 hover:bg-slate-800 hover:text-white"
            >
              <Store className="size-4" />
              Open marketplace
            </Button>
          </div>
        </div>
      </aside>
    </>
  );
}


