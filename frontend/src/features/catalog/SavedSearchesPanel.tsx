import { useMemo, useState } from "react";
import { Search, Trash2 } from "lucide-react";
import { toast } from "sonner";
import { useTranslation } from "react-i18next";
import type { SavedSearchCriteria, SavedSearchResponse } from "@/api/generated/types";
import { parseApiError } from "@/utils";
import { Button } from "../../components/ui/Button";
import { EmptyState } from "../../components/ui/EmptyState";
import { Spinner } from "../../components/ui/Spinner";
import { useDeleteSavedSearch, useSavedSearches } from "./useSavedSearches";

interface SavedSearchesPanelProps {
  compact?: boolean;
  onApply: (criteria: SavedSearchCriteria) => void;
}

export function SavedSearchesPanel({ compact = false, onApply }: SavedSearchesPanelProps) {
  const { t } = useTranslation(["catalog", "common"]);
  const params = useMemo(() => ({ page: 0, size: compact ? 5 : 20, sort: "updatedAt,desc" }), [compact]);
  const { data, isLoading, isError } = useSavedSearches(params);
  const deleteMutation = useDeleteSavedSearch();
  const [pendingDeleteUuid, setPendingDeleteUuid] = useState<string | null>(null);

  const handleDelete = (savedSearch: SavedSearchResponse) => {
    setPendingDeleteUuid(savedSearch.uuid);
    deleteMutation.mutate(savedSearch.uuid, {
      onError: (error) => toast.error(parseApiError(error)),
      onSettled: () => setPendingDeleteUuid((current) => (current === savedSearch.uuid ? null : current)),
    });
  };

  if (isLoading) {
    return (
      <div className="flex justify-center py-6">
        <Spinner />
      </div>
    );
  }

  if (isError) {
    return (
      <div className="rounded-lg border border-rose-100 bg-rose-50 px-3 py-2 text-sm text-rose-700">
        {t("catalog:savedSearches.loadError")}
      </div>
    );
  }

  if (!data || data.content.length === 0) {
    return compact ? (
      <p className="text-sm leading-6 text-slate-500">{t("catalog:savedSearches.emptyCompact")}</p>
    ) : (
      <EmptyState
        icon={<Search className="size-16" />}
        title={t("catalog:savedSearches.emptyTitle")}
        description={t("catalog:savedSearches.emptyDescription")}
      />
    );
  }

  return (
    <div className="space-y-2">
      {data.content.map((savedSearch) => (
        <div
          key={savedSearch.uuid}
          className="rounded-lg border border-slate-200 bg-white p-3 shadow-sm"
        >
          <div className="flex items-start justify-between gap-3">
            <div className="min-w-0">
              <h3 className="truncate text-sm font-semibold text-slate-900">{savedSearch.name}</h3>
              <p className="mt-1 line-clamp-2 text-xs leading-5 text-slate-500">
                {formatCriteria(savedSearch.criteria, t)}
              </p>
            </div>
            <button
              type="button"
              onClick={() => handleDelete(savedSearch)}
              disabled={pendingDeleteUuid === savedSearch.uuid}
              className="inline-flex size-8 shrink-0 items-center justify-center rounded-lg text-slate-400 transition hover:bg-rose-50 hover:text-rose-600 disabled:cursor-not-allowed disabled:opacity-60"
              aria-label={t("catalog:savedSearches.delete")}
            >
              <Trash2 className="size-4" />
            </button>
          </div>

          <Button
            type="button"
            variant="outline"
            onClick={() => onApply(savedSearch.criteria)}
            className="mt-3 h-9 w-full border-slate-200 bg-slate-50 text-slate-700 hover:border-violet-200 hover:bg-violet-50 hover:text-violet-700"
          >
            {t("catalog:savedSearches.apply")}
          </Button>
        </div>
      ))}
    </div>
  );
}

function formatCriteria(
  criteria: SavedSearchCriteria,
  t: (key: string, options?: Record<string, unknown>) => string
) {
  const parts: string[] = [];

  if (criteria.q) {
    parts.push(t("catalog:savedSearches.criteria.query", { query: criteria.q }));
  }
  if (criteria.categoryUuid) {
    parts.push(t("catalog:savedSearches.criteria.category"));
  }
  if (criteria.tagUuids?.length) {
    parts.push(t("catalog:savedSearches.criteria.tags", { count: criteria.tagUuids.length }));
  }
  if (criteria.condition) {
    parts.push(t("catalog:savedSearches.criteria.condition", { condition: t(conditionTranslationKey(criteria.condition)) }));
  }
  if (criteria.location) {
    parts.push(t("catalog:savedSearches.criteria.location", { location: criteria.location }));
  }

  return parts.length > 0 ? parts.join(" · ") : t("catalog:savedSearches.criteria.catalogFilters");
}

function conditionTranslationKey(value: string) {
  const keys: Record<string, string> = {
    NEW: "catalog:condition.new",
    LIKE_NEW: "catalog:condition.likeNew",
    GOOD: "catalog:condition.good",
    USED: "catalog:condition.used",
    FOR_PARTS: "catalog:condition.forParts",
  };
  return keys[value] ?? value;
}

