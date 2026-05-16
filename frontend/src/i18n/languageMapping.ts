import type { PreferredLanguage } from "@/api/generated/types";

export type FrontendLanguage = "sr" | "en";

export const DEFAULT_LANGUAGE: FrontendLanguage = "sr";
export const FALLBACK_LANGUAGE: FrontendLanguage = "sr";
export const SUPPORTED_LANGUAGES: FrontendLanguage[] = ["sr", "en"];

export function mapBackendToFrontendLanguage(
  preferredLanguage?: PreferredLanguage | null,
): FrontendLanguage {
  switch (preferredLanguage) {
    case "EN":
      return "en";
    case "SR":
    default:
      return "sr";
  }
}

export function mapFrontendToBackendLanguage(
  language: FrontendLanguage,
): PreferredLanguage {
  return language === "en" ? "EN" : "SR";
}

export function normalizeFrontendLanguage(
  value?: string | null,
): FrontendLanguage {
  return value === "en" ? "en" : "sr";
}

