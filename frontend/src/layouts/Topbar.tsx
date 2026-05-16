import { Menu, LogOut } from "lucide-react";
import { useTranslation } from "react-i18next";
import { useAuth } from "../auth/AuthContext";
import { Button } from "../components/ui/Button";
import { useNavigate } from "react-router-dom";
import { NotificationBell } from "../features/notifications/NotificationBell";
import { LanguageSwitcher } from "@/features/preferences/LanguageSwitcher";

interface TopbarProps {
  onMenuClick: () => void;
}

export function Topbar({ onMenuClick }: TopbarProps) {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const { t } = useTranslation(["common", "navigation"]);

  const handleLogout = async () => {
    await logout();
    navigate("/login");
  };

  return (
    <header className="sticky top-0 z-30 bg-white dark:bg-slate-800 border-b border-slate-200 dark:border-slate-700">
      <div className="flex items-center justify-between px-4 py-3">
        <button
          onClick={onMenuClick}
          className="lg:hidden p-2 text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-700 rounded-lg transition-colors"
          aria-label={t("navigation:toggleMenu")}
        >
          <Menu className="size-6" />
        </button>

        <div className="hidden lg:block" />

        <div className="flex items-center gap-4">
          <LanguageSwitcher />

          <NotificationBell />

          <div className="text-right hidden sm:block">
            <p className="text-sm font-medium text-slate-900 dark:text-slate-100">
              {user?.username}
            </p>
            <p className="text-xs text-slate-600 dark:text-slate-400">{user?.email}</p>
          </div>

          <Button variant="ghost" size="sm" onClick={handleLogout}>
            <LogOut className="size-4" />
            <span className="hidden sm:inline">{t("common:logout")}</span>
          </Button>
        </div>
      </div>
    </header>
  );
}
