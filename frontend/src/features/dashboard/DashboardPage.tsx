import { useMemo } from "react";
import { Link } from "react-router-dom";
import {
  Store,
  Package,
  Shield,
  Plus,
  Inbox,
  Send,
  Archive,
  ArrowRightLeft,
  Clock,
} from "lucide-react";
import { useAuth } from "../../auth/AuthContext";
import { Card, CardContent, CardHeader, CardTitle } from "../../components/ui/Card";
import { Button } from "../../components/ui/Button";
import { Spinner } from "../../components/ui/Spinner";
import { useMyItems, useSearchItems } from "../catalog/useCatalog";
import { useIncomingTradeOffers, useSentTradeOffers } from "../trade/useTradeOffers";
import { TradeOfferStatusBadge } from "../trade/TradeOfferStatusBadge";
import type { TradeOfferStatus } from "@/api/generated/types.ts";

const STATUS_EXPLANATION: Record<TradeOfferStatus, string> = {
  PENDING: "Waiting for response",
  ACCEPTED: "Trade accepted",
  REJECTED: "Trade rejected",
  CANCELLED: "Cancelled by sender",
  EXPIRED: "Expired",
};

export function DashboardPage() {
  const { user, hasRole } = useAuth();
  const isAdmin = hasRole("ADMIN");

  // My items data (active + archived)
  const { data: myActiveData, isLoading: myActiveLoading } = useMyItems({ page: 0, size: 1, status: "ACTIVE" });
  const { data: myArchivedData, isLoading: myArchivedLoading } = useMyItems({ page: 0, size: 1, status: "ARCHIVED" });

  // Trade offer data (pending)
  const { data: incomingData, isLoading: incomingLoading } = useIncomingTradeOffers({
    page: 0,
    size: 5,
    status: "PENDING",
    sort: "createdAt,desc",
  });
  const { data: sentData, isLoading: sentLoading } = useSentTradeOffers({
    page: 0,
    size: 5,
    status: "PENDING",
    sort: "createdAt,desc",
  });

  // Recent trade activity (all statuses)
  const { data: recentIncoming } = useIncomingTradeOffers({
    page: 0,
    size: 3,
    sort: "createdAt,desc",
  });
  const { data: recentSent } = useSentTradeOffers({
    page: 0,
    size: 3,
    sort: "createdAt,desc",
  });

  // Recent marketplace items — fetch only ACTIVE items from server, request extra to
  // allow client-side filtering of the current user's own items.
  const { data: marketplaceData, isLoading: marketplaceLoading } = useSearchItems({
    page: 0,
    size: 20,
    sort: "createdAt,desc",
    status: "ACTIVE",
  });

  // Filter marketplace to exclude current user's own items
  const recentMarketplace = useMemo(() => {
    if (!marketplaceData || !user) return [];
    return marketplaceData.content
      .filter((item) => item.ownerUuid !== user.uuid)
      .slice(0, 5);
  }, [marketplaceData, user]);

  // Combine recent trade activity
  const recentActivity = useMemo(() => {
    const items = [
      ...(recentIncoming?.content.map((o) => ({ ...o, direction: "incoming" as const })) ?? []),
      ...(recentSent?.content.map((o) => ({ ...o, direction: "sent" as const })) ?? []),
    ];
    items.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
    return items.slice(0, 5);
  }, [recentIncoming, recentSent]);

  const activeCount = myActiveData?.totalElements ?? 0;
  const archivedCount = myArchivedData?.totalElements ?? 0;
  const pendingIncoming = incomingData?.totalElements ?? 0;
  const pendingSent = sentData?.totalElements ?? 0;

  const isStatsLoading = myActiveLoading || myArchivedLoading || incomingLoading || sentLoading;

  return (
    <div className="max-w-7xl mx-auto">
      {/* Welcome */}
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-slate-900 dark:text-white">
          Welcome back, {user?.username}!
        </h1>
        <p className="text-slate-600 dark:text-slate-400 mt-2">
          Here&apos;s what&apos;s happening on your account.
        </p>
      </div>

      {/* Stats cards */}
      <div className="grid gap-4 grid-cols-2 lg:grid-cols-4 mb-8">
        <Link to="/my-items">
          <Card className="hover:shadow-md transition-shadow cursor-pointer h-full">
            <div className="flex items-center gap-3">
              <div className="bg-emerald-100 dark:bg-emerald-900/30 rounded-lg p-2.5">
                <Package className="size-5 text-emerald-600 dark:text-emerald-400" />
              </div>
              <div>
                {isStatsLoading ? (
                  <Spinner size="sm" />
                ) : (
                  <p className="text-2xl font-bold text-slate-900 dark:text-white">{activeCount}</p>
                )}
                <p className="text-xs text-slate-500 dark:text-slate-400">Active Items</p>
              </div>
            </div>
          </Card>
        </Link>

        <Link to="/offers/incoming">
          <Card className="hover:shadow-md transition-shadow cursor-pointer h-full">
            <div className="flex items-center gap-3">
              <div className="bg-amber-100 dark:bg-amber-900/30 rounded-lg p-2.5">
                <Inbox className="size-5 text-amber-600 dark:text-amber-400" />
              </div>
              <div>
                {isStatsLoading ? (
                  <Spinner size="sm" />
                ) : (
                  <p className="text-2xl font-bold text-slate-900 dark:text-white">{pendingIncoming}</p>
                )}
                <p className="text-xs text-slate-500 dark:text-slate-400">Pending Incoming</p>
              </div>
            </div>
          </Card>
        </Link>

        <Link to="/offers/sent">
          <Card className="hover:shadow-md transition-shadow cursor-pointer h-full">
            <div className="flex items-center gap-3">
              <div className="bg-indigo-100 dark:bg-indigo-900/30 rounded-lg p-2.5">
                <Send className="size-5 text-indigo-600 dark:text-indigo-400" />
              </div>
              <div>
                {isStatsLoading ? (
                  <Spinner size="sm" />
                ) : (
                  <p className="text-2xl font-bold text-slate-900 dark:text-white">{pendingSent}</p>
                )}
                <p className="text-xs text-slate-500 dark:text-slate-400">Pending Sent</p>
              </div>
            </div>
          </Card>
        </Link>

        <Link to="/my-items">
          <Card className="hover:shadow-md transition-shadow cursor-pointer h-full">
            <div className="flex items-center gap-3">
              <div className="bg-slate-100 dark:bg-slate-700 rounded-lg p-2.5">
                <Archive className="size-5 text-slate-600 dark:text-slate-400" />
              </div>
              <div>
                {isStatsLoading ? (
                  <Spinner size="sm" />
                ) : (
                  <p className="text-2xl font-bold text-slate-900 dark:text-white">{archivedCount}</p>
                )}
                <p className="text-xs text-slate-500 dark:text-slate-400">Archived Items</p>
              </div>
            </div>
          </Card>
        </Link>
      </div>

      {/* Quick actions */}
      <div className="mb-8">
        <h2 className="text-lg font-semibold text-slate-900 dark:text-white mb-4">Quick Actions</h2>
        <div className="flex flex-wrap gap-3">
          <Link to="/marketplace">
            <Button variant="outline">
              <Store className="size-4" />
              Browse Marketplace
            </Button>
          </Link>
          <Link to="/my-items/new">
            <Button>
              <Plus className="size-4" />
              Create Item
            </Button>
          </Link>
          <Link to="/offers/incoming">
            <Button variant="outline">
              <Inbox className="size-4" />
              View Incoming Offers
              {pendingIncoming > 0 && (
                <span className="ml-1 inline-flex items-center justify-center min-w-[18px] h-[18px] px-1 rounded-full bg-amber-500 text-white text-xs font-semibold">
                  {pendingIncoming}
                </span>
              )}
            </Button>
          </Link>
        </div>
      </div>

      <div className="grid gap-8 lg:grid-cols-2 mb-8">
        {/* Recent Marketplace Items */}
        <div>
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-lg font-semibold text-slate-900 dark:text-white">Recent Marketplace Items</h2>
            <Link to="/marketplace" className="text-sm text-indigo-600 dark:text-indigo-400 hover:underline">
              View all
            </Link>
          </div>
          {marketplaceLoading && (
            <Card>
              <div className="flex items-center justify-center py-8">
                <Spinner size="md" />
              </div>
            </Card>
          )}
          {!marketplaceLoading && recentMarketplace.length === 0 && (
            <Card>
              <div className="text-center py-8">
                <Store className="size-10 text-slate-300 dark:text-slate-600 mx-auto mb-3" />
                <p className="text-sm text-slate-500 dark:text-slate-400">
                  No tradeable items from other users right now.
                </p>
                <p className="text-xs text-slate-400 dark:text-slate-500 mt-1">
                  Check back soon — new items appear here as they&apos;re listed.
                </p>
              </div>
            </Card>
          )}
          {!marketplaceLoading && recentMarketplace.length > 0 && (
            <div className="space-y-2">
              {recentMarketplace.map((item) => (
                <Link key={item.uuid} to={`/marketplace/items/${item.uuid}`}>
                  <Card className="hover:shadow-md transition-shadow cursor-pointer !p-4">
                    <div className="flex items-center justify-between">
                      <div className="min-w-0 flex-1">
                        <p className="font-medium text-slate-900 dark:text-slate-100 truncate">
                          {item.title}
                        </p>
                        <p className="text-xs text-slate-500 dark:text-slate-400">
                          {item.categoryName} · by {item.ownerUsername}
                        </p>
                      </div>
                      <span className="text-xs text-slate-400 dark:text-slate-500 ml-3 shrink-0">
                        {new Date(item.createdAt).toLocaleDateString()}
                      </span>
                    </div>
                  </Card>
                </Link>
              ))}
            </div>
          )}
        </div>

        {/* Recent Trade Activity */}
        <div>
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-lg font-semibold text-slate-900 dark:text-white">Recent Trade Activity</h2>
            <Link to="/offers/incoming" className="text-sm text-indigo-600 dark:text-indigo-400 hover:underline">
              View all
            </Link>
          </div>
          {(incomingLoading || sentLoading) && recentActivity.length === 0 && (
            <Card>
              <div className="flex items-center justify-center py-8">
                <Spinner size="md" />
              </div>
            </Card>
          )}
          {!(incomingLoading || sentLoading) && recentActivity.length === 0 && (
            <Card>
              <div className="text-center py-8">
                <ArrowRightLeft className="size-10 text-slate-300 dark:text-slate-600 mx-auto mb-3" />
                <p className="text-sm text-slate-500 dark:text-slate-400">
                  No pending trade offers right now.
                </p>
                <p className="text-xs text-slate-400 dark:text-slate-500 mt-1">
                  Trade activity will appear here once you start trading.
                </p>
              </div>
            </Card>
          )}
          {recentActivity.length > 0 && (
            <div className="space-y-2">
              {recentActivity.map((offer) => {
                const isSender = offer.sender.uuid === user?.uuid;
                const summary = isSender
                  ? `You offered ${offer.senderItem!.title} for ${offer.receiver.username}'s ${offer.receiverItem.title}`
                  : `${offer.sender.username} wants your ${offer.receiverItem.title} for their ${offer.senderItem!.title}`;

                return (
                  <Link key={offer.uuid} to={`/offers/${offer.uuid}`}>
                    <Card className="hover:shadow-md transition-shadow cursor-pointer !p-4">
                      <div className="flex items-start justify-between gap-3">
                        <div className="min-w-0 flex-1">
                          <div className="flex items-center gap-2 mb-1">
                            {offer.direction === "incoming" ? (
                              <Inbox className="size-3.5 text-amber-500 shrink-0" />
                            ) : (
                              <Send className="size-3.5 text-indigo-500 shrink-0" />
                            )}
                            <TradeOfferStatusBadge status={offer.status} />
                          </div>
                          <p className="text-sm text-slate-700 dark:text-slate-300 line-clamp-1">
                            {summary}
                          </p>
                          <p className="text-xs text-slate-400 dark:text-slate-500 mt-0.5">
                            <Clock className="size-3 inline mr-1" />
                            {STATUS_EXPLANATION[offer.status]} · {new Date(offer.createdAt).toLocaleDateString()}
                          </p>
                        </div>
                      </div>
                    </Card>
                  </Link>
                );
              })}
            </div>
          )}
        </div>
      </div>

      {/* Admin card */}
      {isAdmin && (
        <Card className="border-indigo-200 dark:border-indigo-800">
          <CardHeader>
            <div className="flex items-center gap-3">
              <div className="bg-indigo-100 dark:bg-indigo-900/30 rounded-lg p-2">
                <Shield className="size-5 text-indigo-600 dark:text-indigo-400" />
              </div>
              <div>
                <CardTitle>Admin Access</CardTitle>
                <p className="text-sm text-slate-600 dark:text-slate-400 mt-1">
                  You have administrator privileges
                </p>
              </div>
            </div>
          </CardHeader>
          <CardContent>
            <Link to="/admin">
              <Button>Go to Admin Dashboard</Button>
            </Link>
          </CardContent>
        </Card>
      )}
    </div>
  );
}
