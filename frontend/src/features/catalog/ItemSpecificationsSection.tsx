import { useTranslation } from "react-i18next";
import type { SchemaFieldValueResponse } from "@/api/generated/types.ts";
import { formatSchemaFieldValue, getDisplayableSchemaFieldValues, getSchemaFieldLabel } from "./itemSpecifications";

interface ItemSpecificationsSectionProps {
  schemaFieldValues?: SchemaFieldValueResponse[] | null;
}

/**
 * Renders the saved category-schema field values on the item detail page as a clean,
 * read-only "Specifications" / "Karakteristike" table (Marketplace Schema Engine, Phase 5).
 * Renders nothing when there are no meaningful values to display.
 */
export function ItemSpecificationsSection({ schemaFieldValues }: ItemSpecificationsSectionProps) {
  const { t, i18n } = useTranslation("catalog");
  const displayable = getDisplayableSchemaFieldValues(schemaFieldValues);

  if (displayable.length === 0) {
    return null;
  }

  const yesLabel = t("catalog:fields.booleanYes");
  const noLabel = t("catalog:fields.booleanNo");

  return (
    <section className="marketplace-panel mt-4 p-4" data-testid="item-specifications-section">
      <h2 className="text-lg font-medium text-slate-900">{t("catalog:itemDetail.specifications")}</h2>
      <div className="mt-2.5 border-t border-slate-200 pt-3">
        <dl className="grid grid-cols-1 gap-x-6 gap-y-2 sm:grid-cols-2">
          {displayable.map((value) => (
            <div
              key={value.fieldUuid}
              className="flex items-center justify-between gap-3 rounded-lg border border-slate-200 bg-slate-50 px-3.5 py-2.5 sm:justify-start"
              data-testid={`item-specification-${value.key}`}
            >
              <dt className="text-sm text-slate-500 sm:w-1/2 sm:shrink-0">
                {getSchemaFieldLabel(value, i18n.language)}
              </dt>
              <dd className="text-right text-sm font-medium text-slate-900 sm:w-1/2 sm:text-left">
                {formatSchemaFieldValue(value, { locale: i18n.language, yesLabel, noLabel })}
              </dd>
            </div>
          ))}
        </dl>
      </div>
    </section>
  );
}

