import { useNavigate } from "react-router-dom";
import { Search } from "lucide-react";
import { useTranslation } from "react-i18next";
import type { SavedSearchCriteria } from "@/api/generated/types";
import { routePaths } from "@/routes/routePaths";
import { SavedSearchesPanel } from "./SavedSearchesPanel";

export function SavedSearchesPage() {
  const { t } = useTranslation("catalog");
  const navigate = useNavigate();

  const applySavedSearch = (criteria: SavedSearchCriteria) => {
    navigate(buildMarketplacePath(criteria));
  };

  return (
    <div className="mx-auto max-w-5xl space-y-6">
      <section className="marketplace-panel p-5 sm:p-6">
        <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <div className="inline-flex items-center rounded-full border border-violet-200 bg-violet-50 px-3 py-1 text-[11px] font-semibold uppercase tracking-[0.14em] text-violet-700">
              {t("savedSearches.badge")}
            </div>
            <h1 className="mt-3 text-3xl font-bold text-slate-900">{t("savedSearches.title")}</h1>
            <p className="mt-2 max-w-2xl text-sm leading-6 text-slate-600">
              {t("savedSearches.subtitle")}
            </p>
          </div>
          <div className="flex size-14 items-center justify-center rounded-2xl bg-violet-50 text-violet-600">
            <Search className="size-7" />
          </div>
        </div>
      </section>

      <section className="marketplace-panel p-4 sm:p-5">
        <SavedSearchesPanel onApply={applySavedSearch} />
      </section>
    </div>
  );
}

function buildMarketplacePath(criteria: SavedSearchCriteria) {
  const searchParams = new URLSearchParams();

  if (criteria.q) {
    searchParams.set("q", criteria.q);
  }
  if (criteria.categoryUuid) {
    searchParams.set("categoryUuid", criteria.categoryUuid);
  }
  criteria.tagUuids?.forEach((tagUuid) => searchParams.append("tagUuids", tagUuid));
  if (criteria.condition) {
    searchParams.set("condition", criteria.condition);
  }
  if (criteria.location) {
    searchParams.set("location", criteria.location);
  }
  if (criteria.sort) {
    searchParams.set("sort", criteria.sort);
  }

  const query = searchParams.toString();
  return query ? `${routePaths.marketplace}?${query}` : routePaths.marketplace;
}

