export const tokenService = {
  getAccessToken(): string | null {
    return localStorage.getItem("accessToken");
  },

  getRefreshToken(): string | null {
    return localStorage.getItem("refreshToken");
  },

  setTokens(accessToken: string, refreshToken: string): void {
    localStorage.setItem("accessToken", accessToken);
    localStorage.setItem("refreshToken", refreshToken);
  },

  clearTokens(): void {
    localStorage.removeItem("accessToken");
    localStorage.removeItem("refreshToken");
  },

  hasTokens(): boolean {
    return !!(this.getAccessToken() && this.getRefreshToken());
  },
};
