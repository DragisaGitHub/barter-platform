import { useParams, Link } from "react-router-dom";
import { UserCircle, Package } from "lucide-react";
import { usePublicProfile, usePublicProfileItems } from "./useProfile";
import { TrustSummary } from "./TrustSummary";
import { ItemCard } from "../catalog/ItemCard";
import { Spinner } from "../../components/ui/Spinner";
import { EmptyState } from "../../components/ui/EmptyState";
import { Button } from "../../components/ui/Button";
import { routePaths } from "@/routes/routePaths.ts";

function formatJoinDate(iso: string): string {
  const date = new Date(iso);
  return date.toLocaleDateString(undefined, { month: "long", year: "numeric" });
}

export function PublicProfilePage() {
  const { uuid } = useParams<{ uuid: string }>();
  const {
    data: profile,
    isLoading: profileLoading,
    isError: profileError,
  } = usePublicProfile(uuid ?? "");
  const {
    data: itemsPage,
    isLoading: itemsLoading,
  } = usePublicProfileItems(uuid ?? "");

  if (profileLoading) {
    return (
      <div className="flex items-center justify-center py-20">
        <Spinner size="lg" />
      </div>
    );
  }

  if (profileError || !profile) {
    return (
      <div className="max-w-3xl mx-auto px-4 py-12">
        <EmptyState
          icon={<UserCircle className="size-12" />}
          title="User not found"
          description="This user profile may have been removed or is unavailable."
          action={
            <Link to={routePaths.home}>
              <Button variant="outline">Back to Home</Button>
            </Link>
          }
        />
      </div>
    );
  }

  const items = itemsPage?.content ?? [];

  return (
    <div className="max-w-4xl mx-auto px-4 py-8">
      {/* Header */}
      <div className="flex items-center gap-4 mb-6">
        <div className="size-16 rounded-full bg-indigo-100 dark:bg-indigo-900/30 flex items-center justify-center">
          <UserCircle className="size-10 text-indigo-500 dark:text-indigo-400" />
        </div>
        <div>
          <h1 className="text-2xl font-bold text-slate-900 dark:text-white">
            {profile.username}
          </h1>
          <p className="text-sm text-slate-500 dark:text-slate-400">
            Member since {formatJoinDate(profile.joinedAt)}
          </p>
        </div>
      </div>

      {/* Trust summary */}
      <div className="mb-8">
        <TrustSummary
          activeItemCount={profile.activeItemCount}
          completedTradeCount={profile.completedTradeCount}
          cancelledTradeCount={profile.cancelledTradeCount}
          averageRating={profile.averageRating}
        />
      </div>

      {/* Active items */}
      <h2 className="text-lg font-semibold text-slate-900 dark:text-slate-100 mb-4">
        Active Items
      </h2>

      {itemsLoading ? (
        <div className="flex items-center justify-center py-12">
          <Spinner size="md" />
        </div>
      ) : items.length === 0 ? (
        <EmptyState
          icon={<Package className="size-10" />}
          title="No active items"
          description="This user doesn't have any active items listed right now."
        />
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
          {items.map((item) => (
            <ItemCard key={item.uuid} item={item} />
          ))}
        </div>
      )}
    </div>
  );
}

