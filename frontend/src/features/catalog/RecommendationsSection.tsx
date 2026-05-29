import { Link } from "react-router-dom";
import { Compass, Package } from "lucide-react";
import type { RecommendationItemResponse, RecommendationReason } from "@/api/generated/types";
import { routePaths } from "@/routes/routePaths";
import { Card } from "../../components/ui/Card";
import { Spinner } from "../../components/ui/Spinner";
import { ItemCard } from "./ItemCard";
import { useRecommendations } from "./useCatalog";
import { useTranslation } from "react-i18next";

interface RecommendationsSectionProps {
  size?: number;
  className?: string;
  titleClassName?: string;
  gridClassName?: string;
  showViewAllLink?: boolean;
}

export function RecommendationsSection({
  size = 4,
  className = "",
  titleClassName = "text-lg font-semibold text-slate-900 dark:text-white",
  gridClassName = "grid gap-4 sm:grid-cols-2 xl:grid-cols-4",
  showViewAllLink = true,
}: RecommendationsSectionProps) {
  const { t } = useTranslation("catalog");
  const { data, isLoading, isError } = useRecommendations({
    page: 0,
    size,
    sort: "recommendationScore,desc",
  });
  const recommendations = data?.content ?? [];

  return (
    <section className={className} aria-labelledby="recommendations-heading">
      <div className="mb-4 flex items-start justify-between gap-3">
        <div>
          <h2 id="recommendations-heading" className={titleClassName}>
            {t("recommendations.title")}
          </h2>
          <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">
            {t("recommendations.subtitle")}
          </p>
        </div>
        {showViewAllLink ? (
          <Link to={routePaths.marketplace} className="shrink-0 text-sm text-indigo-600 hover:underline dark:text-indigo-400">
            {t("recommendations.browseAll")}
          </Link>
        ) : null}
      </div>

      {isLoading ? (
        <Card>
          <div className="flex items-center justify-center py-8">
            <Spinner size="md" />
          </div>
        </Card>
      ) : null}

      {!isLoading && isError ? (
        <Card>
          <div className="flex items-center gap-3 py-3 text-sm text-slate-500 dark:text-slate-400">
            <Compass className="size-5 text-slate-300 dark:text-slate-600" />
            <span>{t("recommendations.loadError")}</span>
          </div>
        </Card>
      ) : null}

      {!isLoading && !isError && recommendations.length === 0 ? (
        <Card>
          <div className="text-center py-8">
            <Package className="size-10 text-slate-300 dark:text-slate-600 mx-auto mb-3" />
            <p className="text-sm text-slate-500 dark:text-slate-400">{t("recommendations.emptyTitle")}</p>
            <p className="mt-1 text-xs text-slate-400 dark:text-slate-500">{t("recommendations.emptyDescription")}</p>
          </div>
        </Card>
      ) : null}

      {!isLoading && !isError && recommendations.length > 0 ? (
        <div className={gridClassName}>
          {recommendations.map((recommendation) => (
            <RecommendationCard key={recommendation.item.uuid} recommendation={recommendation} />
          ))}
        </div>
      ) : null}
    </section>
  );
}

export function RecommendationCard({ recommendation }: { recommendation: RecommendationItemResponse }) {
  const { t } = useTranslation("catalog");

  return (
    <div className="space-y-2">
      <div className="inline-flex items-center rounded-full bg-indigo-50 px-2.5 py-1 text-xs font-medium text-indigo-700 dark:bg-indigo-900/30 dark:text-indigo-300">
        {recommendationReasonLabel(recommendation.reason, t)}
      </div>
      <ItemCard item={recommendation.item} />
    </div>
  );
}

function recommendationReasonLabel(
  reason: RecommendationReason,
  t: (key: string) => string,
) {
  const keyByReason: Record<RecommendationReason, string> = {
    BECAUSE_OF_INTERESTS: "recommendations.reasons.becauseOfInterests",
    SIMILAR_TO_YOUR_LISTINGS: "recommendations.reasons.similarToYourListings",
    NEAR_PREFERRED_EXCHANGE_AREA: "recommendations.reasons.nearPreferredExchangeArea",
    POPULAR_RECENTLY: "recommendations.reasons.popularRecently",
  };
  return t(keyByReason[reason] ?? "recommendations.reasons.popularRecently");
}

