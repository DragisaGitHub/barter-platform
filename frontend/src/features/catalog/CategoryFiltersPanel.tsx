import React, { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import type { CategoryFilterFieldResponse } from "@/api/generated/types";
import type { SchemaFieldFilterValues } from "@/api/catalogApi";
import { Button } from "../../components/ui/Button";
import { Spinner } from "../../components/ui/Spinner";
import { useCategoryFilters } from "./useCatalog";

/**
 * Renders dynamic marketplace filter controls for the currently selected category, driven by the
 * category's ACTIVE schema `filterable=true` fields (Marketplace Schema Engine, Phase 6).
 *
 * Renders nothing when no category is selected, while filters are still loading for the first
 * time is handled with a small spinner, and nothing (nor a small empty note) when the category has
 * no active schema or no filterable fields.
 */
export function CategoryFiltersPanel({
  categoryUuid,
  values,
  onApply,
  className = "",
}: {
  categoryUuid?: string;
  values: SchemaFieldFilterValues;
  onApply: (values: SchemaFieldFilterValues) => void;
  className?: string;
}) {
  const { t, i18n } = useTranslation("catalog");
  const isSerbian = i18n.language?.toLowerCase().startsWith("sr");
  const { data, isLoading } = useCategoryFilters(categoryUuid);
  const [pending, setPending] = useState<SchemaFieldFilterValues>(values);

  // Reset pending local state whenever the selected category (and thus its filter set) changes.
  useEffect(() => {
    setPending(values);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [categoryUuid]);

  if (!categoryUuid) {
    return null;
  }

  if (isLoading) {
    return (
      <div className={className}>
        <div className="flex justify-center py-4">
          <Spinner />
        </div>
      </div>
    );
  }

  const filters = data?.filters ?? [];
  if (filters.length === 0) {
    return null;
  }

  const hasPendingValues = Object.values(pending).some((v) =>
    Array.isArray(v) ? v.length > 0 : v !== undefined && v !== ""
  );
  const hasAppliedValues = Object.values(values).some((v) =>
    Array.isArray(v) ? v.length > 0 : v !== undefined && v !== ""
  );

  const localizedLabel = (field: { label: string; labelSr?: string | null }) =>
    isSerbian && field.labelSr ? field.labelSr : field.label;

  const updatePending = (key: string, value: string | boolean | string[] | undefined) => {
    setPending((previous) => ({ ...previous, [key]: value }));
  };

  const handleApply = (event: React.FormEvent) => {
    event.preventDefault();
    onApply(pending);
  };

  const handleClear = () => {
    setPending({});
    onApply({});
  };

  return (
    <div className={className}>
      <div className="mb-3 flex items-center justify-between">
        <h2 className="text-base font-medium text-slate-900">{t("marketplace.categoryFilters.title")}</h2>
        {hasAppliedValues ? (
          <button
            type="button"
            onClick={handleClear}
            className="text-xs font-medium text-violet-600 transition hover:text-violet-800"
          >
            {t("clear")}
          </button>
        ) : null}
      </div>
      <p className="mb-3 text-xs leading-5 text-slate-500">{t("marketplace.categoryFilters.helper")}</p>

      <form onSubmit={handleApply} className="space-y-3">
        {filters.map((field) => (
          <CategoryFilterFieldControl
            key={field.fieldUuid}
            field={field}
            value={pending[field.key]}
            onChange={(value) => updatePending(field.key, value)}
            label={localizedLabel(field)}
          />
        ))}

        <Button
          type="submit"
          variant="outline"
          disabled={!hasPendingValues && !hasAppliedValues}
          className="h-9 w-full rounded-lg border-slate-200 bg-white text-slate-700 hover:border-violet-200 hover:bg-violet-50 hover:text-violet-700"
        >
          {t("marketplace.categoryFilters.apply")}
        </Button>
      </form>
    </div>
  );
}

function CategoryFilterFieldControl({
  field,
  value,
  onChange,
  label,
}: {
  field: CategoryFilterFieldResponse;
  value: string | boolean | string[] | undefined;
  onChange: (value: string | boolean | string[] | undefined) => void;
  label: string;
}) {
  const { t, i18n } = useTranslation("catalog");
  const isSerbian = i18n.language?.toLowerCase().startsWith("sr");
  const options = field.options ?? [];
  const optionLabel = (option: { label: string; labelSr?: string | null }) =>
    isSerbian && option.labelSr ? option.labelSr : option.label;
  const inputId = `category-filter-${field.fieldUuid}`;

  switch (field.fieldType) {
    case "TEXT":
      return (
        <div>
          <label htmlFor={inputId} className="mb-1 block text-xs font-medium text-slate-600">
            {label}
          </label>
          <input
            id={inputId}
            type="text"
            value={(value as string | undefined) ?? ""}
            onChange={(event) => onChange(event.target.value || undefined)}
            className="h-9 w-full rounded-lg border border-slate-200 bg-white px-3 text-sm text-slate-900 outline-none transition focus:border-violet-300 focus:ring-2 focus:ring-violet-100"
          />
        </div>
      );

    case "NUMBER":
      return (
        <div>
          <label htmlFor={inputId} className="mb-1 block text-xs font-medium text-slate-600">
            {label}
            {field.unit ? ` (${field.unit})` : ""}
          </label>
          <input
            id={inputId}
            type="number"
            value={(value as string | undefined) ?? ""}
            onChange={(event) => onChange(event.target.value || undefined)}
            className="h-9 w-full rounded-lg border border-slate-200 bg-white px-3 text-sm text-slate-900 outline-none transition focus:border-violet-300 focus:ring-2 focus:ring-violet-100"
          />
        </div>
      );

    case "DATE":
      return (
        <div>
          <label htmlFor={inputId} className="mb-1 block text-xs font-medium text-slate-600">
            {label}
          </label>
          <input
            id={inputId}
            type="date"
            value={(value as string | undefined) ?? ""}
            onChange={(event) => onChange(event.target.value || undefined)}
            className="h-9 w-full rounded-lg border border-slate-200 bg-white px-3 text-sm text-slate-900 outline-none transition focus:border-violet-300 focus:ring-2 focus:ring-violet-100"
          />
        </div>
      );

    case "BOOLEAN":
      return (
        <label htmlFor={inputId} className="flex items-center gap-2 text-sm text-slate-700">
          <input
            id={inputId}
            type="checkbox"
            checked={value === true}
            onChange={(event) => onChange(event.target.checked ? true : undefined)}
            className="size-4 rounded border-slate-300 text-violet-600 focus:ring-violet-400"
          />
          {label}
        </label>
      );

    case "SINGLE_SELECT":
      return (
        <div>
          <label htmlFor={inputId} className="mb-1 block text-xs font-medium text-slate-600">
            {label}
          </label>
          <select
            id={inputId}
            value={(value as string | undefined) ?? ""}
            onChange={(event) => onChange(event.target.value || undefined)}
            className="h-9 w-full rounded-lg border border-slate-200 bg-white px-2 text-sm text-slate-900 outline-none transition focus:border-violet-300 focus:ring-2 focus:ring-violet-100"
          >
            <option value="">{t("marketplace.categoryFilters.any")}</option>
            {options.map((option) => (
              <option key={option.optionUuid} value={option.value}>
                {optionLabel(option)}
              </option>
            ))}
          </select>
        </div>
      );

    case "MULTI_SELECT": {
      const selected = Array.isArray(value) ? value : [];
      const toggleOption = (optionValue: string) => {
        const next = selected.includes(optionValue)
          ? selected.filter((v) => v !== optionValue)
          : [...selected, optionValue];
        onChange(next.length > 0 ? next : undefined);
      };

      return (
        <fieldset>
          <legend className="mb-1 text-xs font-medium text-slate-600">{label}</legend>
          <div className="flex flex-wrap gap-1.5">
            {options.map((option) => {
              const isSelected = selected.includes(option.value);
              return (
                <button
                  key={option.optionUuid}
                  type="button"
                  onClick={() => toggleOption(option.value)}
                  className={`rounded-full border px-3 py-1 text-xs font-medium transition-colors ${
                    isSelected
                      ? "border-violet-400 bg-violet-100 text-violet-700"
                      : "border-slate-200 bg-white text-slate-600 hover:border-violet-300 hover:text-violet-600"
                  }`}
                >
                  {optionLabel(option)}
                </button>
              );
            })}
          </div>
        </fieldset>
      );
    }

    default:
      return null;
  }
}

