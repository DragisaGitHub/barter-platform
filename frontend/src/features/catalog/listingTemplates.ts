import type { ListingMode, ListingTemplateMetadata, ListingTemplateType } from "@/api/generated/types.ts";

export interface ListingTemplateDraftValues {
  bundleTitle?: string;
  groupingDescription?: string;
  selectionHint?: string;
  collectionName?: string;
  totalOwned?: number;
  duplicateCount?: number;
  missingEntriesText?: string;
  wantedEntriesText?: string;
  exchangeRules?: string;
  wishlistSummary?: string;
  wantedConditionNotes?: string;
}

export const LISTING_TEMPLATE_OPTIONS: {
  value: ListingTemplateType;
  mode: ListingMode;
  labelKey: string;
  helperKey: string;
  exampleKey: string;
}[] = [
  {
    value: "STANDARD_ITEM",
    mode: "SINGLE",
    labelKey: "listingTemplate.label.STANDARD_ITEM",
    helperKey: "listingTemplate.helper.STANDARD_ITEM",
    exampleKey: "listingTemplate.example.STANDARD_ITEM",
  },
  {
    value: "BUNDLE",
    mode: "BUNDLE",
    labelKey: "listingTemplate.label.BUNDLE",
    helperKey: "listingTemplate.helper.BUNDLE",
    exampleKey: "listingTemplate.example.BUNDLE",
  },
  {
    value: "PICK_FROM_COLLECTION",
    mode: "PICK_ANY",
    labelKey: "listingTemplate.label.PICK_FROM_COLLECTION",
    helperKey: "listingTemplate.helper.PICK_FROM_COLLECTION",
    exampleKey: "listingTemplate.example.PICK_FROM_COLLECTION",
  },
  {
    value: "COLLECTION_ALBUM",
    mode: "PICK_ANY",
    labelKey: "listingTemplate.label.COLLECTION_ALBUM",
    helperKey: "listingTemplate.helper.COLLECTION_ALBUM",
    exampleKey: "listingTemplate.example.COLLECTION_ALBUM",
  },
  {
    value: "WISHLIST",
    mode: "SINGLE",
    labelKey: "listingTemplate.label.WISHLIST",
    helperKey: "listingTemplate.helper.WISHLIST",
    exampleKey: "listingTemplate.example.WISHLIST",
  },
];

const DEFAULT_TEMPLATE_BY_MODE: Record<ListingMode, ListingTemplateType> = {
  SINGLE: "STANDARD_ITEM",
  BUNDLE: "BUNDLE",
  PICK_ANY: "PICK_FROM_COLLECTION",
};

export function inferListingTemplateType(
  listingMode?: ListingMode | null,
  listingTemplateType?: ListingTemplateType | null,
): ListingTemplateType {
  if (listingTemplateType) {
    return listingTemplateType;
  }
  return DEFAULT_TEMPLATE_BY_MODE[listingMode ?? "SINGLE"];
}

export function resolveListingModeForTemplate(templateType: ListingTemplateType): ListingMode {
  return LISTING_TEMPLATE_OPTIONS.find((option) => option.value === templateType)?.mode ?? "SINGLE";
}

export function buildTemplateMetadataRequest(values: ListingTemplateDraftValues): ListingTemplateMetadata | undefined {
  const metadata: ListingTemplateMetadata = {};

  assignIfPresent(metadata, "bundleTitle", normalizeText(values.bundleTitle));
  assignIfPresent(metadata, "groupingDescription", normalizeText(values.groupingDescription));
  assignIfPresent(metadata, "selectionHint", normalizeText(values.selectionHint));
  assignIfPresent(metadata, "collectionName", normalizeText(values.collectionName));
  assignIfPresent(metadata, "totalOwned", values.totalOwned);
  assignIfPresent(metadata, "duplicateCount", values.duplicateCount);
  assignIfPresent(metadata, "missingEntries", splitMultilineEntries(values.missingEntriesText));
  assignIfPresent(metadata, "wantedEntries", splitMultilineEntries(values.wantedEntriesText));
  assignIfPresent(metadata, "exchangeRules", normalizeText(values.exchangeRules));
  assignIfPresent(metadata, "wishlistSummary", normalizeText(values.wishlistSummary));
  assignIfPresent(metadata, "wantedConditionNotes", normalizeText(values.wantedConditionNotes));

  return Object.keys(metadata).length > 0 ? metadata : undefined;
}

export function multilineEntriesValue(values?: string[] | null): string {
  return values?.join("\n") ?? "";
}

function splitMultilineEntries(value?: string | null): string[] | undefined {
  if (!value) {
    return undefined;
  }
  const parts = value
    .split(/\r?\n|,/)
    .map((entry) => normalizeText(entry))
    .filter((entry): entry is string => Boolean(entry));
  return parts.length ? Array.from(new Set(parts)) : undefined;
}

function normalizeText(value?: string | null): string | undefined {
  if (!value) {
    return undefined;
  }
  const normalized = value.trim().replace(/\s+/g, " ");
  return normalized ? normalized : undefined;
}

function assignIfPresent<T extends object, K extends keyof ListingTemplateMetadata>(
  target: T,
  key: K,
  value: ListingTemplateMetadata[K] | undefined,
) {
  if (value !== undefined && value !== null && (!(Array.isArray(value)) || value.length > 0)) {
    Object.assign(target, { [key]: value });
  }
}

