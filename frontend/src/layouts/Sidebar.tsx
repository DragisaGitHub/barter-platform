import { NavLink } from "react-router-dom";
import {
  LayoutDashboard,
  Store,
  Inbox,
  Send,
  List,
  MessageSquare,
  User,
  Shield,
  Users,
  KeyRound,
  Lock,
  Settings,
  X,
} from "lucide-react";
import { useAuth } from "../auth/AuthContext";
import { cn } from "@/utils";

interface SidebarProps {
  isOpen: boolean;
  onClose?: () => void;
}

interface NavItem {
  to: string;
  icon: typeof LayoutDashboard;
  label: string;
  adminOnly?: boolean;
}

export function Sidebar({ isOpen, onClose }: SidebarProps) {
  const { hasRole } = useAuth();
  const isAdmin = hasRole("ADMIN");

  const navItems: NavItem[] = [
    { to: "/dashboard", icon: LayoutDashboard, label: "Dashboard" },
    { to: "/marketplace", icon: Store, label: "Marketplace" },
    { to: "/my-items", icon: List, label: "My Items" },
    { to: "/offers/incoming", icon: Inbox, label: "Incoming Offers" },
    { to: "/offers/sent", icon: Send, label: "Sent Offers" },
    { to: "/messages", icon: MessageSquare, label: "Messages" },
    { to: "/profile", icon: User, label: "Profile" },
  ];

  const adminItems: NavItem[] = [
    { to: "/admin", icon: Shield, label: "Admin Dashboard" },
    { to: "/admin/users", icon: Users, label: "Users" },
    { to: "/admin/roles", icon: KeyRound, label: "Roles" },
    { to: "/admin/permissions", icon: Lock, label: "Permissions" },
    { to: "/admin/system", icon: Settings, label: "System" },
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
          <div className="flex items-center justify-between p-6 border-b border-slate-800">
            <h1 className="text-xl font-bold text-white">Barter Platform</h1>
            {onClose && (
              <button
                onClick={onClose}
                className="lg:hidden text-slate-400 hover:text-white transition-colors"
                aria-label="Close sidebar"
              >
                <X className="size-5" />
              </button>
            )}
          </div>

          <nav className="flex-1 overflow-y-auto p-4 space-y-1">
            {navItems.map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                onClick={onClose}
                className={({ isActive }) =>
                  cn(
                    "flex items-center gap-3 px-4 py-3 rounded-lg text-sm font-medium transition-colors",
                    isActive
                      ? "bg-indigo-600 text-white"
                      : "text-slate-300 hover:bg-slate-800 hover:text-white"
                  )
                }
              >
                <item.icon className="size-5" />
                {item.label}
              </NavLink>
            ))}

            {isAdmin && (
              <>
                <div className="pt-4 pb-2">
                  <h2 className="px-4 text-xs font-semibold text-slate-500 uppercase tracking-wider">
                    Administration
                  </h2>
                </div>
                {adminItems.map((item) => (
                  <NavLink
                    key={item.to}
                    to={item.to}
                    onClick={onClose}
                    className={({ isActive }) =>
                      cn(
                        "flex items-center gap-3 px-4 py-3 rounded-lg text-sm font-medium transition-colors",
                        isActive
                          ? "bg-indigo-600 text-white"
                          : "text-slate-300 hover:bg-slate-800 hover:text-white"
                      )
                    }
                  >
                    <item.icon className="size-5" />
                    {item.label}
                  </NavLink>
                ))}
              </>
            )}
          </nav>
        </div>
      </aside>
    </>
  );
}
