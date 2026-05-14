import type { ReactNode } from "react";
import { Link, useNavigate } from "react-router-dom";
import {
  ArrowRight,
  Bell,
  CheckCircle2,
  Heart,
  Inbox,
  LogOut,
  Mail,
  Plus,
  Send,
  ShieldCheck,
  ShieldEllipsis,
  Store,
  UserCircle2,
  Package,
} from "lucide-react";
import { useAuth } from "@/auth/AuthContext";
import { Badge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/Card";
import { EmptyState } from "@/components/ui/EmptyState";
import { Spinner } from "@/components/ui/Spinner";
import { useFavoriteItems, useMyItems } from "@/features/catalog/useCatalog";
import { useUnreadNotificationCount } from "@/features/notifications/useNotifications";
import { useIncomingTradeOffers, useSentTradeOffers } from "@/features/trade/useTradeOffers";
import { routePaths } from "@/routes/routePaths";
import { cn } from "@/utils";
import type { RoleResponse, UserStatus } from "@/api/generated/types";
import type { LucideIcon } from "lucide-react";

function getUserInitials(username?: string, email?: string): string {
  const source = username?.trim() || email?.trim() || "Profile";
  const segments = source
    .split(/[^a-zA-Z0-9]+/)
    .filter(Boolean)
    .slice(0, 2);

  if (segments.length === 0) {
    return "PR";
  }

  return segments.map((segment) => segment[0]?.toUpperCase() ?? "").join("");
}

function formatDateLabel(value?: string | null): string {
  if (!value) {
    return "Not available";
  }

  return new Date(value).toLocaleString(undefined, {
    month: "short",
    day: "numeric",
    year: "numeric",
    hour: "numeric",
    minute: "2-digit",
  });
}

function formatMemberSince(value?: string | null): string {
  if (!value) {
    return "Member information unavailable";
  }

  return new Date(value).toLocaleDateString(undefined, {
    month: "long",
    year: "numeric",
  });
}

function getStatusVariant(status?: UserStatus): "default" | "success" | "warning" | "danger" {
  switch (status) {
    case "ACTIVE":
      return "success";
    case "PENDING_VERIFICATION":
      return "warning";
    case "SUSPENDED":
    case "BANNED":
      return "danger";
    default:
      return "default";
  }
}

function getRoleVariant(index: number): "primary" | "secondary" {
  return index === 0 ? "primary" : "secondary";
}

function formatStatusLabel(status?: UserStatus): string {
  if (!status) {
    return "Unknown status";
  }

  return status
    .toLowerCase()
    .split("_")
    .map((segment) => segment.charAt(0).toUpperCase() + segment.slice(1))
    .join(" ");
}

function InfoRow({
  label,
  value,
  multiLine = false,
}: {
  label: string;
  value: ReactNode;
  multiLine?: boolean;
}) {
  return (
    <div className="flex flex-col gap-1 border-b border-slate-200/80 py-3 last:border-b-0 dark:border-slate-700/70 sm:flex-row sm:items-start sm:justify-between sm:gap-4">
      <dt className="text-sm font-medium text-slate-500 dark:text-slate-400">{label}</dt>
      <dd
        className={cn(
          "text-sm text-slate-900 dark:text-slate-100 sm:text-right",
          multiLine && "sm:max-w-sm"
        )}
      >
        {value}
      </dd>
    </div>
  );
}

function MetricValue({
  value,
  isLoading,
  isError,
}: {
  value?: number;
  isLoading: boolean;
  isError: boolean;
}) {
  if (isLoading) {
    return <Spinner size="sm" className="text-current" />;
  }

  if (isError || value == null) {
    return <span aria-label="Unavailable">—</span>;
  }

  return <>{value}</>;
}

function ActionLink({
  to,
  icon: Icon,
  title,
  description,
  className,
}: {
  to: string;
  icon: LucideIcon;
  title: string;
  description: string;
  className?: string;
}) {
  return (
    <Link
      to={to}
      className={cn(
        "group flex items-center justify-between rounded-2xl border border-slate-200 bg-white px-4 py-3 text-left transition-all hover:border-indigo-300 hover:shadow-sm focus:outline-none focus-visible:ring-2 focus-visible:ring-indigo-500 dark:border-slate-700 dark:bg-slate-800 dark:hover:border-indigo-700/70",
        className
      )}
    >
      <div className="flex min-w-0 items-center gap-3">
        <div className="flex size-10 shrink-0 items-center justify-center rounded-xl bg-slate-100 text-slate-600 transition-colors group-hover:bg-indigo-50 group-hover:text-indigo-600 dark:bg-slate-700/70 dark:text-slate-300 dark:group-hover:bg-indigo-950/40 dark:group-hover:text-indigo-300">
          <Icon className="size-[18px]" />
        </div>
        <div className="min-w-0">
          <p className="text-sm font-semibold text-slate-900 dark:text-slate-100">{title}</p>
          <p className="text-xs text-slate-500 dark:text-slate-400">{description}</p>
        </div>
      </div>
      <ArrowRight className="size-4 shrink-0 text-slate-400 transition-transform group-hover:translate-x-0.5 group-hover:text-indigo-500 dark:text-slate-500 dark:group-hover:text-indigo-300" />
    </Link>
  );
}

interface OverviewItem {
  title: string;
  description: string;
  to: string;
  icon: LucideIcon;
  value?: number;
  isLoading: boolean;
  isError: boolean;
  accentClassName: string;
}

export function ProfilePage() {
  const { user, isLoading, logout } = useAuth();
  const navigate = useNavigate();

  const favoritesQuery = useFavoriteItems({ page: 0, size: 1 }, !!user);
  const myItemsQuery = useMyItems({ page: 0, size: 1 });
  const incomingOffersQuery = useIncomingTradeOffers({ page: 0, size: 1, sort: "createdAt,desc" });
  const sentOffersQuery = useSentTradeOffers({ page: 0, size: 1, sort: "createdAt,desc" });
  const notificationsQuery = useUnreadNotificationCount();

  const handleLogout = async () => {
    await logout();
    navigate(routePaths.login);
  };

  if (isLoading) {
    return (
      <div className="flex min-h-[50vh] items-center justify-center">
        <Spinner size="lg" />
      </div>
    );
  }

  if (!user) {
    return (
      <div className="mx-auto max-w-3xl">
        <Card>
          <EmptyState
            icon={<UserCircle2 className="size-12" />}
            title="Profile unavailable"
            description="We couldn't load your account details right now. Return to the marketplace or sign in again to continue."
            action={
              <div className="flex flex-wrap items-center justify-center gap-3">
                <Link to={routePaths.marketplace} className="inline-flex items-center rounded-lg bg-indigo-600 px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-indigo-700">
                  Go to Marketplace
                </Link>
                <Link to={routePaths.login} className="inline-flex items-center rounded-lg border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700 transition-colors hover:bg-slate-50 dark:border-slate-600 dark:text-slate-200 dark:hover:bg-slate-800">
                  Sign in again
                </Link>
              </div>
            }
          />
        </Card>
      </div>
    );
  }

  const overviewItems: OverviewItem[] = [
    {
      title: "Favorites",
      description: "Saved items to revisit",
      to: routePaths.favorites,
      icon: Heart,
      value: favoritesQuery.data?.totalElements,
      isLoading: favoritesQuery.isLoading,
      isError: favoritesQuery.isError,
      accentClassName: "bg-rose-100 text-rose-600 dark:bg-rose-950/30 dark:text-rose-300",
    },
    {
      title: "My Items",
      description: "Listings you're managing",
      to: routePaths.myItems,
      icon: Package,
      value: myItemsQuery.data?.totalElements,
      isLoading: myItemsQuery.isLoading,
      isError: myItemsQuery.isError,
      accentClassName: "bg-emerald-100 text-emerald-600 dark:bg-emerald-950/30 dark:text-emerald-300",
    },
    {
      title: "Incoming Offers",
      description: "Trades waiting on your review",
      to: routePaths.offersIncoming,
      icon: Inbox,
      value: incomingOffersQuery.data?.totalElements,
      isLoading: incomingOffersQuery.isLoading,
      isError: incomingOffersQuery.isError,
      accentClassName: "bg-amber-100 text-amber-600 dark:bg-amber-950/30 dark:text-amber-300",
    },
    {
      title: "Sent Offers",
      description: "Offers you've sent out",
      to: routePaths.offersSent,
      icon: Send,
      value: sentOffersQuery.data?.totalElements,
      isLoading: sentOffersQuery.isLoading,
      isError: sentOffersQuery.isError,
      accentClassName: "bg-indigo-100 text-indigo-600 dark:bg-indigo-950/30 dark:text-indigo-300",
    },
    {
      title: "Notifications",
      description: "Unread account updates",
      to: routePaths.notifications,
      icon: Bell,
      value: notificationsQuery.data?.count,
      isLoading: notificationsQuery.isLoading,
      isError: notificationsQuery.isError,
      accentClassName: "bg-sky-100 text-sky-600 dark:bg-sky-950/30 dark:text-sky-300",
    },
  ];

  return (
    <div className="mx-auto max-w-7xl space-y-6">
      <section className="overflow-hidden rounded-3xl border border-slate-200 bg-white shadow-sm dark:border-slate-700 dark:bg-slate-800">
        <div className="bg-gradient-to-r from-indigo-500/10 via-slate-100 to-emerald-500/10 px-5 py-6 dark:from-indigo-500/10 dark:via-slate-800 dark:to-emerald-500/10 sm:px-6 lg:px-8">
          <div className="flex flex-col gap-6 lg:flex-row lg:items-center lg:justify-between">
            <div className="flex min-w-0 items-start gap-4">
              <div className="flex size-[72px] shrink-0 items-center justify-center rounded-3xl bg-gradient-to-br from-indigo-600 to-violet-600 text-xl font-semibold text-white shadow-lg shadow-indigo-500/20">
                {getUserInitials(user.username, user.email)}
              </div>
              <div className="min-w-0">
                <div className="flex flex-wrap items-center gap-2">
                  <h1 className="truncate text-2xl font-bold text-slate-900 dark:text-white sm:text-3xl">
                    {user.username}
                  </h1>
                  {user.status === "ACTIVE" && (
                    <Badge variant="success" className="px-2.5 py-1">
                      Active account
                    </Badge>
                  )}
                </div>
                <div className="mt-2 flex items-center gap-2 text-sm text-slate-600 dark:text-slate-300">
                  <Mail className="size-4 shrink-0" />
                  <span className="truncate">{user.email}</span>
                </div>
                <p className="mt-3 max-w-2xl text-sm leading-6 text-slate-600 dark:text-slate-300">
                  Manage your listings and trade activity. Track favorites, offers, and conversations from one place.
                </p>
                <div className="mt-4 flex flex-wrap gap-2">
                  <Badge variant={user.emailVerified ? "success" : "warning"}>
                    {user.emailVerified ? "Email verified" : "Email verification pending"}
                  </Badge>
                  <Badge variant={getStatusVariant(user.status)}>{formatStatusLabel(user.status)}</Badge>
                  {user.roles.map((role, index) => (
                    <Badge key={role.uuid} variant={getRoleVariant(index)}>
                      {role.code}
                    </Badge>
                  ))}
                </div>
              </div>
            </div>

            <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:w-[26rem]">
              <div className="rounded-2xl border border-white/60 bg-white/80 p-4 backdrop-blur dark:border-slate-700/80 dark:bg-slate-900/50">
                <p className="text-xs font-semibold uppercase tracking-[0.16em] text-slate-500 dark:text-slate-400">
                  Member since
                </p>
                <p className="mt-2 text-sm font-semibold text-slate-900 dark:text-slate-100">
                  {formatMemberSince(user.createdAt)}
                </p>
              </div>
              <div className="rounded-2xl border border-white/60 bg-white/80 p-4 backdrop-blur dark:border-slate-700/80 dark:bg-slate-900/50">
                <p className="text-xs font-semibold uppercase tracking-[0.16em] text-slate-500 dark:text-slate-400">
                  Last sign in
                </p>
                <p className="mt-2 text-sm font-semibold text-slate-900 dark:text-slate-100">
                  {formatDateLabel(user.lastLoginAt)}
                </p>
              </div>
            </div>
          </div>
        </div>
      </section>

      <section>
        <div className="mb-3 flex items-center justify-between gap-3">
          <div>
            <h2 className="text-lg font-semibold text-slate-900 dark:text-slate-100">Account overview</h2>
            <p className="text-sm text-slate-600 dark:text-slate-400">
              Quick shortcuts for the sections you use most often.
            </p>
          </div>
        </div>

        <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-5">
          {overviewItems.map((item) => (
            <Link
              key={item.title}
              to={item.to}
              className="group block rounded-2xl focus:outline-none focus-visible:ring-2 focus-visible:ring-indigo-500"
            >
              <Card className="h-full border-slate-200/90 p-5 transition-all group-hover:-translate-y-0.5 group-hover:border-indigo-300 group-hover:shadow-md dark:border-slate-700/90 dark:group-hover:border-indigo-700/70">
                <div className="flex h-full flex-col gap-4">
                  <div className="flex items-start justify-between gap-3">
                    <div className={cn("flex size-11 items-center justify-center rounded-2xl", item.accentClassName)}>
                      <item.icon className="size-5" />
                    </div>
                    <ArrowRight className="size-4 text-slate-300 transition-all group-hover:translate-x-0.5 group-hover:text-indigo-500 dark:text-slate-600 dark:group-hover:text-indigo-300" />
                  </div>
                  <div className="mt-auto">
                    <div className="flex min-h-8 items-center text-2xl font-bold tracking-tight text-slate-900 dark:text-slate-50">
                      <MetricValue
                        value={item.value}
                        isLoading={item.isLoading}
                        isError={item.isError}
                      />
                    </div>
                    <h3 className="mt-2 text-sm font-semibold text-slate-900 dark:text-slate-100">{item.title}</h3>
                    <p className="mt-1 text-xs leading-5 text-slate-500 dark:text-slate-400">{item.description}</p>
                  </div>
                </div>
              </Card>
            </Link>
          ))}
        </div>
      </section>

      <section className="grid gap-6 xl:grid-cols-[minmax(0,2fr)_minmax(320px,1fr)]">
        <Card className="rounded-3xl">
          <CardHeader className="mb-0 border-b border-slate-200 pb-4 dark:border-slate-700">
            <CardTitle className="text-xl">Account details</CardTitle>
            <CardDescription>
              Review your current account identity, access, and sign-in details.
            </CardDescription>
          </CardHeader>
          <CardContent className="pt-2">
            <dl>
              <InfoRow label="Username" value={user.username} />
              <InfoRow label="Email" value={user.email} />
              <InfoRow
                label="Roles"
                value={
                  <div className="flex flex-wrap gap-2 sm:justify-end">
                    {user.roles.map((role: RoleResponse, index) => (
                      <Badge key={role.uuid} variant={getRoleVariant(index)}>
                        {role.name}
                      </Badge>
                    ))}
                  </div>
                }
              />
              <InfoRow label="Account status" value={formatStatusLabel(user.status)} />
              <InfoRow
                label="Email confirmation"
                value={
                  <span className="inline-flex items-center gap-2 sm:justify-end">
                    <CheckCircle2 className={cn("size-4", user.emailVerified ? "text-emerald-500" : "text-amber-500")} />
                    {user.emailVerified ? "Verified" : "Pending verification"}
                  </span>
                }
              />
              <InfoRow
                label="Multi-factor authentication"
                value={user.mfaEnabled ? "Enabled" : "Not enabled"}
              />
              {user.oauthAccounts.length > 0 && (
                <InfoRow
                  label="Connected providers"
                  value={
                    <div className="flex flex-wrap gap-2 sm:justify-end">
                      {user.oauthAccounts.map((account) => (
                        <Badge key={account.uuid} variant="default">
                          {account.provider}
                        </Badge>
                      ))}
                    </div>
                  }
                />
              )}
              <InfoRow label="Created" value={formatDateLabel(user.createdAt)} />
              <InfoRow label="Last sign in" value={formatDateLabel(user.lastLoginAt)} />
              <InfoRow
                label="Permissions"
                multiLine
                value={
                  user.permissions.length > 0
                    ? `${user.permissions.length} permission${user.permissions.length === 1 ? "" : "s"} assigned`
                    : "No permissions available"
                }
              />
            </dl>
          </CardContent>
        </Card>

        <div className="space-y-6">
          <Card className="rounded-3xl">
            <CardHeader>
              <CardTitle className="text-xl">Quick actions</CardTitle>
              <CardDescription>
                Jump back into the marketplace or continue managing your listings.
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-3">
              <ActionLink
                to={routePaths.marketplace}
                icon={Store}
                title="Go to Marketplace"
                description="Browse active listings and discover new trade opportunities."
              />
              <ActionLink
                to={routePaths.myItemsNew}
                icon={Plus}
                title="Create New Listing"
                description="Post a new item and start receiving trade offers."
              />
              <Button
                type="button"
                variant="outline"
                className="w-full justify-between rounded-2xl border-slate-200 px-4 py-3 text-left dark:border-slate-700"
                onClick={handleLogout}
              >
                <span className="flex items-center gap-3">
                  <span className="flex size-10 items-center justify-center rounded-xl bg-slate-100 text-slate-600 dark:bg-slate-700/70 dark:text-slate-300">
                    <LogOut className="size-[18px]" />
                  </span>
                  <span>
                    <span className="block text-sm font-semibold">Logout</span>
                    <span className="block text-xs text-slate-500 dark:text-slate-400">
                      Securely end your current session.
                    </span>
                  </span>
                </span>
                <ArrowRight className="size-4 shrink-0 text-slate-400" />
              </Button>
            </CardContent>
          </Card>

          <Card className="rounded-3xl border-indigo-200/80 bg-indigo-50/70 dark:border-indigo-900/60 dark:bg-indigo-950/20">
            <CardContent className="p-5">
              <div className="flex items-start gap-3">
                <div className="flex size-11 shrink-0 items-center justify-center rounded-2xl bg-white text-indigo-600 shadow-sm dark:bg-slate-900/60 dark:text-indigo-300">
                  <ShieldCheck className="size-5" />
                </div>
                <div>
                  <h3 className="text-sm font-semibold text-slate-900 dark:text-slate-100">
                    Marketplace account center
                  </h3>
                  <p className="mt-1 text-sm leading-6 text-slate-600 dark:text-slate-300">
                    Keep an eye on saved items, offer activity, and notifications so you can respond quickly when a trade moves forward.
                  </p>
                </div>
              </div>
            </CardContent>
          </Card>

          <Card className="rounded-3xl">
            <CardContent className="p-5">
              <div className="flex items-start gap-3">
                <div className="flex size-11 shrink-0 items-center justify-center rounded-2xl bg-slate-100 text-slate-600 dark:bg-slate-700/70 dark:text-slate-300">
                  <ShieldEllipsis className="size-5" />
                </div>
                <div>
                  <h3 className="text-sm font-semibold text-slate-900 dark:text-slate-100">
                    Account snapshot
                  </h3>
                  <p className="mt-1 text-sm text-slate-600 dark:text-slate-400">
                    Your profile reflects live authentication data only. No hidden settings, no placeholder metrics.
                  </p>
                </div>
              </div>
            </CardContent>
          </Card>
        </div>
      </section>
    </div>
  );
}

