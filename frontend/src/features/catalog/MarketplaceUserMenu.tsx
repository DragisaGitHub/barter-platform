import * as DropdownMenu from "@radix-ui/react-dropdown-menu";
import { ChevronDown, LayoutDashboard, LogOut, User } from "lucide-react";
import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router-dom";
import { useAuth } from "@/auth/AuthContext";
import { routePaths } from "@/routes/routePaths";
import { cn } from "@/utils";

interface MarketplaceUserMenuProps {
  username?: string | null;
  compact?: boolean;
  className?: string;
  align?: "start" | "center" | "end";
}

export function MarketplaceUserMenu({
  username,
  compact = false,
  className,
  align = "end",
}: MarketplaceUserMenuProps) {
  const navigate = useNavigate();
  const { logout } = useAuth();
  const { t } = useTranslation(["common"]);
  const usernameLabel = username?.trim() || t("common:myProfile");

  const handleLogout = async () => {
    await logout();
    navigate(routePaths.login, { replace: true });
  };

  return (
    <DropdownMenu.Root>
      <DropdownMenu.Trigger asChild>
        <button
          type="button"
          aria-label={compact ? usernameLabel : t("common:welcomeUser", { username: usernameLabel })}
          className={cn(
            "inline-flex items-center rounded-lg border border-violet-200 bg-violet-50 font-medium text-violet-700 transition hover:border-violet-300 hover:bg-violet-100 focus:outline-none focus:ring-2 focus:ring-violet-200 focus:ring-offset-2",
            compact ? "gap-1.5 px-3 py-1.5 text-xs" : "h-10 gap-2 px-4 text-sm",
            className
          )}
        >
          {compact ? (
            <>
              <User className="size-3.5 shrink-0" />
              <span className="max-w-24 truncate">{usernameLabel}</span>
            </>
          ) : (
            <span className="max-w-64 truncate">{t("common:welcomeUser", { username: usernameLabel })}</span>
          )}
          <ChevronDown className={cn("shrink-0 opacity-70", compact ? "size-3.5" : "size-4")} />
        </button>
      </DropdownMenu.Trigger>

      <DropdownMenu.Portal>
        <DropdownMenu.Content
          align={align}
          sideOffset={8}
          className="z-50 min-w-56 overflow-hidden rounded-xl border border-slate-200 bg-white p-1.5 text-slate-700 shadow-[0_18px_48px_rgba(15,23,42,0.14)] outline-none"
        >
          <DropdownMenu.Item
            onSelect={() => navigate(routePaths.profile)}
            className="flex cursor-pointer items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium outline-none transition data-highlighted:bg-violet-50 data-highlighted:text-violet-700"
          >
            <User className="size-4 shrink-0" />
            {t("common:myProfile")}
          </DropdownMenu.Item>

          <DropdownMenu.Item
            onSelect={() => navigate(routePaths.dashboard)}
            className="flex cursor-pointer items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium outline-none transition data-highlighted:bg-violet-50 data-highlighted:text-violet-700"
          >
            <LayoutDashboard className="size-4 shrink-0" />
            {t("common:dashboard")}
          </DropdownMenu.Item>

          <DropdownMenu.Separator className="my-1 h-px bg-slate-200" />

          <DropdownMenu.Item
            onSelect={() => {
              void handleLogout();
            }}
            className="flex cursor-pointer items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium text-rose-600 outline-none transition data-highlighted:bg-rose-50 data-highlighted:text-rose-700"
          >
            <LogOut className="size-4 shrink-0" />
            {t("common:logout")}
          </DropdownMenu.Item>
        </DropdownMenu.Content>
      </DropdownMenu.Portal>
    </DropdownMenu.Root>
  );
}

