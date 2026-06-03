import { useState } from "react";
import { Send } from "lucide-react";
import { useSentTradeOffers } from "./useTradeOffers";
import { TradeOfferCard } from "./TradeOfferCard";
import { Pagination } from "../../components/data/Pagination";
import { EmptyState } from "../../components/ui/EmptyState";
import { Button } from "../../components/ui/Button";
import { Spinner } from "../../components/ui/Spinner";
import { useAuth } from "../../auth/AuthContext";
import type { TradeOfferStatus } from "@/api/generated/types.ts";
import type { ListTradeOffersParams } from "@/api/tradeOfferApi.ts";
import { useTranslation } from "react-i18next";
import { Link } from "react-router-dom";
import { routePaths } from "@/routes/routePaths";

const STATUS_OPTIONS: { value: TradeOfferStatus | ""; labelKey: string }[] = [
  { value: "", labelKey: "allStatuses" },
  { value: "PENDING", labelKey: "status.pending" },
  { value: "ACCEPTED", labelKey: "status.awaitingCompletion" },
  { value: "COMPLETED", labelKey: "status.completed" },
  { value: "REJECTED", labelKey: "status.rejected" },
  { value: "CANCELLED", labelKey: "status.cancelled" },
  { value: "EXPIRED", labelKey: "status.expired" },
  { value: "INVALIDATED", labelKey: "status.invalidated" },
];

export function SentOffersPage() {
  const { user } = useAuth();
  const { t } = useTranslation(["trade", "common"]);
  const [params, setParams] = useState<ListTradeOffersParams>({
    page: 0,
    size: 12,
    sort: "createdAt,desc",
  });

  const { data, isLoading, isError } = useSentTradeOffers(params);

  const handleStatusChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    const val = e.target.value as TradeOfferStatus | "";
    setParams((prev) => ({
      ...prev,
      page: 0,
      status: val || undefined,
    }));
  };

  return (
    <div className="max-w-7xl mx-auto">
      <h1 className="text-3xl font-bold text-slate-900 dark:text-white mb-6">{t("trade:sentOffers")}</h1>

      <div className="mb-6">
        <select
          className="rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100"
          value={params.status ?? ""}
          onChange={handleStatusChange}
        >
          {STATUS_OPTIONS.map((opt) => (
            <option key={opt.value} value={opt.value}>
              {t(`trade:${opt.labelKey}`)}
            </option>
          ))}
        </select>
      </div>

      {isLoading && (
        <div className="flex items-center justify-center py-20">
          <Spinner size="lg" />
        </div>
      )}

      {isError && (
        <EmptyState
          title={t("trade:failedToLoadOffers")}
          description={t("trade:failedToLoadOffersBody")}
          action={
            <Button variant="outline" onClick={() => window.location.reload()}>
              {t("common:tryAgain")}
            </Button>
          }
        />
      )}

      {data && data.content.length === 0 && (
        <EmptyState
          icon={<Send className="size-16" />}
          title={t("trade:noSentOffers")}
          description={t("trade:noSentOffersBody")}
          action={
            <Link to={routePaths.marketplace}>
              <Button>{t("trade:emptyCta.browseMarketplace")}</Button>
            </Link>
          }
        />
      )}

      {data && data.content.length > 0 && (
        <>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            {data.content.map((offer) => (
              <TradeOfferCard
                key={offer.uuid}
                offer={offer}
                currentUserUuid={user?.uuid ?? ""}
              />
            ))}
          </div>

          {data.totalPages > 1 && (
            <div className="mt-6">
              <Pagination
                currentPage={data.page}
                totalPages={data.totalPages}
                onPageChange={(page) => setParams((prev) => ({ ...prev, page }))}
              />
            </div>
          )}
        </>
      )}
    </div>
  );
}

