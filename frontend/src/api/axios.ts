import axios from "axios";
import * as Sentry from "@sentry/react";
import type { TokenResponse } from "./generated/types";
import { tokenService } from "../auth/token.service";
import { isSentryEnabled } from "../lib/sentry";

const baseURL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080/api/v1";

export const apiClient = axios.create({
  baseURL,
  headers: {
    "Content-Type": "application/json",
  },
});

let isRefreshing = false;
let refreshSubscribers: ((token: string) => void)[] = [];

function onRefreshed(token: string) {
  refreshSubscribers.forEach((callback) => callback(token));
  refreshSubscribers = [];
}

function addRefreshSubscriber(callback: (token: string) => void) {
  refreshSubscribers.push(callback);
}

apiClient.interceptors.request.use(
  (config) => {
    const token = tokenService.getAccessToken();
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;
    const requestUrl = typeof originalRequest?.url === "string" ? originalRequest.url : "";
    const isAuthMeRequest = requestUrl.includes("/auth/me");

    if (requestUrl.includes("/auth/login")) {
      return Promise.reject(error);
    }

    if (error.response?.status === 401 && originalRequest && !originalRequest._retry) {
      if (isRefreshing) {
        return new Promise((resolve) => {
          addRefreshSubscriber((token: string) => {
            originalRequest.headers.Authorization = `Bearer ${token}`;
            resolve(apiClient(originalRequest));
          });
        });
      }

      originalRequest._retry = true;
      isRefreshing = true;

      try {
        const refreshToken = tokenService.getRefreshToken();
        if (!refreshToken) {
          tokenService.clearTokens();

          if (isAuthMeRequest) {
            isRefreshing = false;
            return Promise.reject(error);
          }

          throw new Error("No refresh token");
        }

        const response = await axios.post<TokenResponse>(
          `${baseURL}/auth/refresh`,
          { refreshToken }
        );

        const { accessToken, refreshToken: newRefreshToken } = response.data;
        tokenService.setTokens(accessToken, newRefreshToken);

        apiClient.defaults.headers.common.Authorization = `Bearer ${accessToken}`;
        originalRequest.headers.Authorization = `Bearer ${accessToken}`;

        onRefreshed(accessToken);
        isRefreshing = false;

        return apiClient(originalRequest);
      } catch (refreshError) {
        isRefreshing = false;
        tokenService.clearTokens();

        if (!isAuthMeRequest) {
          window.location.href = "/login";
          return Promise.reject(refreshError);
        }

        return Promise.reject(error);
      }
    }

    return Promise.reject(error);
  }
);

// --- Sentry observability interceptor ---
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (isSentryEnabled() && axios.isAxiosError(error)) {
      const status = error.response?.status;
      const method = error.config?.method?.toUpperCase() ?? "UNKNOWN";
      const url = error.config?.url ?? "unknown";
      const requestId =
        error.response?.headers?.["x-request-id"] ??
        error.response?.headers?.["x-correlation-id"];
      const backendCode = error.response?.data?.code;
      const backendMessage = error.response?.data?.message;

      // Only report unexpected server errors (5xx) to Sentry
      if (status && status >= 500) {
        Sentry.captureException(error, {
          tags: {
            "api.status": status,
            "api.method": method,
            ...(requestId ? { "api.requestId": requestId } : {}),
          },
          contexts: {
            api: {
              url,
              method,
              status,
              ...(requestId ? { requestId } : {}),
              ...(backendCode ? { errorCode: backendCode } : {}),
              ...(backendMessage ? { errorMessage: backendMessage } : {}),
            },
          },
        });
      }

      // Add breadcrumb for all API errors (safe metadata only)
      Sentry.addBreadcrumb({
        category: "api.error",
        level: status && status >= 500 ? "error" : "warning",
        data: {
          method,
          url,
          status,
          ...(requestId ? { requestId } : {}),
          ...(backendCode ? { errorCode: backendCode } : {}),
        },
      });
    }

    return Promise.reject(error);
  }
);

