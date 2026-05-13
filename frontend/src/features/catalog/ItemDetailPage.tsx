import { useMemo, useState } from "react";
import { useParams, Link } from "react-router-dom";
import {
  ArrowLeft,
  ArrowRightLeft,
  CalendarDays,
  Package,
  ShieldCheck,
  Tag,
  User,
} from "lucide-react";
import { format } from "date-fns";
import { useItemDetail } from "./useCatalog";
import { ItemStatusBadge, ItemConditionBadge } from "./ItemBadges";
import { Badge } from "../../components/ui/Badge";
import { Button } from "../../components/ui/Button";
import { Spinner } from "../../components/ui/Spinner";
import { EmptyState } from "../../components/ui/EmptyState";
import { SendOfferModal } from "../trade/SendOfferModal";
import { useAuth } from "../../auth/AuthContext";
import { routePaths } from "@/routes/routePaths.ts";
import type { ItemImageResponse } from "@/api/generated/types.ts";
import { cn } from "@/utils";

function ImageSection({ images }: { images: ItemImageResponse[] }) {
  const [selectedIdx, setSelectedIdx] = useState(0);
  const sorted = useMemo(
    () =>
      [...images].sort((a, b) => {
        if (a.isPrimary) return -1;
        if (b.isPrimary) return 1;
        return a.sortOrder - b.sortOrder;
      }),
    [images]
  );
  const primary = sorted[0];
  const displayed = sorted[selectedIdx] ?? primary;

  if (images.length === 0) {
    return (
      <div className="marketplace-panel overflow-hidden p-0">
        <div className="aspect-[5/4] bg-slate-100 flex flex-col items-center justify-center gap-2.5">
          <div className="flex size-14 items-center justify-center rounded-full bg-white border border-slate-200">
            <Package className="size-7 text-slate-300" />
          </div>
          <div className="text-center">
            <p className="text-sm font-medium text-slate-700">No photos available</p>
            <p className="mt-1 text-xs text-slate-500">The seller has not uploaded images for this listing yet.</p>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="marketplace-panel p-3">
      <div className="aspect-[5/4] overflow-hidden rounded-lg border border-slate-200 bg-slate-100">
        {displayed ? (
          <img
            src={displayed.url}
            alt={displayed.originalFilename}
            loading="lazy"
            className="w-full h-full object-cover"
          />
        ) : (
          <div className="w-full h-full flex items-center justify-center">
            <Package className="size-16 text-slate-300" />
          </div>
        )}
      </div>

      {sorted.length > 1 && (
        <div className="mt-2.5 grid grid-cols-5 gap-1.5 sm:grid-cols-6">
          {sorted.map((img, idx) => (
            <button
              key={img.uuid}
              type="button"
              onClick={() => setSelectedIdx(idx)}
              className={cn(
                "relative aspect-square overflow-hidden rounded-lg border bg-slate-100 transition-colors",
                idx === selectedIdx
                  ? "border-violet-400 ring-1 ring-violet-200"
                  : "border-slate-200 hover:border-slate-300"
              )}
            >
              <img
                src={img.url}
                alt={img.originalFilename}
                loading="lazy"
                className="w-full h-full object-cover"
              />
              {img.isPrimary ? <span className="sr-only">Primary image</span> : null}
            </button>
          ))}
        </div>
      )}
    </div>
  );
}

export function ItemDetailPage() {
  const { uuid } = useParams<{ uuid: string }>();
  const { data: item, isLoading, isError } = useItemDetail(uuid ?? "");
  const { user, isAuthenticated } = useAuth();
  const [showOfferModal, setShowOfferModal] = useState(false);

  if (isLoading) {
    return (
      <div className="marketplace-page min-h-screen px-4 py-12 sm:px-6">
        <div className="mx-auto flex max-w-[1600px] items-center justify-center py-24">
          <Spinner size="lg" />
        </div>
      </div>
    );
  }

  if (isError || !item) {
    return (
      <div className="marketplace-page min-h-screen px-4 py-12 sm:px-6">
        <div className="mx-auto max-w-3xl">
          <EmptyState
            title="Item not found"
            description="This item may have been removed or does not exist."
            action={
              <Link to={routePaths.marketplace}>
                <Button variant="outline">Back to Marketplace</Button>
              </Link>
            }
          />
        </div>
      </div>
    );
  }

  const isOwner = !!user && user.uuid === item.ownerUuid;
  const canProposeTrade = isAuthenticated && item.status === "ACTIVE" && !isOwner;
  const showGuestTradeCta = !isAuthenticated && item.status === "ACTIVE";
  const loginRedirectUrl = `${routePaths.login}?redirect=${encodeURIComponent(
    routePaths.marketplaceItem(item.uuid)
  )}`;
  const registerRedirectUrl = `${routePaths.register}?redirect=${encodeURIComponent(
    routePaths.marketplaceItem(item.uuid)
  )}`;
  const descriptionPreview = item.description?.trim();
  const listedDate = format(new Date(item.createdAt), "MMM d, yyyy");

  return (
    <div className="marketplace-page min-h-screen px-4 py-6 sm:px-6">
      <div className="mx-auto max-w-[1480px]">
        <Link
          to={routePaths.marketplace}
          className="mb-3 inline-flex items-center gap-2 text-sm font-medium text-slate-600 transition hover:text-violet-600"
        >
          <ArrowLeft className="size-4" />
          Back to marketplace
        </Link>

        <div className="grid grid-cols-1 gap-4 xl:grid-cols-[minmax(0,1.05fr)_minmax(0,0.9fr)_290px]">
          <div className="min-w-0">
            <ImageSection images={item.images ?? []} />
          </div>

          <section className="marketplace-panel p-4">
            <div className="flex flex-wrap gap-1.5">
              <ItemStatusBadge status={item.status} />
              <ItemConditionBadge condition={item.condition} />
              <span className="marketplace-soft-badge inline-flex items-center bg-violet-50 px-2 py-0.5 text-[11px] font-medium text-violet-700">
                {item.category.name}
              </span>
            </div>

            <h1 className="mt-3 text-xl font-semibold leading-tight text-slate-900 sm:text-[26px]">
              {item.title}
            </h1>

            <div className="mt-4 grid gap-2.5 sm:grid-cols-2">
              <div className="rounded-lg border border-slate-200 bg-slate-50 px-3.5 py-2.5">
                <div className="flex items-center gap-2 text-xs font-medium uppercase tracking-[0.08em] text-slate-500">
                  <CalendarDays className="size-3.5" />
                  Listed on
                </div>
                <p className="mt-1 text-sm font-medium text-slate-900">{listedDate}</p>
              </div>

              <div className="rounded-lg border border-slate-200 bg-slate-50 px-3.5 py-2.5">
                <div className="flex items-center gap-2 text-xs font-medium uppercase tracking-[0.08em] text-slate-500">
                  <Tag className="size-3.5" />
                  Category
                </div>
                <p className="mt-1 text-sm font-medium text-slate-900">{item.category.name}</p>
              </div>
            </div>

            {item.tags.length > 0 ? (
              <div className="mt-4">
                <h2 className="text-sm font-medium text-slate-500">Tags</h2>
                <div className="mt-1.5 flex flex-wrap gap-1.5">
                  {item.tags.map((tag) => (
                    <Badge
                      key={tag.uuid}
                      variant="secondary"
                      className="rounded-md border border-slate-200 bg-white px-2 py-0.5 text-[11px] font-medium text-slate-600 shadow-none"
                    >
                      {tag.name}
                    </Badge>
                  ))}
                </div>
              </div>
            ) : null}

            <div className="mt-4 border-t border-slate-200 pt-4">
              <h2 className="text-sm font-medium text-slate-500">Listing preview</h2>
              <p className="mt-1.5 whitespace-pre-line text-sm leading-6 text-slate-700">
                {descriptionPreview || "The seller has not added a description for this item yet."}
              </p>
            </div>
          </section>

          <aside className="space-y-3">
            <section className="marketplace-panel p-4">
              <div className="flex items-start gap-2.5">
                <div className="flex size-10 shrink-0 items-center justify-center rounded-full bg-violet-50 text-violet-600">
                  <User className="size-4.5" />
                </div>
                <div className="min-w-0">
                  <p className="text-xs font-medium uppercase tracking-[0.08em] text-slate-500">Seller</p>
                  {item.ownerUuid ? (
                    <Link
                      to={routePaths.publicProfile(item.ownerUuid)}
                      className="mt-0.5 block truncate text-[15px] font-semibold text-slate-900 transition hover:text-violet-600"
                    >
                      {item.ownerUsername}
                    </Link>
                  ) : (
                    <p className="mt-0.5 truncate text-[15px] font-semibold text-slate-900">{item.ownerUsername}</p>
                  )}
                  <p className="mt-0.5 text-sm text-slate-500">Member trader on Barter Platform</p>
                </div>
              </div>

              <div className="mt-3 rounded-lg border border-slate-200 bg-slate-50 px-3.5 py-2.5 text-sm text-slate-600">
                <div className="flex items-center gap-2 font-medium text-slate-700">
                  <ShieldCheck className="size-4 text-violet-600" />
                  Trade safely
                </div>
                <p className="mt-1 leading-5 text-slate-500">
                  Sign up to contact the owner and send a trade offer.
                </p>
              </div>

              {showGuestTradeCta ? (
                <div className="mt-4 space-y-2.5">
                  <Link to={registerRedirectUrl} className="block">
                    <Button className="h-10 w-full rounded-lg bg-violet-500 text-white hover:bg-violet-600">
                      <ArrowRightLeft className="size-4" />
                      Create account to propose trade
                    </Button>
                  </Link>
                  <Link
                    to={loginRedirectUrl}
                    className="inline-flex items-center justify-center text-sm font-medium text-violet-600 transition hover:text-violet-700"
                  >
                    Already have an account? Login
                  </Link>
                </div>
              ) : null}

              {canProposeTrade ? (
                <div className="mt-4 space-y-2.5">
                  <Button
                    onClick={() => setShowOfferModal(true)}
                    className="h-10 w-full rounded-lg bg-violet-500 text-white hover:bg-violet-600"
                  >
                    <ArrowRightLeft className="size-4" />
                    Propose Trade
                  </Button>
                  <p className="text-sm leading-5 text-slate-500">
                    Send your offer directly to the owner and continue the conversation after signing in.
                  </p>
                </div>
              ) : null}

              {isOwner ? (
                <div className="mt-4 space-y-2.5">
                  <div className="rounded-lg border border-violet-200 bg-violet-50 px-3.5 py-2.5 text-sm leading-5 text-violet-700">
                    This is your listing. You can manage it from your items dashboard.
                  </div>
                  <Link to={routePaths.myItemsEdit(item.uuid)} className="block">
                    <Button variant="outline" className="h-10 w-full rounded-lg border-slate-200">
                      Edit listing
                    </Button>
                  </Link>
                </div>
              ) : null}
            </section>
          </aside>
        </div>

        <section className="marketplace-panel mt-4 p-4">
          <h2 className="text-lg font-medium text-slate-900">Description</h2>
          <div className="mt-2.5 border-t border-slate-200 pt-3">
            <p className="whitespace-pre-line text-sm leading-6 text-slate-700">
              {descriptionPreview || "The seller has not added a description for this item yet."}
            </p>
          </div>
        </section>
      </div>

      {canProposeTrade && (
        <SendOfferModal
          isOpen={showOfferModal}
          onClose={() => setShowOfferModal(false)}
          receiverItem={item}
        />
      )}
    </div>
  );
}

