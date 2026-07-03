import type { CategoryFormFieldOptionResponse, SchemaFieldValueResponse } from "@/api/generated/types.ts";

/**
 * Pure helpers for displaying persisted schema field values (Marketplace Schema Engine, Phase 5)
 * on the item detail page as a read-only "Specifications" table. Kept free of React/i18n
 * dependencies (aside from a small translate callback) so they are easy to unit test.
 */

/** Returns true when a locale string should be treated as Serbian for label selection. */
export function isSerbianLocale(locale?: string | null): boolean {
  return !!locale && locale.toLowerCase().startsWith("sr");
}

/** Picks the localized label for a schema field value, preferring labelSr when Serbian. */
export function getSchemaFieldLabel(value: SchemaFieldValueResponse, locale?: string | null): string {
  if (isSerbianLocale(locale) && value.labelSr) {
    return value.labelSr;
  }
  return value.label;
}

/** Picks the localized label for a single selected option, preferring labelSr when Serbian. */
function getOptionLabel(option: CategoryFormFieldOptionResponse, locale?: string | null): string {
  if (isSerbianLocale(locale) && option.labelSr) {
    return option.labelSr;
  }
  return option.label;
}

/** Returns true when a schema field value has a meaningful value worth displaying. */
export function hasSchemaFieldValue(value: SchemaFieldValueResponse): boolean {
  switch (value.fieldType) {
    case "TEXT":
      return !!value.valueText && value.valueText.trim().length > 0;
    case "NUMBER":
      return value.valueNumber !== undefined && value.valueNumber !== null;
    case "BOOLEAN":
      return value.valueBoolean !== undefined && value.valueBoolean !== null;
    case "DATE":
      return !!value.valueDate;
    case "SINGLE_SELECT":
    case "MULTI_SELECT":
      return !!value.options && value.options.length > 0;
    default:
      return false;
  }
}

export interface FormatSchemaFieldValueOptions {
  /** Current i18n locale, used to prefer labelSr / localized formatting for Serbian. */
  locale?: string | null;
  /** Translated "Yes" label (defaults to "Yes"). */
  yesLabel?: string;
  /** Translated "No" label (defaults to "No"). */
  noLabel?: string;
}

/**
 * Formats a schema field value for display according to its fieldType.
 * Returns an empty string when the value has nothing meaningful to display;
 * callers should generally guard with hasSchemaFieldValue() first.
 */
export function formatSchemaFieldValue(
  value: SchemaFieldValueResponse,
  options: FormatSchemaFieldValueOptions = {}
): string {
  const { locale, yesLabel = "Yes", noLabel = "No" } = options;

  switch (value.fieldType) {
    case "TEXT":
      return value.valueText?.trim() ?? "";

    case "NUMBER": {
      if (value.valueNumber === undefined || value.valueNumber === null) {
        return "";
      }
      const formattedNumber = new Intl.NumberFormat(isSerbianLocale(locale) ? "sr-Latn-RS" : "en-US").format(
        value.valueNumber
      );
      return value.unit ? `${formattedNumber} ${value.unit}` : formattedNumber;
    }

    case "BOOLEAN":
      if (value.valueBoolean === undefined || value.valueBoolean === null) {
        return "";
      }
      return value.valueBoolean ? yesLabel : noLabel;

    case "DATE": {
      if (!value.valueDate) {
        return "";
      }
      const date = new Date(value.valueDate);
      if (Number.isNaN(date.getTime())) {
        return value.valueDate;
      }
      return date.toLocaleDateString(isSerbianLocale(locale) ? "sr-Latn-RS" : "en-US", {
        day: "numeric",
        month: "short",
        year: "numeric",
      });
    }

    case "SINGLE_SELECT":
    case "MULTI_SELECT":
      return (value.options ?? []).map((option) => getOptionLabel(option, locale)).join(", ");

    default:
      return "";
  }
}

/** Sorts schema field values by displayOrder (falling back to stable input order). */
export function sortSchemaFieldValues(values: SchemaFieldValueResponse[]): SchemaFieldValueResponse[] {
  return [...values].sort((a, b) => (a.displayOrder ?? 0) - (b.displayOrder ?? 0));
}

/** Filters and sorts schema field values down to only those meaningful for display. */
export function getDisplayableSchemaFieldValues(
  values?: SchemaFieldValueResponse[] | null
): SchemaFieldValueResponse[] {
  if (!values || values.length === 0) {
    return [];
  }
  return sortSchemaFieldValues(values.filter(hasSchemaFieldValue));
}

