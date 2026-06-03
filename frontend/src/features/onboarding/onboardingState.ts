const MARKETPLACE_VISITED_KEY = "onboarding.marketplaceVisited";
const BETA_FEEDBACK_SUBMITTED_KEY = "onboarding.betaFeedbackSubmitted";

type StoredMap = Record<string, boolean>;

function readStoredMap(key: string): StoredMap {
  try {
    const raw = window.localStorage.getItem(key);
    if (!raw) {
      return {};
    }

    const parsed = JSON.parse(raw) as unknown;
    return parsed && typeof parsed === "object" ? (parsed as StoredMap) : {};
  } catch {
    return {};
  }
}

function writeStoredMap(key: string, value: StoredMap) {
  try {
    window.localStorage.setItem(key, JSON.stringify(value));
  } catch {
    // Ignore storage failures so onboarding stays non-blocking.
  }
}

function readFlag(key: string, userUuid?: string | null): boolean {
  if (!userUuid || typeof window === "undefined") {
    return false;
  }

  return Boolean(readStoredMap(key)[userUuid]);
}

function writeFlag(key: string, userUuid?: string | null) {
  if (!userUuid || typeof window === "undefined") {
    return;
  }

  const current = readStoredMap(key);
  writeStoredMap(key, {
    ...current,
    [userUuid]: true,
  });
}

export function hasVisitedMarketplace(userUuid?: string | null): boolean {
  return readFlag(MARKETPLACE_VISITED_KEY, userUuid);
}

export function markMarketplaceVisited(userUuid?: string | null) {
  writeFlag(MARKETPLACE_VISITED_KEY, userUuid);
}

export function hasSubmittedBetaFeedback(userUuid?: string | null): boolean {
  return readFlag(BETA_FEEDBACK_SUBMITTED_KEY, userUuid);
}

export function markBetaFeedbackSubmitted(userUuid?: string | null) {
  writeFlag(BETA_FEEDBACK_SUBMITTED_KEY, userUuid);
}

