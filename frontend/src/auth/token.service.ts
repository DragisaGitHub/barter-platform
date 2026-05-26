const ACCESS_TOKEN_KEY = "accessToken";
const REFRESH_TOKEN_KEY = "refreshToken";

type StoredTokens = {
  accessToken: string | null;
  refreshToken: string | null;
};

let tokenCache: StoredTokens = {
  accessToken: null,
  refreshToken: null,
};

function readStoredTokens(): StoredTokens {
  return {
    accessToken: window.localStorage.getItem(ACCESS_TOKEN_KEY),
    refreshToken: window.localStorage.getItem(REFRESH_TOKEN_KEY),
  };
}

function syncCacheFromStorage(): StoredTokens {
  tokenCache = readStoredTokens();
  return tokenCache;
}

export const tokenService = {
  getAccessToken(): string | null {
    return tokenCache.accessToken ?? syncCacheFromStorage().accessToken;
  },

  getRefreshToken(): string | null {
    return tokenCache.refreshToken ?? syncCacheFromStorage().refreshToken;
  },

  setTokens(accessToken: string, refreshToken: string): void {
    tokenCache = { accessToken, refreshToken };
    window.localStorage.setItem(ACCESS_TOKEN_KEY, accessToken);
    window.localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken);
  },

  clearTokens(): void {
    tokenCache = { accessToken: null, refreshToken: null };
    window.localStorage.removeItem(ACCESS_TOKEN_KEY);
    window.localStorage.removeItem(REFRESH_TOKEN_KEY);
  },

  hasTokens(): boolean {
    const tokens = syncCacheFromStorage();
    return Boolean(tokens.accessToken && tokens.refreshToken);
  },

  /**
   * Current SPA tradeoff: tokens still persist in localStorage for page-refresh resilience.
   * This keeps the existing UX intact, but they remain readable by injected JavaScript if XSS occurs.
   * A future roadmap step can move the refresh token to an httpOnly cookie.
   */
  getStorageMode(): "localStorage" {
    return "localStorage";
  },
};
